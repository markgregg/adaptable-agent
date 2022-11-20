package org.adaptable.agent.controllers

import org.adaptable.common.api.Response
import org.adaptable.common.api.interfaces.Session
import org.adaptable.common.web.WebResponse
import org.slf4j.LoggerFactory
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.*

class SocketSession(
    private val session: WebSocketSession
) : Session {
    companion object {
        private val logger = LoggerFactory.getLogger(SocketSession::class.java)
    }
    override val sessionId: String = UUID.randomUUID().toString()

    override fun sendResponse(response: Response) {
        try {
            val webResponse = response as? WebResponse<*>
            if( webResponse == null) {
                logger.info("${response.javaClass.name} isn't handled")
                return
            }
            val payload = webResponse.payload()
            if( payload is String) {
                session.sendMessage(TextMessage(payload))
            } else if( payload is ByteArray ) {
                session.sendMessage(BinaryMessage(payload))
            } else {
                logger.info("Only text or binary payloads are handled")
            }
        } catch (e: Exception) {
            logger.error("Failed to send message, reason: ${e.message}")
        }
    }
}