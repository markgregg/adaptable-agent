package io.github.markgregg.agent.web

import io.github.markgregg.common.api.endpoints.EventEndPoint
import java.util.concurrent.atomic.AtomicBoolean

interface SocketService : EventEndPoint {
    val port: Int
    val url: Url
    val unavailable: AtomicBoolean
}