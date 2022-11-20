package org.adaptable.agent.web

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.adaptable.common.api.Rule

class SocketEndPointTest : FunSpec({

    test("port") {
        val endPoint = constructSocketEndPoint()
        endPoint.port shouldBe 8000
    }

    test("url") {
        val endPoint = constructSocketEndPoint()
        endPoint.url.matches("test") shouldBe true
    }

    test("initialise") {
        val endPoint = constructSocketEndPoint()
        endPoint.initialise()
    }

}) {
    companion object {
        private fun constructSocketEndPoint(rules: List<Rule> = emptyList()): SocketEndPoint {
            return SocketEndPoint("test", rules, mapOf("port" to "8000", "url" to "test"), null )
        }
    }
}

