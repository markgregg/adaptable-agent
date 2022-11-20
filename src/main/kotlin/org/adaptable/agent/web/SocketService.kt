package org.adaptable.agent.web

import org.adaptable.common.api.endpoints.EventEndPoint
import java.util.concurrent.atomic.AtomicBoolean

interface SocketService : EventEndPoint {
    val port: Int
    val url: Url
    val unavailable: AtomicBoolean
}