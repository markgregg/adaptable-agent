package io.github.markgregg.agent.configuration

import io.github.markgregg.common.api.endpoints.EventEndPoint
import io.github.markgregg.common.api.interfaces.EndPoint
import io.github.markgregg.common.api.interfaces.EndPointConfiguration

interface ServiceConfiguration : EndPointConfiguration {
    val keyStoreType: String?
    val keyStorePath: String?
    val keySorePassword: String?
    val keyAlias: String?
    val ports: List<Int>?
    val securePorts: List<Int>?
    val restEndPoints: Map<String, Map<String,EndPoint>>
    val socketEndPoints: Map<String, Map<String, EventEndPoint>>
}