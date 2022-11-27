package io.github.markgregg.agent.controllers

import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

interface ClientController {
    fun processMessage(session: WebSocketSession, message: TextMessage)
    fun notifySessionClose(session: WebSocketSession)
}