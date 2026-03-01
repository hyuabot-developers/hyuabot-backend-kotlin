package app.hyuabot.backend.notice.controller

import app.hyuabot.backend.codegen.types.Notice
import app.hyuabot.backend.codegen.types.NoticeCategory
import app.hyuabot.backend.codegen.types.NoticeInput
import app.hyuabot.backend.notice.NoticeService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.time.ZonedDateTime

@DgsComponent
class NoticeDataFetcher(
    private val noticeService: NoticeService,
) {
    @DgsQuery
    fun notices(
        @InputArgument input: NoticeInput?
    ): List<NoticeCategory> {
        return noticeService
            .fetchNotices(
                category = input?.category,
                language = input?.language,
                since = input?.timestamp,
                currentTime = ZonedDateTime.now(),
            ).map {
                NoticeCategory(
                    seq = it.id!!,
                    name = it.name,
                    notices =
                        it.notice.map { notice ->
                            Notice(
                                seq = notice.id!!,
                                title = notice.title,
                                url = notice.url,
                                language = notice.language,
                                expiredAt = notice.expiredAt,
                                userID = notice.userID,
                            )
                        },
                )
            }
    }
}
