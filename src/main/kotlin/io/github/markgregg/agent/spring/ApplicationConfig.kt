package io.github.markgregg.agent.spring

import io.github.markgregg.agent.configPath
import io.github.markgregg.agent.configuration.ServiceConfigLoader
import io.github.markgregg.agent.configuration.ServiceConfiguration
import io.github.markgregg.agent.configuration.ServiceConfigurationImpl
import io.github.markgregg.agent.controllers.ClientControllerImpl
import io.github.markgregg.agent.utils.TypeDiscovery
import io.github.markgregg.agent.utils.TypeDiscoveryImpl
import io.github.markgregg.common.api.ServiceDefinition
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream


@Configuration
class ApplicationConfig {
    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }

    @Bean
    fun serviceDefinition(): ServiceDefinition {
        try {
            return ServiceConfigLoader.load(FileInputStream(configPath!!))
        } catch( e: Exception) {
            logger.error("Failed to open config file - $configPath, reason: ${e.message}")
            throw e
        }
    }

    @Bean
    fun serviceConfiguration(
        serviceDefinition: ServiceDefinition,
        typeDiscovery: TypeDiscovery
    ): ServiceConfiguration =
        ServiceConfigurationImpl(serviceDefinition, typeDiscovery)

    @Bean
    fun clientController(serviceConfiguration: ServiceConfiguration ): ClientControllerImpl =
        ClientControllerImpl(serviceConfiguration)

    @Bean
    fun typeDiscovery(): TypeDiscovery =
        TypeDiscoveryImpl()
}