package io.github.markgregg.agent.configuration

import io.github.markgregg.common.api.Request
import io.github.markgregg.common.api.Response
import io.github.markgregg.common.api.annotations.EndPoint
import io.github.markgregg.common.api.endpoints.BaseEndPoint
import io.github.markgregg.common.web.TextResponse

@EndPoint("NoMatchingConstructor")
class NoMatchingConstructor() : BaseEndPoint("", emptyList(),  null) {

    override fun handleRequest(request: Request): Response? {
        TODO("Not yet implemented")
    }

    override fun initialise() {
        TODO("Not yet implemented")
    }
}