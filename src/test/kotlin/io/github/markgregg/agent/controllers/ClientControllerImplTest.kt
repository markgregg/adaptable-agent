package io.github.markgregg.agent.controllers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.github.markgregg.agent.configuration.ServiceConfiguration
import io.github.markgregg.common.api.endpoints.TestCaseNotActiveException
import io.github.markgregg.common.api.interfaces.EndPoint
import io.github.markgregg.common.protocol.EndpointResponse
import org.mockito.Mockito.mock
import org.mockito.kotlin.*
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.lang.Thread.sleep
import java.time.LocalDateTime

class ClientControllerImplTest : FunSpec() {
	
	init {

		test("when a message cannot be processed no action is taken") {
			val clientControllerImpl = ClientControllerImpl(mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java))
			val session = getSession()
			clientControllerImpl.processMessage(session, TextMessage("{}"))
		}

		test("when a message is not recognised no action is taken") {
			val clientControllerImpl = ClientControllerImpl(mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java))
			val session = getSession()
			clientControllerImpl.processMessage(session, TextMessage("{\"@class\": \"class\"}"))
		}

		test("failed to start response is sent to client if unable to handle start message") {
			val clientControllerImpl = ClientControllerImpl(mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java))
			val session = getSession()
			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"testx\":{\"id\":\"id\",\"endPoints\":[]}}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(session).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":false,\"message\":\"Failed to start test ({\\\"@class\\\":\\\"io.github.markgregg.common.protocol.StartTest\\\",\\\"testx\\\":{\\\"id\\\":\\\"id\\\",\\\"endPoints\\\":[]}}), reason: Instantiation of [simple type, class io.github.markgregg.common.protocol.StartTest] value failed for JSON property test due to missing (therefore NULL) value for creator parameter test which is a non-nullable type\\n at [Source: (String)\\\"{\\\"@class\\\":\\\"io.github.markgregg.common.protocol.StartTest\\\",\\\"testx\\\":{\\\"id\\\":\\\"id\\\",\\\"endPoints\\\":[]}}\\\"; line: 1, column: 93] (through reference chain: io.github.markgregg.common.protocol.StartTest[\\\"test\\\"])\"}"
		}

		test("failed to start response is sent when unable to start test") {
			val endPoint = mock(EndPoint::class.java)
			doAnswer {
				throw Exception("error")
			}.whenever(endPoint).startTest(isA())
			val config = getConfig(1, "id2" to endPoint)

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false},{\"id\":\"id2\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(config.endPoints["id"])!!.startTest(isA())
			verify(config.endPoints["id"])!!.endTest("id")
			verify(session).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":false,\"message\":\"Failed to start test id, reason: error\"}"

		}

		test("successful start response is sent when test has started successfully") {
			val config = getConfig()

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(config.endPoints["id"])!!.startTest(isA())
			verify(session).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":true,\"message\":null}"
		}

		test("Attempting to start when session closed does nothing") {
			val config = getConfig()

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = mock(WebSocketSession::class.java)
			whenever(session.isOpen).thenReturn(false)

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			verify(config.endPoints["id"], times(0))!!.startTest(isA())
			verify(session, times(0)).sendMessage(isA())
		}

		test("test is terminated when timeout passes") {
			val config = getConfig(1)

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			sleep(2000)
			val captor = argumentCaptor<TextMessage>()
			verify(config.endPoints["id"])!!.startTest(isA())
			verify(config.endPoints["id"])!!.endTest("id")
			verify(session).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":true,\"message\":null}"
		}

		test("timeout does nothing if test has already been terminated") {
			val config = getConfig(1000)

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.EndTest\",\"id\":\"id\"}")
			)
			sleep(2000)
			val captor = argumentCaptor<TextMessage>()
			verify(config.endPoints["id"])!!.startTest(isA())
			verify(config.endPoints["id"], times(1))!!.endTest("id")
			verify(session, times(2)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":true,\"message\":null}"
		}

		test("When test end endpoints are notified and a response is sent back to the client") {
			val config = getConfig()

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.EndTest\",\"id\":\"id\"}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(config.endPoints["id"])!!.startTest(isA())
			verify(config.endPoints["id"], times(1))!!.endTest("id")
			verify(session, times(2)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":true,\"message\":null}"
			captor.secondValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.EndTestResponse\",\"success\":true,\"message\":null}"
		}

		test("When test fails a response is sent back to the client") {
			val config = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
			val clientControllerImpl = ClientControllerImpl(config)
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.EndTest\",\"idx\":\"id\"}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(session, times(1)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.EndTestResponse\",\"success\":false,\"message\":\"Failed to end test ({\\\"@class\\\":\\\"io.github.markgregg.common.protocol.EndTest\\\",\\\"idx\\\":\\\"id\\\"}), reason: Instantiation of [simple type, class io.github.markgregg.common.protocol.EndTest] value failed for JSON property id due to missing (therefore NULL) value for creator parameter id which is a non-nullable type\\n at [Source: (String)\\\"{\\\"@class\\\":\\\"io.github.markgregg.common.protocol.EndTest\\\",\\\"idx\\\":\\\"id\\\"}\\\"; line: 1, column: 67] (through reference chain: io.github.markgregg.common.protocol.EndTest[\\\"id\\\"])\"}"
		}

		test("multiple requests to start tests are queued") {
			val config = getConfig()

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id2\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id3\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)

			val captor = argumentCaptor<TextMessage>()
			verify(config.endPoints["id"], times(1))!!.startTest(isA())
			verify(session, times(1)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":true,\"message\":null}"
		}

		test("when tests end new tests are started") {
			val config = getConfig()

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id2\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id3\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)

			val captor = argumentCaptor<TextMessage>()
			verify(config.endPoints["id"], times(1))!!.startTest(isA())
			verify(session, times(1)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":true,\"message\":null}"

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.EndTest\",\"id\":\"id\"}")
			)
			verify(config.endPoints["id"], times(1))!!.endTest(isA())
			verify(config.endPoints["id"], times(2))!!.startTest(isA())
			verify(session, times(3)).sendMessage(captor.capture())
			captor.thirdValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.StartTestResponse\",\"success\":true,\"message\":null}"
		}

		test("when making an endpoint unavailable a response is sent to the client") {
			val config = getConfig()

			val clientControllerImpl = ClientControllerImpl(config)
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.MakeUnavailable\",\"endPoint\":\"id\"}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(session, times(1)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.MakeUnavailableResponse\",\"success\":true,\"message\":null}"
		}

		test("Attempting to make an endpoint unavailable throws an exception") {
			val config = getConfig()
			whenever(config.endPoints["id"]!!.unavailable("id")).thenThrow(TestCaseNotActiveException())

			val clientControllerImpl = ClientControllerImpl(config)
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.MakeUnavailable\",\"endPoint\":\"id\"}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(session, times(1)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.MakeUnavailableResponse\",\"success\":false,\"message\":\"Failed to make unavailable ({\\\"@class\\\":\\\"io.github.markgregg.common.protocol.MakeUnavailable\\\",\\\"endPoint\\\":\\\"id\\\"}), reason: null\"}"
		}

		test("when making an endpoint available a response is sent to the client") {
			val config = getConfig()

			val clientControllerImpl = ClientControllerImpl(config)
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.MakeAvailable\",\"endPoint\":\"id\"}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(session, times(1)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.MakeAvailableResponse\",\"success\":true,\"message\":null}"
		}

		test("Attempting to make an endpoint available throws an exception") {
			val config = getConfig()
			whenever(config.endPoints["id"]!!.available("id")).thenThrow(TestCaseNotActiveException())

			val clientControllerImpl = ClientControllerImpl(config)
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.MakeAvailable\",\"endPoint\":\"id\"}")
			)
			val captor = argumentCaptor<TextMessage>()
			verify(session, times(1)).sendMessage(captor.capture())
			captor.firstValue.payload shouldBe "{\"@class\":\"io.github.markgregg.common.protocol.MakeAvailableResponse\",\"success\":false,\"message\":\"Failed to make available ({\\\"@class\\\":\\\"io.github.markgregg.common.protocol.MakeAvailable\\\",\\\"endPoint\\\":\\\"id\\\"}), reason: null\"}"
		}

		test("when session is closed test is ended") {
			val config = getConfig()

			val behaviourSubject = BehaviorSubject.create<LocalDateTime>()
			val responseSubject = BehaviorSubject.create<EndpointResponse>()
			val clientControllerImpl = ClientControllerImpl(config, { behaviourSubject }, { responseSubject })
			val session = getSession()

			clientControllerImpl.processMessage(
				session,
				TextMessage("{\"@class\":\"io.github.markgregg.common.protocol.StartTest\",\"test\":{\"id\":\"id\",\"endPoints\":[{\"id\":\"id\",\"type\":null,\"properties\":null,\"rules\":[],\"messageConverter\":null,\"unavailable\":false}]}}")
			)
			verify(config.endPoints["id"])!!.startTest(isA())
			verify(session).sendMessage(isA())
			clientControllerImpl.notifySessionClose(session)
			verify(config.endPoints["id"])!!.endTest(isA())
		}

		test("when session is closed and no test is running, nothing happens") {
			val config = getConfig()

			val clientControllerImpl = ClientControllerImpl(config)
			val session = getSession()
			clientControllerImpl.notifySessionClose(session)
		}
	}
	
	private fun getSession(): WebSocketSession {
		val session = mock(WebSocketSession::class.java)
		whenever(session.isOpen).thenReturn(true)
		return session
	}

	private fun getConfig(timeout: Long? = null, endpoint: Pair<String, EndPoint>? = null): io.github.markgregg.agent.configuration.ServiceConfiguration {
		val config = mock(io.github.markgregg.agent.configuration.ServiceConfiguration::class.java)
		if( timeout != null) {
			whenever(config.testCaseTimeout).thenReturn(timeout)
		}
		val endPoints = HashMap<String, EndPoint>()
		val endPointValidate = mock(EndPoint::class.java)
		endPoints["id"] = endPointValidate
		if( endpoint != null ){
			endPoints[endpoint.first] = endpoint.second
		}
		whenever(config.endPoints).thenReturn(endPoints)

		return config
	}
}
