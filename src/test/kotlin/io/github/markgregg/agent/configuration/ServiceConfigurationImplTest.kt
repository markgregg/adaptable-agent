package io.github.markgregg.agent.configuration

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.github.markgregg.agent.utils.TypeDiscoveryMock.typeDiscoveryMock
import io.github.markgregg.agent.web.RestEndPoint

class ServiceConfigurationImplTest : FunSpec() {
	init {

		test("keyStoreType") {
			val result = io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceConfigLoader.load(xmlProps.byteInputStream()),
                typeDiscoveryMock()
            )
			result.keyStoreType shouldBe "PKCS12"
		}

		test("keyStorePath") {
			val result = io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceConfigLoader.load(xmlProps.byteInputStream()),
                typeDiscoveryMock()
            )
			result.keyStorePath shouldBe "test"
		}

		test("keySorePassword") {
			val result = io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceConfigLoader.load(xmlProps.byteInputStream()),
                typeDiscoveryMock()
            )
			result.keySorePassword shouldBe "test"
		}

		test("keyAlias") {
			val result = io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceConfigLoader.load(xmlProps.byteInputStream()),
                typeDiscoveryMock()
            )
			result.keyAlias shouldBe "test"
		}

		test("ports") {
			val result = io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceConfigLoader.load(xmlProps.byteInputStream()),
                typeDiscoveryMock()
            )
			result.ports shouldBe listOf(8089,8090)
		}

		test("securePorts") {
			val result = io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceConfigLoader.load(xmlProps.byteInputStream()),
                typeDiscoveryMock()
            )
			result.securePorts shouldBe listOf(9089,9090)
		}

		test("endPoints") {
			val typeDiscovery = typeDiscoveryMock()
			val result = io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                ServiceConfigLoader.load(xml.byteInputStream()),
                typeDiscovery
            )
			result.endPoints.size shouldBe 1
			result.endPoints["test"]?.rules?.size shouldBe 1
			result.endPoints["test"].shouldBeInstanceOf<RestEndPoint>()
		}

		test("load with bad endpoint") {
			val exception = shouldThrowExactly<EndPointDefinitionException> {
                io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                    ServiceConfigLoader.load(xmlBadEndPoint.byteInputStream()),
                    typeDiscoveryMock()
                )
			}

			exception.message shouldBe "No endpoint class has an annotation with BadType"
		}

		test("load with no matching constructor") {
			val exception = shouldThrowExactly<EndPointDefinitionException> {

                io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                    ServiceConfigLoader.load(
                        xmlNoMatchingConstructor.byteInputStream()
                    ), typeDiscoveryMock()
                )
			}

			exception.message shouldBe "NoMatchingConstructor class does not have a matching constructor (id, rules, properties, converter, timeout)"
		}

		test("load with duplicate endpoint") {
			val exception = shouldThrowExactly<DuplicateEndPointException> {
                io.github.markgregg.agent.configuration.ServiceConfigurationImpl(
                    ServiceConfigLoader.load(
                        xmlDuplicateEndPoint.byteInputStream()
                    ), typeDiscoveryMock()
                )
			}

			exception.message shouldBe "test endpoint already exists"
		}
	}

	companion object {
		const val xmlProps = """
			{
    "extensionDir" : "test",
    "keyStoreType" : "PKCS12",
    "keyStorePath" : "test",
    "keySorePassword" : "test",
    "keyAlias" : "test",
    "ports" : [ 8089, 8090 ],
    "securePorts" : [ 9089, 9090 ],
    "testCaseTimeout" : 1000,
    "endPoints" : []
}
			<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<serviceDefinition keyStoreType="PKCS12" keyStorePath="test" keySorePassword="test" keyAlias="test" ports="8089 8090" securePorts="9089 9090">
<endPoints/>
</serviceDefinition>"""

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
    	"type" : "Rest",
		"properties" : {
			"port": "9089",
			"url": "/api/secure",
			"method": "GET"
		},
		"rules" : [ {
			"@class" : "io.github.markgregg.common.api.StandardRule",
			"expression" : """ + "\"\$body.field=='test'\"," + """
			"response" : {
				"@class" : "io.github.markgregg.common.web.TextResponse",
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

		const val xmlBadEndPoint = """
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
    	"type" : "BadType",
		"properties" : {
			"port" : "9089",
			"url" : "/api/secure",
			"method" : "GET"
		},
		"messageConverter" : null,
		"unavailable" : null
	} ]
}			
"""

		const val xmlNoMatchingConstructor = """
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
    	"type" : "NoMatchingConstructor",
		"properties" : {
			"port" : "9089",
			"url" : "/api/secure",
			"method" : "GET"
		},
		"messageConverter" : null,
		"unavailable" : null
	} ]
}
"""

		const val xmlDuplicateEndPoint = """
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
    	"type" : "Rest",
		"properties" : {
			"port" : "9089",
			"url" : "/api/secure",
			"method" : "GET"
		},
		"messageConverter" : null,
		"unavailable" : null
	},
	 {
    	"id" : "test",
    	"type" : "Rest",
		"properties" : {
			"port" : "9089",
			"url" : "/api/secure",
			"method" : "GET"
		},
		"messageConverter" : null,
		"unavailable" : null
	}]
}			
"""

	}
}
