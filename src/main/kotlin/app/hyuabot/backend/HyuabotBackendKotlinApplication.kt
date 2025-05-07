package app.hyuabot.backend

import com.ulisesbocchio.jasyptspringboot.annotation.EncryptablePropertySource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EncryptablePropertySource(name = "EncryptedProperties", value = ["classpath:encrypted.properties"])
class HyuabotBackendKotlinApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<HyuabotBackendKotlinApplication>(*args)
        }
    }
}