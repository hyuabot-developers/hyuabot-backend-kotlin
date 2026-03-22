package app.hyuabot.backend.utility

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@DgsComponent
class ScalarRegistration {
    @DgsRuntimeWiring
    fun addScalar(builder: RuntimeWiring.Builder): RuntimeWiring.Builder =
        builder
            .scalar(ExtendedScalars.Date)
            .scalar(ExtendedScalars.LocalTime)
            .scalar(koreaDateTimeScalar())

    private fun koreaDateTimeScalar() =
        GraphQLScalarType
            .newScalar()
            .name("DateTime")
            .description("DateTime Scalar (KST)")
            .coercing(
                object : Coercing<ZonedDateTime, String> {
                    private val zone = ZoneId.of("Asia/Seoul")
                    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

                    override fun serialize(
                        dataFetcherResult: Any,
                        graphQLContext: GraphQLContext,
                        locale: Locale,
                    ): String? =
                        when (dataFetcherResult) {
                            is ZonedDateTime -> dataFetcherResult.withZoneSameInstant(zone).format(formatter)
                            is String ->
                                try {
                                    ZonedDateTime.parse(dataFetcherResult, formatter).withZoneSameInstant(zone).format(formatter)
                                } catch (e: java.time.format.DateTimeParseException) {
                                    throw CoercingSerializeException("Unable to serialize '$dataFetcherResult' as ZonedDateTime: ${e.message}")
                                }
                            else -> throw CoercingSerializeException("Unable to serialize $dataFetcherResult as ZonedDateTime")
                        }

                    override fun parseValue(
                        input: Any,
                        graphQLContext: GraphQLContext,
                        locale: Locale,
                    ): ZonedDateTime? =
                        when (input) {
                            is String -> ZonedDateTime.parse(input, formatter).withZoneSameInstant(zone)
                            else -> throw CoercingParseValueException("Unable to parse $input as ZonedDateTime")
                        }

                    override fun parseLiteral(
                        input: Value<*>,
                        variables: CoercedVariables,
                        graphQLContext: GraphQLContext,
                        locale: Locale,
                    ): ZonedDateTime? =
                        when (input) {
                            is graphql.language.StringValue -> {
                                ZonedDateTime
                                    .parse(
                                        input.value.toString(),
                                        formatter,
                                    ).withZoneSameInstant(zone)
                            }

                            else -> {
                                throw CoercingParseLiteralException("Unable to parse literal $input as ZonedDateTime")
                            }
                        }
                },
            ).build()
}
