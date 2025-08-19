package app.hyuabot.backend.cafeteria.domain

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class MenuRequest(
    @get:Schema(description = "날짜", example = "2023-10-01")
    val date: LocalDate,
    @get:Schema(description = "조식/중식/석식 구분", example = "조식")
    val type: String,
    @get:Schema(description = "메뉴", example = "김치찌개, 계란찜, 밥")
    val food: String,
    @get:Schema(description = "가격", example = "3000")
    val price: String,
)
