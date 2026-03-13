package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.codegen.types.Cafeteria
import app.hyuabot.backend.codegen.types.CafeteriaInput
import app.hyuabot.backend.codegen.types.CafeteriaRunningTime
import app.hyuabot.backend.codegen.types.Menu
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDate

@DgsComponent
class CafeteriaDataFetcher(
    private val cafeteriaService: CafeteriaService,
    private val menuService: MenuService,
) {
    @DgsQuery
    fun cafeteria(
        @InputArgument input: CafeteriaInput,
        dfe: DataFetchingEnvironment,
    ): List<Cafeteria> {
        dfe.graphQlContext.put("date", input.date)
        return cafeteriaService.getCafeteriaList(campusID = input.campus).map {
            Cafeteria(
                seq = it.id,
                name = it.name,
                campus = it.campusID,
                latitude = it.latitude,
                longitude = it.longitude,
                runningTime =
                    CafeteriaRunningTime(
                        breakfast = it.breakfastTime,
                        lunch = it.lunchTime,
                        dinner = it.dinnerTime,
                    ),
                menus = emptyList(),
            )
        }
    }

    @DgsData(parentType = "Cafeteria")
    fun menus(dfe: DataFetchingEnvironment): List<Menu> {
        val source = dfe.getSource<Cafeteria>()
        val date = dfe.graphQlContext.get<LocalDate>("date")
        return menuService
            .getMenuList(
                cafeteriaID = source!!.seq,
                date = date,
                type = null,
            ).map {
                Menu(
                    seq = it.seq!!,
                    type = it.type,
                    food = it.food,
                    price = it.price,
                )
            }
    }
}
