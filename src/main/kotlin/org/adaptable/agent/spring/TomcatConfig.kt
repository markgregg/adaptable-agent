package org.adaptable.agent.spring

import org.adaptable.agent.configuration.ServiceConfiguration
import org.adaptable.agent.controllers.ServerHandlerMapping
import org.apache.catalina.connector.Connector
import org.apache.coyote.http11.Http11NioProtocol
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.Ssl
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class TomcatConfig() {

    @Bean
    fun serverHandlerMapping(serviceConfiguration: ServiceConfiguration): ServerHandlerMapping {
        val handler =  ServerHandlerMapping(serviceConfiguration)
        handler.order = Ordered.HIGHEST_PRECEDENCE
        return handler
    }

    @Bean
    fun servletContainer(
        serverProperties: ServerProperties,
        serviceConfiguration: ServiceConfiguration
    ): ServletWebServerFactory {
        if( serviceConfiguration.keyStorePath != null) {
            val ssl = Ssl()
            val keystorePassword = serviceConfiguration.keySorePassword
            ssl.keyPassword = keystorePassword
            ssl.keyAlias = serviceConfiguration.keyAlias
            ssl.keyStoreType = serviceConfiguration.keyStoreType
            ssl.keyStore = serviceConfiguration.keyStorePath
            ssl.keyStorePassword = serviceConfiguration.keySorePassword
            System.setProperty("server.ssl.key-store-password", keystorePassword!!)
            serverProperties.ssl = ssl
        }
        val tomcat = TomcatServletWebServerFactory()
        additionalConnectors(serviceConfiguration).forEach { tomcat.addAdditionalTomcatConnectors(it) }
        return tomcat
    }

    private fun additionalConnectors(serviceConfiguration: ServiceConfiguration): List<Connector> {
        val connectors = ArrayList<Connector>()

        if( serviceConfiguration.ports != null ) {
            for (port in serviceConfiguration.ports!!) {
                val connector = Connector("org.apache.coyote.http11.Http11NioProtocol")
                connector.scheme = "http"
                connector.port = port
                connectors.add(connector)
            }
        }

        if( serviceConfiguration.securePorts != null) {
            for (port in serviceConfiguration.securePorts!!) {
                val connector = Connector("org.apache.coyote.http11.Http11NioProtocol")
                connector.scheme = "https"
                connector.port = port
                connector.secure = true
                val protocol = connector.protocolHandler as Http11NioProtocol
                protocol.isSSLEnabled = true
                protocol.keystoreFile = serviceConfiguration.keyStorePath
                protocol.keystorePass = serviceConfiguration.keySorePassword
                protocol.keystoreType = serviceConfiguration.keyStoreType
                protocol.setProperty("keystoreProvider", "keystoreProvider")
                protocol.keyAlias = serviceConfiguration.keyAlias
                connectors.add(connector)
            }
        }
        return connectors
    }
}