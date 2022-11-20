package org.adaptable.agent.configuration

import org.adaptable.common.api.annotations.Converter
import org.adaptable.common.api.interfaces.MessageConverter

@Converter("Mock")
class MessageConverterMock : MessageConverter {
    override fun convert(payload: Any?): String? {
        return null
    }
}