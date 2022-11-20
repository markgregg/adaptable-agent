package org.adaptable.agent.utils

import org.adaptable.agent.configuration.MessageConverterMock
import org.adaptable.agent.configuration.NoMatchingConstructor
import org.adaptable.agent.web.RestEndPoint
import org.adaptable.agent.web.SocketEndPoint
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