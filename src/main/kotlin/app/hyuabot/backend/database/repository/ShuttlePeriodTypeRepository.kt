package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttlePeriodType
import org.springframework.data.jpa.repository.JpaRepository

interface ShuttlePeriodTypeRepository : JpaRepository<ShuttlePeriodType, String>
