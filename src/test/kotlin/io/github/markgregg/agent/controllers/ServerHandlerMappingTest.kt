package io.github.markgregg.agent.controllers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.github.markgregg.agent.configuration.ServiceConfiguration
import io.github.markgregg.agent.configuration.ServiceConfigurationImpl
import io.github.markgregg.agent.utils.TypeDiscoveryMock.typeDiscoveryMock
import io.github.markgregg.agent.web.RestService
import io.github.markgregg.agent.web.Url
import io.github.markgregg.common.api.EndPoint
import io.github.markgregg.common.api.ServiceDefinition
import io.github.markgregg.common.api.interfaces.MessageConverter
import io.github.markgregg.common.web.TextResponse
import io.github.markgregg.common.web.WebRequest
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.isA
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.method.HandlerMethod
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.http.HttpServletRequest

class ServerHandlerMappingTest : FunSpec() {

    init {
        test("returns null when endpoint not found") {
            val request = getRequest(8097)
            callHandler(request).shouldBeNull()
        }

        test("returns null when endpoint is unavailable") {
            val request = getRequest(8098)
            val endPoint = getEndPoint(unavailable = true)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            callHandler(request, serviceConfiguration).shouldBeNull()
        }


        test("returns process method when end point not found") {
            val request = getRequest(8098)
            callHandler(request).shouldBeInstanceOf<HandlerMethod>()
        }

        test("process null endpoint") {
            val request = getRequest(1111)
            whenever(request.headerNames).thenReturn(Collections.enumeration(Collections.emptyList()))
            val response = callProcess(getServiceConfiguration(), request, "") as ResponseEntity<*>
            response.body shouldBe "1111 GET /api/secure does not exist"
        }

        test("process returns null when endpoint unavailable") {
            val request = getRequest(8098)
            whenever(request.headerNames).thenReturn(Collections.enumeration(Collections.emptyList()))

            val endPoint = getEndPoint(unavailable = true)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))

            val response = callProcess(serviceConfiguration, request, "") as ResponseEntity<*>
            response.body shouldBe "8098 GET /api/secure does not exist"
        }

        test("process returns null if no rules match") {
            val request = getRequest(8098)
            whenever(request.headerNames).thenReturn(Collections.enumeration(Collections.emptyList()))

            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(null)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))
            val response = callProcess(serviceConfiguration, request, "") as ResponseEntity<*>
            response.body shouldBe "No response defined for request for 8098 GET /api/secure"

        }

        test("process returns response if rules match") {
            val request = getRequest(8098)
            whenever(request.headerNames).thenReturn(Collections.enumeration(Collections.emptyList()))

            val returnResponse = TextResponse(
                HttpStatus.OK.value(),
                "Test message",
                mapOf("name" to "value"),
                null
            )
            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))
            val response = callProcess(serviceConfiguration, request, "") as ResponseEntity<*>
            response.body shouldBe "Test message"
            response.statusCode shouldBe HttpStatus.OK
            response.headers.size shouldBe 1
            response.headers["name"] shouldBe listOf("value")
        }

        test("process extracts url parameters") {
            val request = getRequest(8098, "http://localhost:8080/api/secure/value1/value2")
            whenever(request.headerNames).thenReturn(Collections.enumeration(Collections.emptyList()))

            val returnResponse = TextResponse(
                HttpStatus.OK.value(),
                "Test message",
                mapOf("name" to "value")
            )
            val endPoint = getEndPoint(url = "/api/secure/{param1}/{param2}")
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))
            val response = callProcess(serviceConfiguration, request, "") as ResponseEntity<*>
            response.body shouldBe "Test message"
            response.statusCode shouldBe HttpStatus.OK
            response.headers.size shouldBe 1
            response.headers["name"] shouldBe listOf("value")

            val captor = argumentCaptor<WebRequest>()
            verify(endPoint).processRequest(captor.capture())
            captor.firstValue.parameters shouldBe mapOf("param1" to "value1", "param2" to "value2")
        }

        test("converts message if a message converter is supplied") {
            val request = getRequest(8098)
            whenever(request.headerNames).thenReturn(Collections.enumeration(Collections.emptyList()))

            val returnResponse = TextResponse(
                HttpStatus.OK.value(),
                "Test message",
                mapOf("name" to "value"),
                null
            )
            val messageConverter = mock(MessageConverter::class.java)
            val endPoint = getEndPoint()
            whenever(endPoint.processRequest(isA())).thenReturn(returnResponse)
            whenever(endPoint.messageConverter).thenReturn(messageConverter)

            val serviceConfiguration = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
            whenever(serviceConfiguration.endPoints).thenReturn(mapOf("id" to endPoint))
            val response = callProcess(serviceConfiguration, request, "") as ResponseEntity<*>
            response.body shouldBe "Test message"
            response.statusCode shouldBe HttpStatus.OK
            response.headers.size shouldBe 1
            response.headers["name"] shouldBe listOf("value")

            verify(messageConverter).convert(isA())
        }
    }

    companion object {
        private fun getServiceConfiguration(): io.github.markgregg.agent.configuration.ServiceConfiguration {
            return io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceDefinition(
                    null, null, null, null, null, listOf(8099), null, null,
                    listOf(
                        EndPoint(
                            "id",
                            "Rest",
                            mapOf(
                                "port" to "8098",
                                "url" to "/api/secure",
                                "method" to "GET"
                            ), null, null, false
                        )
                    ),
                ),
                typeDiscoveryMock()
            )
        }
        private fun callHandler(request: HttpServletRequest, serviceConfiguration: io.github.markgregg.agent.configuration.ServiceConfiguration = getServiceConfiguration()) : Any? {
            val method = ServerHandlerMapping::class.java.getDeclaredMethod("getHandlerInternal", HttpServletRequest::class.java)
            return method.invoke(ServerHandlerMapping(serviceConfiguration), request)
        }

        private fun callProcess(serviceConfiguration: io.github.markgregg.agent.configuration.ServiceConfiguration, request: HttpServletRequest, body: Any?) : Any? {
            val method = ServerHandlerMapping::class.java.getDeclaredMethod("process", HttpServletRequest::class.java, Any::class.java)
            return method.invoke(ServerHandlerMapping(serviceConfiguration), request, body)
        }

        private fun getRequest(port: Int, url: String = "http://localhost:8080/api/secure"): HttpServletRequest {
            val request = mock(HttpServletRequest::class.java)
            whenever(request.serverPort).thenReturn(port)
            whenever(request.requestURL).thenReturn(StringBuffer(url))
            whenever(request.method).thenReturn("GET")
            return request
        }

        private fun getEndPoint(port: Int = 8098, url: String = "/api/secure", unavailable: Boolean = false): RestService {
            val endPoint = mock(RestService::class.java)
            whenever(endPoint.url).thenReturn(Url(url))
            whenever(endPoint.port).thenReturn(port)
            whenever(endPoint.method).thenReturn(HttpMethod.GET)
            whenever(endPoint.unavailable).thenReturn(AtomicBoolean(unavailable))
            return endPoint
        }
    }

}
