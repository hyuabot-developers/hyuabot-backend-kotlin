package app.hyuabot.backend.utility

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.schema.CoercingSerializeException
import graphql.schema.idl.RuntimeWiring
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ScalarRegistrationTest {
    private val scalarRegistration = ScalarRegistration()
    private val scalar =
        scalarRegistration
            .addScalar(
                RuntimeWiring.newRuntimeWiring(),
            ).build()
            .scalars["DateTime"]!!
    private val coercing = scalar.coercing
    private val context = GraphQLContext.getDefault()
    private val locale = Locale.getDefault()

    @Test
    @DisplayName("ZonedDateTime 직렬화")
    fun testSerializeZonedDateTime() {
        val input = ZonedDateTime.parse("2023-10-31T12:34:56+09:00")
        val result = coercing.serialize(input, context, locale)
        assertNotNull(result)
        assertEquals("2023-10-31T12:34:56+09:00", result)
    }

    @Test
    @DisplayName("String 직렬화")
    fun testSerializeString() {
        val input = "2023-10-31T12:34:56+09:00"
        val result = coercing.serialize(input, context, locale)
        assertNotNull(result)
    }

    @Test
    @DisplayName("직렬화 실패")
    fun testSerializeInvalid() {
        val input = 123
        assertThrows<CoercingSerializeException> { coercing.serialize(input, context, locale) }
    }

    @Test
    @DisplayName("String 파싱")
    fun testParseValueString() {
        val input = "2023-10-31T12:34:56+09:00"
        val result = coercing.parseValue(input, context, locale)
        assertNotNull(result)
    }

    @Test
    @DisplayName("파싱 실패")
    fun testParseValueInvalid() {
        val input = 123
        assertThrows<CoercingSerializeException> { coercing.parseValue(input, context, locale) }
    }

    @Test
    @DisplayName("리터럴 파싱")
    fun testParseLiteral() {
        val input = StringValue.of("2023-10-31T12:34:56+09:00")
        val result = coercing.parseLiteral(input, CoercedVariables.emptyVariables(), context, locale)
        assertNotNull(result)
    }

    @Test
    @DisplayName("리터럴 파싱 실패")
    fun testParseLiteralInvalid() {
        val input = IntValue.of(123)
        assertThrows<CoercingSerializeException> { coercing.parseLiteral(input, CoercedVariables.emptyVariables(), context, locale) }
    }
}
