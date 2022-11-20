package org.adaptable.agent.spring

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.adaptable.agent.configuration.ServiceConfigurationImpl
import org.adaptable.agent.utils.TypeDiscoveryMock.typeDiscoveryMock
import org.adaptable.common.api.EndPoint
import org.adaptable.common.api.ServiceDefinition
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.core.Ordered

class TomcatConfigurationTest : FunSpec() {
    init {
        test("serverHandlerMapping") {
            val serviceConfiguration = ServiceConfigurationImpl(
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
            val mapping = TomcatConfig().serverHandlerMapping(serviceConfiguration)
            mapping.order shouldBe Ordered.HIGHEST_PRECEDENCE
        }

        test("simple tomcat server") {
            val serviceConfiguration = ServiceConfigurationImpl(
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
                            ), null, null,  false
                        )
                    ),
                ),
                typeDiscoveryMock()
            )

            val server = TomcatConfig().servletContainer(
                ServerProperties(),
                serviceConfiguration
            ) as TomcatServletWebServerFactory
            server.additionalTomcatConnectors.size shouldBe 2
            server.additionalTomcatConnectors[0].port shouldBe 1110
            server.additionalTomcatConnectors[0].secure shouldBe false
            server.additionalTomcatConnectors[1].port shouldBe 1111
            server.additionalTomcatConnectors[1].secure shouldBe false
        }

        test("secure tomcat server") {
            val serviceConfiguration = ServiceConfigurationImpl(
                ServiceDefinition(
                    null,
                    "PKCS12",
                    this.javaClass.classLoader.getResource("test.p12")!!.path,
                    "test",
                    "test",
                    null,
                    listOf(2110, 2111),
                    null,
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

            val server = TomcatConfig().servletContainer(
                ServerProperties(),
                serviceConfiguration
            ) as TomcatServletWebServerFactory
            server.additionalTomcatConnectors.size shouldBe 2
            server.additionalTomcatConnectors[0].port shouldBe 2110
            server.additionalTomcatConnectors[0].secure shouldBe true
            server.additionalTomcatConnectors[1].port shouldBe 2111
            server.additionalTomcatConnectors[1].secure shouldBe true
        }
    }
}

