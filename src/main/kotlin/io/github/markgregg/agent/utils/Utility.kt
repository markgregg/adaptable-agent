package io.github.markgregg.agent.utils

import io.github.markgregg.common.api.Payload
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.nio.ByteBuffer

object Utility {

    internal fun sendMessage(message: Payload<*>, session: WebSocketSession) {
        if( message.payload() != null ) {
            val payload = when (message.payload()) {
                is ByteArray -> { BinaryMessage(ByteBuffer.wrap(message.payload() as ByteArray)) }
                is String -> { TextMessage(message.payload() as String) }
                else -> { null }
            }
            if (payload != null) {
                session.sendMessage(payload)
            }
        }
    }
}