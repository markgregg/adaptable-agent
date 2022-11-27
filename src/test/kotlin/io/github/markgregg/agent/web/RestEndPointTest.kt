package io.github.markgregg.agent.web

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.github.markgregg.common.api.Rule
import org.springframework.http.HttpMethod

class RestEndPointTest : FunSpec({

    test("port") {
        val endPoint = constructRestEndPoint()
        endPoint.port shouldBe 8000
    }

    test("url") {
        val endPoint = constructRestEndPoint()
        endPoint.url.matches("test") shouldBe true
    }

    test("method") {
        val endPoint = constructRestEndPoint()
        endPoint.method shouldBe HttpMethod.GET
    }

    test("initialise") {
        val endPoint = constructRestEndPoint()
        endPoint.initialise()
    }


}) {
    companion object {
        private fun constructRestEndPoint(rules: List<Rule> = emptyList()): RestEndPoint {
            return RestEndPoint("test", rules, mapOf("port" to "8000", "url" to "test", "method" to "GET"),null )
        }
    }
}
