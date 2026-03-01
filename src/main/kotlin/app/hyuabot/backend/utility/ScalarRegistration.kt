package app.hyuabot.backend.utility

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring

@DgsComponent
class ScalarRegistration {
    @DgsRuntimeWiring
    fun addScalar(builder: RuntimeWiring.Builder): RuntimeWiring.Builder = builder.scalar(ExtendedScalars.DateTime)
}
