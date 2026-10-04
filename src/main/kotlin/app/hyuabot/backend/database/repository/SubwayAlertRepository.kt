package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayAlert
import org.springframework.data.jpa.repository.JpaRepository

interface SubwayAlertRepository : JpaRepository<SubwayAlert, String>
