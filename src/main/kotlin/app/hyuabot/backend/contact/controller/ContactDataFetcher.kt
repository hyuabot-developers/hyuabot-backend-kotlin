package app.hyuabot.backend.contact.controller

import app.hyuabot.backend.codegen.types.Phonebook
import app.hyuabot.backend.codegen.types.PhonebookCategory
import app.hyuabot.backend.codegen.types.PhonebookEntry
import app.hyuabot.backend.codegen.types.PhonebookInput
import app.hyuabot.backend.contact.ContactService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import graphql.schema.DataFetchingEnvironment

@DgsComponent
class ContactDataFetcher(
    private val contactService: ContactService,
) {
    @DgsQuery
    fun phonebook(
        @InputArgument input: PhonebookInput?,
        env: DgsDataFetchingEnvironment,
    ) {
        input?.let {
            env.graphQlContext.put("input", it)
        }
        Phonebook(version = "", categories = emptyList())
    }

    @DgsData(parentType = "Phonebook")
    fun version(): String = contactService.getContactVersion().name

    @DgsData(parentType = "Phonebook")
    fun categories(env: DataFetchingEnvironment): List<PhonebookCategory> {
        val input = env.graphQlContext.get<PhonebookInput>("input")
        return contactService
            .fetchContacts(
                category = input?.category,
                campus = input?.campus,
                name = input?.name,
                phone = input?.phone,
            ).map { category ->
                PhonebookCategory(
                    seq = category.id!!,
                    name = category.name,
                    entries =
                        category.contact.map { contact ->
                            PhonebookEntry(
                                seq = contact.id!!,
                                name = contact.name,
                                phone = contact.phone,
                            )
                        },
                )
            }
    }
}
