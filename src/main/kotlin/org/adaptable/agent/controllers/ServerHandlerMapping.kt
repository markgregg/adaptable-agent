package org.adaptable.agent.controllers

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.adaptable.agent.configuration.ServiceConfiguration
import org.adaptable.agent.web.RestService
import org.adaptable.common.protocol.EndPointRequest
import org.adaptable.common.web.WebRequest
import org.adaptable.common.web.WebResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.handler.AbstractHandlerMapping
import java.net.URL
import java.util.*
import javax.servlet.http.HttpServletRequest

class ServerHandlerMapping(
    private val serviceConfiguration: ServiceConfiguration
) : AbstractHandlerMapping()  {
    private val executionMethod = ServerHandlerMapping::class.java.getMethod("process", HttpServletRequest::class.java, Any::class.java)

    override fun getHandlerInternal(request: HttpServletRequest): Any? {
        val url = URL(request.requestURL.toString()).path
        logger.debug("Request received for (${url}-${request.serverPort}-${request.method})")
        return if( serviceConfiguration.endPoints
            .values
            .filterIsInstance<RestService>()
            .any {
                it.url.matches(url) &&
                        it.method.toString() == request.method &&
                        it.port == request.serverPort &&
                        !it.unavailable.get() }
            || serviceConfiguration.restEndPoints.containsKey(url))
                {
            HandlerMethod(this,executionMethod)
        } else {
            logger.debug("No endpoint defined for (${url}-${request.serverPort}-${request.method})")
            null
        }
    }

    fun process(
        request: HttpServletRequest,
        @RequestBody body: Any?
    ): ResponseEntity<Any?> {
        println("Received request for ${request.requestURL}")
        val url = URL(request.requestURL.toString()).path
        val endPoint = serviceConfiguration.endPoints
            .values
            .filterIsInstance<RestService>()
            .firstOrNull {
                it.url.matches(url) &&
                        it.method.toString() == request.method &&
                        it.port == request.serverPort &&
                        !it.unavailable.get()
            }
        if( endPoint != null ) {
            val internalRequest = if( body is Map<*, *>) {
                WebRequest(
                    UUID.randomUUID(),
                    getHeaders(request, url),
                    getQueryParams(endPoint.url.parameters(url), request),
                    try {
                        jacksonObjectMapper().valueToTree(body) as ObjectNode
                    } catch (e: Exception) {
                        jacksonObjectMapper().createObjectNode() as ObjectNode
                    }
                )
            } else {
                WebRequest(
                    UUID.randomUUID(),
                    getHeaders(request, url),
                    getQueryParams(endPoint.url.parameters(url), request),
                    try {
                        if( body is String ) {
                            endPoint.messageConverter?.convert(body) ?: body
                        } else {
                            endPoint.messageConverter?.convert(body) ?: body.toString()
                        }
                    } catch (e: Exception) {
                        ""
                    }
                )
            }
            logger.info("Request = $internalRequest")
            val response = endPoint.processRequest(internalRequest) as? WebResponse<*>
            if( response != null ) {
                val responseHeaders = HttpHeaders()
                response.headers?.forEach { responseHeaders.set(it.key, it.value) }
                return ResponseEntity.status(response.status!!)
                    .headers(responseHeaders)
                    .body(response.payload())
            }
            logger.debug("No response rules for (${url}-${request.serverPort}-${request.method})")
            return ResponseEntity("No response defined for request for ${request.serverPort} ${request.method} $url", HttpStatus.BAD_REQUEST)
        }
        if( serviceConfiguration.restEndPoints.containsKey(url) ) {
            try {
                val endPointMessage = jacksonObjectMapper().readValue(body as String, EndPointRequest::class.java)
                val endPont = serviceConfiguration.socketEndPoints[url]
                    ?.get(endPointMessage.endPointId)
                val response = endPont?.processRequest(endPointMessage.request)
                if( response != null ) {
                    return ResponseEntity.status(HttpStatus.OK)
                        .body(jacksonObjectMapper().writeValueAsString(response))
                }
            } catch (e: Exception) {
                logger.error("Unable to handle message for url: ${url}, reason: ${e.message}")
            }
        }
        logger.debug("No endpoint defined for (${url}-${request.serverPort}-${request.method})")
        return ResponseEntity("${request.serverPort} ${request.method} $url does not exist", HttpStatus.BAD_REQUEST)
    }

    private fun getQueryParams(urlParameters: Map<String, String>, request: HttpServletRequest): Map<String, String> {
        val parameters = HashMap(urlParameters)
        request.queryString
            ?.split("&")
            ?.map { it.split("=") }
            ?.filter { it.size > 1 }
            ?.forEach { parameters[it[0]] = it[1]}
        return parameters
    }

    private fun getHeaders(request: HttpServletRequest, url: String): Map<String, String> {
        val map = HashMap<String,String>()
        map["url"] = url
        val headerNames: Enumeration<String> = request.headerNames

        while (headerNames.hasMoreElements()) {
            val name = headerNames.nextElement()
            map[name] =  request.getHeader(name)
        }
        return map
    }
}
