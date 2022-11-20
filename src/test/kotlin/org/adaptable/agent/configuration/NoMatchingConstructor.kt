package org.adaptable.agent.configuration

import org.adaptable.common.api.Request
import org.adaptable.common.api.Response
import org.adaptable.common.api.annotations.EndPoint
import org.adaptable.common.api.endpoints.BaseEndPoint
import org.adaptable.common.web.TextResponse

@EndPoint("NoMatchingConstructor")
class NoMatchingConstructor() : BaseEndPoint("", emptyList(),  null) {

    override fun handleRequest(request: Request): Response? {
        TODO("Not yet implemented")
    }

    override fun initialise() {
        TODO("Not yet implemented")
    }
}