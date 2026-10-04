package app.hyuabot.backend.graphql

import graphql.language.InputObjectTypeDefinition
import graphql.language.ObjectTypeDefinition
import graphql.parser.Parser
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.validation.Validator
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClientOperationCompatibilityTest {
    private fun readResource(path: String): String =
        checkNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }
            .bufferedReader()
            .use { it.readText() }

    @Test
    fun `stage one schema preserves every previous object and input field`() {
        val oldSchemaText = readResource("/schema/schema-before-stage1.graphql.snapshot")
        val newSchemaText = readResource("/schema/schema.graphqls")
        val oldTypes = SchemaParser().parse(oldSchemaText).types()
        val newTypes = SchemaParser().parse(newSchemaText).types()

        oldTypes.forEach { (typeName, oldType) ->
            when (oldType) {
                is ObjectTypeDefinition -> {
                    val newType = newTypes[typeName] as? ObjectTypeDefinition
                    assertNotNull(newType, "Existing GraphQL object $typeName was removed")
                    oldType.fieldDefinitions.forEach { oldField ->
                        val newField = newType.fieldDefinitions.firstOrNull { it.name == oldField.name }
                        assertNotNull(newField, "Existing field $typeName.${oldField.name} was removed")
                        assertEquals(
                            oldField.type.toString(),
                            newField.type.toString(),
                            "Existing field $typeName.${oldField.name} changed type",
                        )
                    }
                }
                is InputObjectTypeDefinition -> {
                    val newType = newTypes[typeName] as? InputObjectTypeDefinition
                    assertNotNull(newType, "Existing GraphQL input $typeName was removed")
                    oldType.inputValueDefinitions.forEach { oldField ->
                        val newField = newType.inputValueDefinitions.firstOrNull { it.name == oldField.name }
                        assertNotNull(newField, "Existing input field $typeName.${oldField.name} was removed")
                        assertEquals(
                            oldField.type.toString(),
                            newField.type.toString(),
                            "Existing input field $typeName.${oldField.name} changed type",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `legacy Android and iOS client operations validate against the current schema`() {
        val schemaText = readResource("/schema/schema.graphqls")
        val schema = SchemaGenerator.createdMockedSchema(schemaText)
        val validator = Validator()
        val operationPaths =
            readResource("/client-operations/manifest.txt")
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()

        val expectedSourceCounts = mapOf("android-app" to 26, "android-watch" to 1, "ios" to 27)
        val actualSourceCounts = operationPaths.groupingBy { it.substringBefore('/') }.eachCount()
        assertEquals(expectedSourceCounts, actualSourceCounts, "The legacy operation fixture is incomplete")

        operationPaths.forEach { operationPath ->
            val operationSource = readResource("/client-operations/$operationPath")
            val document = Parser().parseDocument(operationSource)
            val validationErrors = validator.validateDocument(schema, document, Locale.ROOT)

            assertTrue(
                validationErrors.isEmpty(),
                "$operationPath is not valid against the current GraphQL schema:\n${validationErrors.joinToString("\n")}",
            )
        }
    }
}
