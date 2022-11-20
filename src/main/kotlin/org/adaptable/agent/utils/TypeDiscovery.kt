package org.adaptable.agent.utils

import org.adaptable.common.api.interfaces.MessageConverter

interface TypeDiscovery {
    fun initialise(extensionDir: String?)
    fun getEndPointTypes(): Map<String,Class<*>>
    fun getConverterTypes(): Map<String, MessageConverter>
}
