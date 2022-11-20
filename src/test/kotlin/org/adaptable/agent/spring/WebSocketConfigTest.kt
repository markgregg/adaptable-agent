package org.adaptable.agent.spring

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import org.adaptable.agent.configuration.ServiceConfiguration
import org.adaptable.agent.configuration.ServiceConfigurationImpl
import org.adaptable.agent.controllers.ClientController
import org.adaptable.agent.utils.TypeDiscoveryMock.typeDiscoveryMock
import org.adaptable.common.api.EndPoint
import org.adaptable.common.api.ServiceDefinition
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.verify
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

class WebSocketConfigTest : FunSpec({


    test("registerWebSocketHandlers") {
        val registry = mock(WebSocketHandlerRegistry::class.java)
        WebSocketConfig(getConfig(), mock(ClientController::class.java)).registerWebSocketHandlers(registry)
        verify(registry).addHandler(isA(),eq("**"))
    }

    test("socketHandler") {
        WebSocketConfig(getConfig(),mock(ClientController::class.java)).socketHandler().shouldNotBeNull()
    }
}) {
    companion object {
        private fun getConfig(): ServiceConfiguration {
            return ServiceConfigurationImpl(
                ServiceDefinition(
                    null, null, null, null, null, listOf(1110, 1111), null, null,
                    listOf(
                        EndPoint(
                        "id",
                        "Rest",
                            mapOf(
                                "port" to "9089",
                                "url" to "/api/secure",
                                "method" to "GET"
                            ), null, null, false
                    )
                    ),
                ),
                typeDiscoveryMock()
            )
        }
    }
}
