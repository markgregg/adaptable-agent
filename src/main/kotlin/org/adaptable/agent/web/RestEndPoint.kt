package org.adaptable.agent.web

import org.adaptable.common.api.Request
import org.adaptable.common.api.Rule
import org.adaptable.common.api.annotations.EndPoint
import org.adaptable.common.api.endpoints.BaseEndPoint
import org.adaptable.common.api.interfaces.MessageConverter
import org.adaptable.common.web.TextResponse
import org.springframework.http.HttpMethod

@EndPoint("Rest")
open class RestEndPoint(
    id: String,
    rules: List<Rule>,
    properties: Map<String,String>,
    messageConverter: MessageConverter?
) : BaseEndPoint(id, rules, messageConverter), RestService {
    final override val port: Int
    final override val url: Url
    final override val method: HttpMethod

    init {
        port = properties["port"]!!.toInt()
        url = Url(properties["url"]!!)
        method = HttpMethod.valueOf(properties["method"]!!)
    }

    override fun initialise() {
    }

    override fun handleRequest(request: Request): TextResponse? {
        return null
    }


}