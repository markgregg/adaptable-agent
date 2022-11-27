package io.github.markgregg.agent.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.markgregg.common.api.ServiceDefinition
import java.io.InputStream
import java.nio.charset.StandardCharsets

object ServiceConfigLoader {
     fun load(configStream: InputStream): ServiceDefinition {
         val text = String(configStream.readAllBytes(), StandardCharsets.UTF_8)
         return jacksonObjectMapper().readValue(text, ServiceDefinition::class.java)
    }
}

