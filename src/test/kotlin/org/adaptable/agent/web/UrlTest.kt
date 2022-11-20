package org.adaptable.agent.web

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UrlTest : FunSpec({

    test("matches a basic url") {
        val url = Url("/test/test2")

        url.matches("/test/test2") shouldBe true
    }

    test("matches a basic url without first slash") {
        val url = Url("/test/test2")

        url.matches("test/test2") shouldBe true
    }

    test("matches a basic url without first slash both ways") {
        val url = Url("/test/test2")

        url.matches("test/test2") shouldBe true
    }

    test("matches a simple wildcard url") {
        val url = Url("/test/test2/*")

        url.matches("/test/test2/hello") shouldBe true
    }

    test("matches multiple wildcards url") {
        val url = Url("/test/test2/*/*/exact")

        url.matches("/test/test2/hello/world/exact") shouldBe true
    }

    test("matches with wildcard end") {
        val url = Url("/test/test2/**")

        url.matches("/test/test2/hello/world/exact")  shouldBe true
    }

    test("doesn't match if url bigger") {
        val url = Url("/test/test2")

        url.matches("test/test2/test3") shouldBe false
    }

    test("doesn't match if bigger than url") {
        val url = Url("/test/test2/tes3")

        url.matches("test/test2") shouldBe false
    }

    test("parameters are extracted") {
        val url = Url("/test/test2/{parameter1}/{parameter2}")

        url.parameters("test/test2/value1/value2") shouldBe mapOf("parameter1" to "value1", "parameter2" to "value2")
    }

    test("parameters are extracted after wildcards") {
        val url = Url("/test/test2/*/{parameter2}")

        url.parameters("test/test2/value1/value2") shouldBe mapOf("parameter2" to "value2")
    }

    test("only specified parameters are extracted") {
        val url = Url("/test/test2/{parameter1}")

        url.parameters("test/test2/value1/value2") shouldBe mapOf("parameter1" to "value1")
    }

    test("Spaces before and after brackets are ignored") {
        val url = Url("/test/test2/ {parameter1} ")

        url.parameters("test/test2/value1/value2") shouldBe mapOf("parameter1" to "value1")
    }

})
