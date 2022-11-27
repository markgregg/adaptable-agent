package io.github.markgregg.agent.web

import io.github.markgregg.common.api.interfaces.EndPoint
import org.springframework.http.HttpMethod
import java.util.concurrent.atomic.AtomicBoolean

interface RestService : EndPoint {
    val url: Url
    val port: Int
    val method: HttpMethod
    val unavailable: AtomicBoolean
}