package io.github.markgregg.agent.utils

import io.github.markgregg.agent.configuration.MessageConverterMock
import io.github.markgregg.agent.configuration.NoMatchingConstructor
import io.github.markgregg.agent.web.RestEndPoint
import io.github.markgregg.agent.web.SocketEndPoint
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

object TypeDiscoveryMock {
    fun typeDiscoveryMock(): TypeDiscovery {
        val typeDiscovery = mock(TypeDiscovery::class.java)

        whenever(typeDiscovery.getEndPointTypes()).thenReturn(
            mapOf("rest" to RestEndPoint::class.java,
                "socket" to SocketEndPoint::class.java,
                "nomatchingconstructor" to NoMatchingConstructor::class.java
            )
        )
        whenever(typeDiscovery.getConverterTypes()).thenReturn(
            mapOf("mock" to MessageConverterMock())
        )
        return typeDiscovery
    }
}