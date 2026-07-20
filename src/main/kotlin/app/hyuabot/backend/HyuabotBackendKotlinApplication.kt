package app.hyuabot.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HyuabotBackendKotlinApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<HyuabotBackendKotlinApplication>(*args)
        }
    }
}
