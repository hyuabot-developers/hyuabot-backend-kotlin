package app.hyuabot.backend.readingRoom

import app.hyuabot.backend.codegen.types.ReadingRoom
import app.hyuabot.backend.codegen.types.ReadingRoomSeat
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

@DgsComponent
class ReadingRoomDataFetcher(
    private val readingRoomService: ReadingRoomService,
) {
    @DgsQuery
    fun readingRoom(): List<ReadingRoom> =
        readingRoomService
            .getReadingRoomList(
                null,
            ).map { readingRoom ->
                ReadingRoom(
                    seq = readingRoom.id,
                    name = readingRoom.name,
                    campus = readingRoom.campusID,
                    active = readingRoom.isActive,
                    reservable = readingRoom.isReservable,
                    seats =
                        ReadingRoomSeat(
                            total = readingRoom.total,
                            available = readingRoom.available,
                            active = readingRoom.active,
                            occupied = readingRoom.occupied,
                        ),
                    updatedAt = readingRoom.updatedAt.withZoneSameInstant(LocalDateTimeBuilder.serviceTimezone),
                )
            }
}
