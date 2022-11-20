package org.adaptable.agent.web

import org.adaptable.common.api.Request
import org.adaptable.common.api.Response
import org.adaptable.common.api.Rule
import org.adaptable.common.api.annotations.EndPoint
import org.adaptable.common.api.endpoints.BaseEndPoint
import org.adaptable.common.api.endpoints.EventEndPoint
import org.adaptable.common.api.interfaces.MessageConverter
import org.adaptable.common.api.interfaces.Session
import org.adaptable.common.web.TextResponse
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