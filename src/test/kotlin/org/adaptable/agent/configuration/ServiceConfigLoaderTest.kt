package org.adaptable.agent.configuration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ServiceConfigLoaderTest : FunSpec({

    test("load") {
        val result = ServiceConfigLoader.load(xml.byteInputStream())
        result.keyStoreType shouldBe "PKCS12"
        result.keyStorePath shouldBe "test"
        result.keySorePassword shouldBe "test"
        result.keyAlias shouldBe "test"
        result.ports?.size shouldBe 2
        result.securePorts?.size shouldBe 2
        result.endPoints?.size shouldBe 1
        result.endPoints?.get(0)?.rules?.size shouldBe 1
    }


}) {
    companion object {
        const val xml = """
{
    "extensionDir" : "test",
    "keyStoreType" : "PKCS12",
    "keyStorePath" : "test",
    "keySorePassword" : "test",
    "keyAlias" : "test",
    "ports" : [ 8089, 8090 ],
    "securePorts" : [ 9089, 9090 ],
    "testCaseTimeout" : 1000,
    "endPoints" : [ {
    "id" : "test",
    "type" : "test",
    "properties" : {
    "test" : "test"
    } ,
    "rules" : [ {
    "@class" : "org.adaptable.common.api.StandardRule",
    "expression":""" + "\"\$body.field=='test'\"," + """
    "response" : {
    "@class" : "org.adaptable.common.web.TextResponse",
    "status" : 200,
    "body" : "Hello world",
    "headers" : null,
    "isFile" : null
},
    "responses" : null
} ],
    "messageConverter" : null,
    "unavailable" : null
} ]
}
"""
    }
}
