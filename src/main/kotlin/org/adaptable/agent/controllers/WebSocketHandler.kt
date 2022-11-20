package org.adaptable.agent.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.adaptable.agent.configuration.ServiceConfiguration
import org.adaptable.agent.utils.Utility.sendMessage
import org.adaptable.agent.web.SocketService
import org.adaptable.common.protocol.EndPointRequest
import org.adaptable.common.web.WebRequest
import org.adaptable.common.web.WebResponse
import org.slf4j.LoggerFactory
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.AbstractWebSocketHandler
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class WebSocketHandler(
    private val serviceConfiguration: ServiceConfiguration,
    private val clientController: ClientController
) : AbstractWebSocketHandler() {
    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketHandler::class.java)
    }
    private val clientControllerUrl =  "/agentClient/api"
    private val sessionMap = ConcurrentHashMap<WebSocketSession, SocketService>()

    @Throws(InterruptedException::class, IOException::class)
    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        handleMessage(session, message)
    }

    @Throws(InterruptedException::class, IOException::class)
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        if( session.uri?.path == clientControllerUrl ) {
            clientController.processMessage(session, message)
            return
        }
        handleMessage(session, message)
    }

    @Throws(Exception::class)
    override fun afterConnectionEstablished(session: WebSocketSession) {
        if( session.uri?.path == clientControllerUrl
            || serviceConfiguration.socketEndPoints.containsKey(session.uri?.path)) {
            return //client of agent or extension
        }
        val webSocket = serviceConfiguration.endPoints
            .values
            .filterIsInstance<SocketService>()
            .firstOrNull {
                it.url.matches(session.uri?.path ?: "") &&
                        it.port == session.localAddress?.port &&
                        !it.unavailable.get()
            }
        if( webSocket == null) {
            logger.info("Can't find endpoint, closing session")
            session.close()
            return
        }
        logger.info("Session connected to ${session.uri?.path}-${session.localAddress?.port}")
        webSocket.setSession(SocketSession(session))
        sessionMap[session] = webSocket
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        super.afterConnectionClosed(session, status)
        if( session.uri?.path?.contains("agentClient/api") == true ) {
            clientController.notifySessionClose(session)
        }
        sessionMap[session]?.clearSession()
        sessionMap.remove(session)
    }

    private fun handleMessage(session: WebSocketSession, message: AbstractWebSocketMessage<*>) {
        val webSocket = sessionMap[session]
        if( webSocket != null ) {
            val request = WebRequest(
                UUID.randomUUID(),
                mapOf("url" to (session.uri?.path ?: "")),
                webSocket.url.parameters(session.uri?.path ?: ""),
                try {
                    when (message) {
                        is TextMessage -> { webSocket.messageConverter?.convert(message.payload)
                            ?: message.payload }
                        is BinaryMessage -> { webSocket.messageConverter?.convert(message.payload)
                                ?: StandardCharsets.UTF_8.decode(message.payload).toString() }
                        else -> { "" }
                    }
                } catch (e: Exception) {
                    ""
                }
            )
            logger.info("Request = $request")
            val response = webSocket.processRequest(request) as? WebResponse<*>
            if( response != null ) {
                sendMessage(response, session)
            }
        } else  if( message is TextMessage) {
            try {
                val endPointMessage = jacksonObjectMapper().readValue(message.payload, EndPointRequest::class.java)
                val endPont = serviceConfiguration.socketEndPoints[session.uri?.path]
                    ?.get(endPointMessage.endPointId)
                val response = endPont?.processRequest(endPointMessage.request)
                if( response != null ) {
                    session.sendMessage(TextMessage(jacksonObjectMapper().writeValueAsString(response)))
                }
            } catch (e: Exception) {
                logger.error("Unable to handle message for url: ${session.uri?.path}, reason: ${e.message}")
            }
        }
    }


}