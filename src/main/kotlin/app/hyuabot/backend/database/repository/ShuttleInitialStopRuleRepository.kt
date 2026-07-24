package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleInitialStopRule
import org.springframework.data.jpa.repository.JpaRepository

interface ShuttleInitialStopRuleRepository : JpaRepository<ShuttleInitialStopRule, Int> {
    fun findAllByOrderByPriorityDescSeqAsc(): List<ShuttleInitialStopRule>

    fun findByPeriodTypeInAndWeekdayInAndEnabledTrueOrderByPriorityDescSeqAsc(
        periodTypes: Collection<String>,
        weekdays: Collection<Boolean>,
    ): List<ShuttleInitialStopRule>
}
