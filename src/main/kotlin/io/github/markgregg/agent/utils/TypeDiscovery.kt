package io.github.markgregg.agent.utils

import io.github.markgregg.common.api.interfaces.MessageConverter

interface TypeDiscovery {
    fun initialise(extensionDir: String?)
    fun getEndPointTypes(): Map<String,Class<*>>
    fun getConverterTypes(): Map<String, MessageConverter>
}
