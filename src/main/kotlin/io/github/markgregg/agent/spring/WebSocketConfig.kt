package io.github.markgregg.agent.spring

import io.github.markgregg.agent.configuration.ServiceConfiguration
import io.github.markgregg.agent.controllers.ClientController
import io.github.markgregg.agent.controllers.WebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry


@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val serviceConfiguration: io.github.markgregg.agent.configuration.ServiceConfiguration,
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