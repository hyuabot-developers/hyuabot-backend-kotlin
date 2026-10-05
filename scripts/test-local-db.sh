#!/usr/bin/env bash

set -Eeuo pipefail
set +x

readonly PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
readonly CONFIG_FILE="${HOME}/.config/hyuabot-test-db.env"
readonly TEST_PROPERTIES="${PROJECT_ROOT}/src/main/resources/application-test.properties"
readonly TEST_BUILD_DIR="${PROJECT_ROOT}/build"
readonly JDBC_URL="jdbc:postgresql://nas.hyuabot.app:55432/hyuabot_test"

created_properties=0
backup_properties=""
backup_directory=""
backup_mode=""

restore_previous_properties() {
    python3 - "${backup_properties}" "${TEST_PROPERTIES}" "${backup_mode}" <<'PY'
import os
import sys
import tempfile
from pathlib import Path

backup_path = Path(sys.argv[1])
properties_path = Path(sys.argv[2])
original_mode = int(sys.argv[3], 8)
temporary_path = None
try:
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=".application-test.properties.restore.",
        dir=str(properties_path.parent),
    )
    temporary_path = Path(temporary_name)
    with os.fdopen(descriptor, "wb") as destination, backup_path.open("rb") as source:
        while True:
            chunk = source.read(1024 * 1024)
            if not chunk:
                break
            destination.write(chunk)
        destination.flush()
        os.fsync(destination.fileno())
        os.fchmod(destination.fileno(), original_mode)
        os.fsync(destination.fileno())
    os.replace(temporary_path, properties_path)
except OSError:
    if temporary_path is not None:
        try:
            temporary_path.unlink()
        except OSError:
            pass
    raise SystemExit("Unable to restore the pre-existing application-test.properties.") from None
PY
}

cleanup() {
    local status=$?
    local preserve_backup=0
    trap - EXIT INT TERM

    if [[ -n "${backup_properties}" ]]; then
        if ! restore_previous_properties; then
            echo "Failed to restore the pre-existing application-test.properties; private backup preserved at ${backup_properties}." >&2
            if ! rm -f "${TEST_PROPERTIES}"; then
                echo "Unable to remove temporary application-test.properties after restoration failed." >&2
            fi
            preserve_backup=1
            status=1
        else
            rm -f "${backup_properties}"
        fi
    elif [[ "${created_properties}" -eq 1 ]]; then
        rm -f "${TEST_PROPERTIES}"
    fi

    if [[ -d "${TEST_BUILD_DIR}" ]]; then
        find "${TEST_BUILD_DIR}" -type f -name application-test.properties -delete
    fi

    if [[ -n "${backup_directory}" && "${preserve_backup}" -eq 0 ]]; then
        rm -rf "${backup_directory}"
    fi

    exit "${status}"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if [[ -L "${TEST_PROPERTIES}" ]]; then
    echo "Refusing to replace a symlink at src/main/resources/application-test.properties." >&2
    exit 1
fi

if [[ -e "${TEST_PROPERTIES}" ]]; then
    backup_directory="$(mktemp -d "${TMPDIR:-/tmp}/hyuabot-application-test.XXXXXX")"
    chmod 700 "${backup_directory}"
    backup_candidate="${backup_directory}/application-test.properties"
    backup_mode="$(python3 - "${TEST_PROPERTIES}" "${backup_candidate}" <<'PY'
import os
import stat
import sys
from pathlib import Path

source_path = Path(sys.argv[1])
backup_path = Path(sys.argv[2])
source_descriptor = None
backup_descriptor = None
try:
    source_flags = os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0)
    source_descriptor = os.open(source_path, source_flags)
    original_mode = stat.S_IMODE(os.fstat(source_descriptor).st_mode)
    backup_descriptor = os.open(backup_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    os.fchmod(backup_descriptor, 0o600)
    with os.fdopen(source_descriptor, "rb") as source, os.fdopen(backup_descriptor, "wb") as backup:
        source_descriptor = None
        backup_descriptor = None
        while True:
            chunk = source.read(1024 * 1024)
            if not chunk:
                break
            backup.write(chunk)
        backup.flush()
        os.fsync(backup.fileno())
    print(format(original_mode, "o"))
except OSError:
    if source_descriptor is not None:
        os.close(source_descriptor)
    if backup_descriptor is not None:
        os.close(backup_descriptor)
    try:
        backup_path.unlink()
    except OSError:
        pass
    raise SystemExit("Unable to back up the existing application-test.properties.") from None
PY
)"
    backup_properties="${backup_candidate}"
    rm -f "${TEST_PROPERTIES}"
else
    created_properties=1
fi

python3 - "${CONFIG_FILE}" "${TEST_PROPERTIES}" "${JDBC_URL}" <<'PY'
import os
import re
import sys
import tempfile
from pathlib import Path

config_path = Path(sys.argv[1])
properties_path = Path(sys.argv[2])
jdbc_url = sys.argv[3]
required = {"DATABASE_USERNAME", "DATABASE_PASSWORD"}

if not config_path.is_file():
    raise SystemExit("Missing ~/.config/hyuabot-test-db.env.")

values = {}
try:
    input_lines = config_path.read_text(encoding="utf-8").splitlines()
except (OSError, UnicodeError):
    raise SystemExit("Unable to read ~/.config/hyuabot-test-db.env as UTF-8.") from None

for line_number, raw_line in enumerate(input_lines, start=1):
    line = raw_line.strip()
    if not line or line.startswith("#"):
        continue
    key, separator, raw_value = line.partition("=")
    key = key.strip()
    if not separator or not re.fullmatch(r"[A-Z][A-Z0-9_]*", key):
        raise SystemExit(f"Invalid setting on line {line_number} of ~/.config/hyuabot-test-db.env.")
    if key not in required:
        raise SystemExit(f"Unexpected setting {key} in ~/.config/hyuabot-test-db.env.")
    if key in values:
        raise SystemExit(f"Duplicate setting {key} in ~/.config/hyuabot-test-db.env.")
    value = raw_value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        value = value[1:-1]
    if not value or any(ord(character) < 32 for character in value):
        raise SystemExit(f"Missing or invalid value for {key} in ~/.config/hyuabot-test-db.env.")
    values[key] = value

missing = sorted(required - values.keys())
if missing:
    raise SystemExit("Missing required settings in ~/.config/hyuabot-test-db.env: " + ", ".join(missing))

def escape_property(value: str) -> str:
    escaped = []
    for index, character in enumerate(value):
        if character == "\\":
            escaped.append("\\\\")
        elif character in "\t\n\r\f":
            escaped.append({"\t": "\\t", "\n": "\\n", "\r": "\\r", "\f": "\\f"}[character])
        elif character in "=:#!" or (character == " " and (index == 0 or index == len(value) - 1)):
            escaped.append("\\" + character)
        else:
            escaped.append(character)
    return "".join(escaped)

properties = (
    f"spring.datasource.url={jdbc_url}\n"
    f"spring.datasource.username={escape_property(values['DATABASE_USERNAME'])}\n"
    f"spring.datasource.password={escape_property(values['DATABASE_PASSWORD'])}\n"
    "spring.data.redis.host=localhost\n"
    "spring.data.redis.port=6379\n"
    "spring.jpa.hibernate.ddl-auto=validate\n"
    "jasypt.encryptor.password=local-test-only\n"
    "jwt.secret=local-test-jwt-secret-for-tests-only\n"
)
temporary_path = None
try:
    descriptor, temporary_name = tempfile.mkstemp(
        prefix=".application-test.properties.",
        dir=str(properties_path.parent),
    )
    temporary_path = Path(temporary_name)
    os.fchmod(descriptor, 0o600)
    with os.fdopen(descriptor, "w", encoding="utf-8") as properties_file:
        properties_file.write(properties)
        properties_file.flush()
        os.fsync(properties_file.fileno())
    os.replace(temporary_path, properties_path)
except OSError:
    if temporary_path is not None:
        try:
            temporary_path.unlink()
        except OSError:
            pass
    raise SystemExit("Unable to write temporary application-test.properties.") from None
PY

check_redis() {
    python3 - <<'PY'
import socket
import sys

try:
    with socket.create_connection(("localhost", 6379), timeout=2) as connection:
        connection.sendall(b"*1\r\n$4\r\nPING\r\n")
        response = connection.recv(64)
except OSError:
    raise SystemExit("Local Redis prerequisite failed at localhost:6379.") from None

if response != b"+PONG\r\n":
    raise SystemExit("Local Redis prerequisite did not return PONG.")
PY
    echo "Local Redis prerequisite passed."
}

run_gate() {
    local run_number="$1"
    check_redis
    echo "Running local test gate ${run_number}/2."
    ./gradlew --no-daemon --console=plain --no-build-cache clean
    ./gradlew --no-daemon --console=plain --no-build-cache --rerun-tasks \
        ktlintCheck test jacocoTestReport
}

cd "${PROJECT_ROOT}"
run_gate 1
run_gate 2
echo "Both local test gate runs passed."
