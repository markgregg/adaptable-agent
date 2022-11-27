package io.github.markgregg.agent.web

import io.github.markgregg.common.api.Request
import io.github.markgregg.common.api.Rule
import io.github.markgregg.common.api.annotations.EndPoint
import io.github.markgregg.common.api.endpoints.BaseEndPoint
import io.github.markgregg.common.api.interfaces.MessageConverter
import io.github.markgregg.common.web.TextResponse
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