package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRouteStop

interface BusRouteStopRepositoryCustom {
    fun fetchBusRouteStopPairs(keys: Set<Pair<Int, Int>>): List<BusRouteStop>
}
