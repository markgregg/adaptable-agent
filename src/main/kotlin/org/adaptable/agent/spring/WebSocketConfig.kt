package org.adaptable.agent.spring

import org.adaptable.agent.configuration.ServiceConfiguration
import org.adaptable.agent.controllers.ClientController
import org.adaptable.agent.controllers.WebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry


@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val serviceConfiguration: ServiceConfiguration,
    private val clientController: ClientController
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(socketHandler(), "**")
    }

    @Bean
    fun socketHandler(): WebSocketHandler {
        return WebSocketHandler(serviceConfiguration, clientController)
    }

}