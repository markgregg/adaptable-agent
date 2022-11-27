package io.github.markgregg.agent.controllers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.github.markgregg.agent.configuration.ServiceConfiguration
import io.github.markgregg.agent.configuration.ServiceConfigurationImpl
import io.github.markgregg.agent.utils.TypeDiscoveryMock.typeDiscoveryMock
import io.github.markgregg.agent.web.SocketService
import io.github.markgregg.agent.web.Url
import io.github.markgregg.common.api.EndPoint
import io.github.markgregg.common.api.ServiceDefinition
import io.github.markgregg.common.api.interfaces.MessageConverter
import io.github.markgregg.common.web.BinaryResponse
import io.github.markgregg.common.web.TextResponse
import io.github.markgregg.common.web.WebRequest
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketHandlerTest : FunSpec() {

    init {
        test("Session closed if end point not found") {
            val session = getSession(7777, "ws://localhost:7777/test")
            callAfterConnectionEstablished(webSocketHandler(getServiceConfiguration()), session)
            verify(session).close()
        }

        test("Session is not closed if end point is for agentClient") {
            val session = getSession(7777, "ws://localhost:7777/agentClient/api")
            callAfterConnectionEstablished(webSocketHandler(getServiceConfiguration()), session)
            verify(session, times(0)).close()
        }

        test("Session set if end point found") {
            val session = getSession(8098, "ws://localhost:8098/api/secure")
            val configuration = mockServiceConfiguration()
            callAfterConnectionEstablished(webSocketHandler(configuration), session)
            verify( configuration.endPoints["id"] as SocketService).setSession(isA())
        }

        test("handle TextMessage does nothing when no end point found") {
            val session = getSession(8091, "ws://localhost:8091/api/secure")

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(null)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            callHandleTextMessage(webSocketHandler(serviceConfiguration), session, TextMessage("test"))
        }

        test("handle TextMessage calls client if for agentClient api") {
            val session = getSession(8091, "ws://localhost:8091/agentClient/api")

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(null)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val client = mock(ClientController::class.java)
            callHandleTextMessage(webSocketHandler(serviceConfiguration, client), session, TextMessage("test"))

            verify(client).processMessage(eq(session), isA())
        }

        test("handle TextMessage does nothing when no response found") {
            val session = getSession(8098, "ws://localhost:8098/api/secure")

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(null)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val webSocketHandler = webSocketHandler(serviceConfiguration)
            callAfterConnectionEstablished(webSocketHandler, session)
            callHandleTextMessage(webSocketHandler, session, TextMessage("test"))

            verify(endPoint).processRequest(isA())
        }

        test("handle TextMessage returns response when found") {
            val session = getSession(8098, "ws://localhost:8098/api/secure")

            val returnResponse = TextResponse(
                HttpStatus.OK.value(),
                "Test message",
               mapOf("name" to "value"),
                null
            )

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val webSocketHandler = webSocketHandler(serviceConfiguration)
            callAfterConnectionEstablished(webSocketHandler, session)
            callHandleTextMessage(webSocketHandler, session, TextMessage("{\"field\":\"test\"}"))

            verify(endPoint).processRequest(isA())
            verify(session).sendMessage(isA())
        }

        test("handle extracts url parameters") {
            val session = getSession(8098, "ws://localhost:8098/api/secure/value1/value2")

            val returnResponse = TextResponse(
                HttpStatus.OK.value(),
                "Test message",
               mapOf("name" to "value"),
                null
            )

            val endPoint = getEndPoint(url = "/api/secure/{param1}/{param2}")
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val webSocketHandler = webSocketHandler(serviceConfiguration)
            callAfterConnectionEstablished(webSocketHandler, session)
            callHandleTextMessage(webSocketHandler, session, TextMessage("{\"field\":\"test\"}"))

            verify(endPoint).processRequest(isA())
            verify(session).sendMessage(isA())
            val captor = argumentCaptor<WebRequest>()
            verify(endPoint).processRequest(captor.capture())
            captor.firstValue.parameters shouldBe mapOf("param1" to "value1", "param2" to "value2")
        }

        test("convert message if a message converter found for text message") {
            val session = getSession(8098, "ws://localhost:8098/api/secure")

            val returnResponse = TextResponse(
                HttpStatus.OK.value(),
                "Test message",
               mapOf("name" to "value"),
                null
            )

            val messageConverter = mock(MessageConverter::class.java)

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)
            whenever(endPoint.messageConverter).thenReturn(messageConverter)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val webSocketHandler = webSocketHandler(serviceConfiguration)
            callAfterConnectionEstablished(webSocketHandler, session)
            callHandleTextMessage(webSocketHandler, session, TextMessage("{\"field\":\"test\"}"))

            verify(endPoint).processRequest(isA())
            verify(session).sendMessage(isA())
            verify(messageConverter).convert(isA())
        }

        test("handle BinaryMessage returns response when found") {
            val session = getSession(8098, "ws://localhost:8098/api/secure")

            val returnResponse = BinaryResponse(
                HttpStatus.OK.value(),
                Base64.getEncoder().encodeToString("Test message".encodeToByteArray()),
               mapOf("name" to "value"),
                null
            )

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val webSocketHandler = webSocketHandler(serviceConfiguration)
            callAfterConnectionEstablished(webSocketHandler, session)
            callHandleBinaryMessage(
                webSocketHandler,
                session,
                BinaryMessage(ByteBuffer.wrap("{\"field\":\"test\"}".encodeToByteArray()))
            )

            verify(endPoint).processRequest(isA())
            verify(session).sendMessage(isA())
        }

        test("convert message if a message converter found for binary message") {
            val session = getSession(8098, "ws://localhost:8098/api/secure")

            val returnResponse = BinaryResponse(
                HttpStatus.OK.value(),
                Base64.getEncoder().encodeToString("Test message".encodeToByteArray()),
               mapOf("name" to "value"),
                null
            )

            val messageConverter = mock(MessageConverter::class.java)

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)
            whenever(endPoint.messageConverter).thenReturn(messageConverter)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val webSocketHandler = webSocketHandler(serviceConfiguration)
            callAfterConnectionEstablished(webSocketHandler, session)
            callHandleBinaryMessage(
                webSocketHandler,
                session,
                BinaryMessage(ByteBuffer.wrap("{\"field\":\"test\"}".encodeToByteArray()))
            )

            verify(endPoint).processRequest(isA())
            verify(session).sendMessage(isA())
            verify(messageConverter).convert(isA())
        }
    }

    companion object {

        private fun mockServiceConfiguration(): io.github.markgregg.agent.configuration.ServiceConfiguration {
            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.ports).thenReturn(listOf(8099))
            val socket = mock(SocketService::class.java)
            whenever(socket.url).thenReturn(Url("/api/secure"))
            whenever(socket.port).thenReturn(8098)
            whenever(socket.unavailable).thenReturn(AtomicBoolean())
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to socket))
            return serviceConfiguration
        }

        private fun getServiceConfiguration(): io.github.markgregg.agent.configuration.ServiceConfiguration {
            return io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceDefinition(
                    null, null, null, null, null, listOf(8099), null, null,
                    listOf(
                        EndPoint(
                            "id",
                            "Socket",
                            mapOf(
                                "port" to "8098",
                                "url" to "/api/secure",
                            ), null, null, false
                        )
                    ),
                ),
                typeDiscoveryMock()
            )
        }

        private fun webSocketHandler(
            serviceConfiguration: io.github.markgregg.agent.configuration.ServiceConfiguration,
            clientController: ClientController = mock(ClientController::class.java)
        ): WebSocketHandler {
            return  WebSocketHandler(serviceConfiguration, clientController)
        }

        private fun callAfterConnectionEstablished(
            webSocketHandler: WebSocketHandler,
            session: WebSocketSession
        ) : Any? {
            val method = WebSocketHandler::class.java.getDeclaredMethod("afterConnectionEstablished", WebSocketSession::class.java)
            return method.invoke(webSocketHandler, session)
        }

        private fun callHandleBinaryMessage(
            webSocketHandler: WebSocketHandler,
            session: WebSocketSession,
            binaryMessage: BinaryMessage
        ) : Any? {
            val method = WebSocketHandler::class.java.getDeclaredMethod("handleBinaryMessage", WebSocketSession::class.java, BinaryMessage::class.java)
            return method.invoke(webSocketHandler, session, binaryMessage)
        }

        private fun callHandleTextMessage(
            webSocketHandler: WebSocketHandler,
            session: WebSocketSession, textMessage: TextMessage
        ) : Any? {
            val method = WebSocketHandler::class.java.getDeclaredMethod("handleTextMessage", WebSocketSession::class.java, TextMessage::class.java)
            return method.invoke(webSocketHandler, session, textMessage)
        }

        private fun getSession(port: Int, url: String = "ws://localhost:8098/api/secure/value1/value2"): WebSocketSession {
            val session = mock(WebSocketSession::class.java)
            val address = InetSocketAddress(port)
            whenever(session.localAddress).thenReturn(address)
            whenever(session.uri).thenReturn(URI.create(url))
            return session
        }

        private fun getEndPoint(port: Int = 8098, url: String = "/api/secure", available: Boolean = false): SocketService {
            val endPoint = mock(SocketService::class.java)
            whenever(endPoint.url).thenReturn(Url(url))
            whenever(endPoint.port).thenReturn(port)
            whenever(endPoint.unavailable).thenReturn(AtomicBoolean(available))
            return endPoint
        }
    }
}
