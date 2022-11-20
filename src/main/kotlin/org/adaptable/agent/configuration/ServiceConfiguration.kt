package org.adaptable.agent.configuration

import org.adaptable.common.api.endpoints.EventEndPoint
import org.adaptable.common.api.interfaces.EndPoint
import org.adaptable.common.api.interfaces.EndPointConfiguration

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