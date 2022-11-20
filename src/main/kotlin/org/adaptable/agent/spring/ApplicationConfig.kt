package org.adaptable.agent.spring

import org.adaptable.agent.configPath
import org.adaptable.agent.configuration.ServiceConfigLoader
import org.adaptable.agent.configuration.ServiceConfiguration
import org.adaptable.agent.configuration.ServiceConfigurationImpl
import org.adaptable.agent.controllers.ClientControllerImpl
import org.adaptable.agent.utils.TypeDiscovery
import org.adaptable.agent.utils.TypeDiscoveryImpl
import org.adaptable.common.api.ServiceDefinition
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
    ): ServiceConfiguration {
        return ServiceConfigurationImpl(serviceDefinition, typeDiscovery)
    }

    @Bean
    fun clientController(serviceConfiguration: ServiceConfiguration ): ClientControllerImpl {
        return ClientControllerImpl(serviceConfiguration)
    }

    @Bean
    fun typeDiscovery(): TypeDiscovery {
        return TypeDiscoveryImpl()
    }
}