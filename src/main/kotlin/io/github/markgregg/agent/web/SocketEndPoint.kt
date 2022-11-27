package io.github.markgregg.agent.web

import io.github.markgregg.common.api.Request
import io.github.markgregg.common.api.Response
import io.github.markgregg.common.api.Rule
import io.github.markgregg.common.api.annotations.EndPoint
import io.github.markgregg.common.api.endpoints.BaseEndPoint
import io.github.markgregg.common.api.endpoints.EventEndPoint
import io.github.markgregg.common.api.interfaces.MessageConverter
import io.github.markgregg.common.api.interfaces.Session
import io.github.markgregg.common.web.TextResponse
import java.util.concurrent.atomic.AtomicReference

@EndPoint("Socket")
open class SocketEndPoint(
    id: String,
    rules: List<Rule>,
    properties: Map<String,String>,
    messageConverter: MessageConverter?
) : BaseEndPoint(id, rules, messageConverter), EventEndPoint, SocketService {
    final override val port: Int
    final override val url: Url
    private val session = AtomicReference<Session?>()

    init {
        url = Url(properties["url"]!!)
        port = properties["port"]!!.toInt()
    }

    override fun initialise() {
    }

    override fun handleRequest(request: Request): TextResponse? {
        return null
    }

    override fun sendMessage(response: Response) {
        session.get()?.sendResponse(response)
    }


    override fun setSession(session: Session) {
        this.session.set(session)
    }

    override fun clearSession() {
        this.session.set(null)
    }
}