package space.dawdawich.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import space.dawdawich.controller.model.analyzer.CreateCandleTailAnalyzerRequest
import space.dawdawich.controller.model.analyzer.CreateGridAnalyzerRequest

@Configuration
class MapperConfiguration {
    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerSubtypes(NamedType(CreateGridAnalyzerRequest::class.java, "grid-table-strategy"))
        registerSubtypes(NamedType(CreateCandleTailAnalyzerRequest::class.java, "candle-tail-strategy"))
    }
}
