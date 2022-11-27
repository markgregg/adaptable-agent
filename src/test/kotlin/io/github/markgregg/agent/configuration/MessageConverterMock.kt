package io.github.markgregg.agent.configuration

import io.github.markgregg.common.api.annotations.Converter
import io.github.markgregg.common.api.interfaces.MessageConverter

@Converter("Mock")
class MessageConverterMock : MessageConverter {
    override fun convert(payload: Any?): String? {
        return null
    }
}