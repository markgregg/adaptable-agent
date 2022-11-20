package org.adaptable.agent.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import org.adaptable.agent.configuration.ServiceConfiguration
import org.adaptable.common.api.Request
import org.adaptable.common.api.Response
import org.adaptable.common.api.Test
import org.adaptable.common.api.endpoints.EventEndPoint
import org.adaptable.common.api.interfaces.ActiveTestCase
import org.adaptable.common.api.interfaces.EndPoint
import org.adaptable.common.protocol.*
import org.slf4j.LoggerFactory
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.timerTask

class ClientControllerImpl(
    private val serviceConfiguration: ServiceConfiguration,
    subjectCreator: () -> Subject<LocalDateTime>,
    endPointSubjectCreator: () -> Subject<EndpointResponse>
) : ClientController {
    constructor(serviceConfiguration: ServiceConfiguration) : this( serviceConfiguration, { PublishSubject.create() }, { PublishSubject.create() })
    companion object {
        private val logger = LoggerFactory.getLogger(ClientControllerImpl::class.java)
    }

    private val activeTest = AtomicReference<Test?>()
    private val activeTestSession = AtomicReference<WebSocketSession?>()
    private val testQueue = ConcurrentLinkedQueue<TestSession>()
    private val timer = AtomicReference<Timer?>()
    private val startTestSequence = subjectCreator.invoke()
    private val responseSequence = endPointSubjectCreator.invoke()

    init {
        startTestSequence.map { testQueue.remove() }
            .filter { true }
            .subscribe { startTest(it.session, it.test)}
    }

    override fun processMessage(session: WebSocketSession, message: TextMessage) {
        try {
            when {
                message.payload.contains("org.adaptable.common.protocol.StartTest") -> addStartTestMessage(session, message.payload)
                message.payload.contains("org.adaptable.common.protocol.EndTest") -> processEndTestMessage(session, message.payload)
                message.payload.contains("org.adaptable.common.protocol.MakeAvailable") -> processMakeAvailableMessage(session, message.payload)
                message.payload.contains("org.adaptable.common.protocol.MakeUnavailable") -> processMakeUnavailableMessage(session, message.payload)
                message.payload.contains("org.adaptable.common.protocol.Message") -> processResponseMessage(session, message.payload)
                message.payload.contains("org.adaptable.common.protocol.EndpointResponse") -> processEndpointResponse(message.payload)
                else -> logger.warn("Unknown message (${message.payload})")
            }
        } catch(e: Exception) {
            val error = "Failed to process message (${message.payload})"
            logger.warn(error)
            sendResponse(session, ErrorResponse(error, message.payload))
        }
    }

    override fun notifySessionClose(session: WebSocketSession) {
        logger.info("Socket has closed")
        if( activeTestSession.get() == session && activeTest.get() != null) {
            logger.info("Ending test and clearing session")
            endTest(activeTest.get()!!.id)
            clearActiveTest()
        }
    }

    private fun addStartTestMessage(session: WebSocketSession, payload: String) {
        try {
            val startTest = jacksonObjectMapper().readValue(
                payload,
                StartTest::class.java
            )
            logger.info("Adding test id: ${startTest.test.id} to queue.")
            testQueue.add(
                TestSession( startTest.test, session)
            )
            nextSequence()
        } catch (e: Exception) {
            logger.warn("Failed to process start message ($payload).")
            sendResponse(session, StartTestResponse(false,"Failed to start test ($payload), reason: ${e.message}") )
        }
    }

    private fun startTest(session: WebSocketSession, test: Test) {
        val endPointsTest = HashMap<String, EndPoint>()
        try {
            if( !session.isOpen ) {
                logger.info("Test id: ${test.id} session has closed.")
                return //do nothing as the session has terminated
            }
            logger.info("Starting test ${test.id}.")
            for (endPointDef in test.endPoints) {
                val endPoint = serviceConfiguration.endPoints[endPointDef.id] ?: throw EndPointNotFoundException()
                logger.info("Adding rules for ${test.id} to end point id ${endPointDef.id}.")
                endPoint.startTest(
                    ActiveTestCase(test.id, endPointDef.rules ) { request, requiresResponse ->
                        sendRequestToClient(test.id, endPointDef.id, endPointDef.sendsResponse == true && requiresResponse, request)
                    }
                )
                endPointsTest[endPointDef.id] = endPoint
            }
            activeTest.set(test)
            activeTestSession.set(session)
            logger.info("Sending start response for test ${test.id}")
            sendResponse(session, StartTestResponse(true,null))

            if ((serviceConfiguration.testCaseTimeout ?: 0) > 0) {
                if (timer.get() != null) {
                    timer.get()!!.cancel()
                    timer.get()!!.purge()
                }
                timer.set(Timer("test", false))
                timer.get()!!.schedule(timerTask {
                    logger.info("Test ${test.id} has timed out terminating.")
                    if (activeTest.get() != null && activeTest.get()?.id == test.id) {
                        endTest(activeTest.get()!!.id)
                    }
                }, serviceConfiguration.testCaseTimeout!!)
            }
        } catch (e: Exception) {
            logger.error("Failed to start test")
            endPointsTest.values.forEach {
                it.endTest(test.id)
            }
            sendResponse(session, StartTestResponse(false,"Failed to start test ${test.id}, reason: ${e.message}") )
        }
    }

    private fun processEndTestMessage(session: WebSocketSession, payload: String) {
        try {
            val id = jacksonObjectMapper().readValue(
                payload,
                EndTest::class.java
            ).id
            if( activeTest.get()?.id == id ) {
                logger.info("Ending test $id.")
                endTest(id)
                logger.info("Sending end response for test $id")
                sendResponse(session, EndTestResponse(true, null))
            } else {
                val test = testQueue.firstOrNull { it.test.id == id }
                if( test != null ) {
                    testQueue.remove(test)
                }
                nextSequence()
            }
        } catch (e: Exception) {
            logger.info("Failed to end test, reason: ${e.message}")
            sendResponse(session, EndTestResponse(false,"Failed to end test ($payload), reason: ${e.message}") )
        }
    }

    private fun endTest(id: String) {
        if( activeTest.get()?.id == id) {
            for (endPointDef in activeTest.get()!!.endPoints) {
                logger.info("clearing end point ${endPointDef.id} of rules.")
                val endPoint = serviceConfiguration.endPoints[endPointDef.id] ?: throw EndPointNotFoundException()
                endPoint.endTest(id)
            }
            clearActiveTest()
        }
        nextSequence()
    }

    private fun processMakeUnavailableMessage(session: WebSocketSession, payload: String) {
        try {
            val id = jacksonObjectMapper().readValue(
                payload,
                MakeUnavailable::class.java
            ).endPoint
            logger.info("Make end point $id unavailable.")
            changeEndPointAvailability(id, true)
            logger.info("Sending unavailable response for test $id")
            sendResponse(session, MakeUnavailableResponse(true, null) )
        } catch (e: Exception) {
            logger.info("Failed to process make unavailable for ($payload)")
            sendResponse(session, MakeUnavailableResponse(false,"Failed to make unavailable ($payload), reason: ${e.message}") )
        }
    }

    private fun processMakeAvailableMessage(session: WebSocketSession, payload: String) {
        try {
            val id = jacksonObjectMapper().readValue(
                payload,
                MakeAvailable::class.java
            ).endPoint
            logger.info("Make end point $id available.")
            changeEndPointAvailability(id, false)
            logger.info("Sending available response for test $id")
            sendResponse(session, MakeAvailableResponse(true, null) )
        } catch (e: Exception) {
            logger.info("Failed to process make available for ($payload)")
            sendResponse(session, MakeAvailableResponse(false,"Failed to make available ($payload), reason: ${e.message}") )
        }
    }

    private fun changeEndPointAvailability(id: String, unavailable: Boolean) {
        val endPoint = serviceConfiguration.endPoints[id] ?: throw EndPointNotFoundException()
        if( unavailable ) {
            endPoint.unavailable(id)
        } else {
            endPoint.available(id)
        }
    }

    private fun sendRequestToClient(testCaseId: String, endPointId: String, waitForResponse: Boolean, request: Request): Response? {
        logger.info("Sending request to client: $request")
        if( testCaseId != activeTest.get()?.id ) {
            return null
        }
        if( waitForResponse ) {
            return sendRequestToClientAndWaitForResponse(endPointId, request)
        }
        activeTestSession.get()?.sendMessage(TextMessage(jacksonObjectMapper()
            .writeValueAsString(RequestResponse(endPointId, request))))
        return null
    }

    private fun sendRequestToClientAndWaitForResponse(endPointId: String, request: Request): Response? {
        val response = AtomicReference<EndpointResponse?>()
        val countDown = CountDownLatch(1)
        val disposable = responseSequence
            .subscribe {
                if(  it is EndpointResponse) {
                    response.set(it)
                    countDown.countDown()
                }
            }
        try {
            activeTestSession.get()?.sendMessage(TextMessage(jacksonObjectMapper()
                .writeValueAsString(RequestResponse(endPointId, request))))
            logger.info("Waiting for response")
            countDown.await(10000, TimeUnit.MILLISECONDS)
        } finally {
            if( !disposable.isDisposed ) {
                disposable.dispose()
            }
        }
       return response.get()?.response
    }

    private fun processResponseMessage(session: WebSocketSession, payload: String) {
        try {
            val message = jacksonObjectMapper().readValue(
                payload,
                Message::class.java
            )
            logger.info("Sending message ${message.response} to ${message.endPoint}")
            val endPoint = serviceConfiguration.endPoints[message.endPoint] as? EventEndPoint
            if( endPoint == null ) {
                logger.info("Endpoint ${message.endPoint} does not exist or is not an event type")
                sendResponse(session, MessageResponse(false, "Endpoint ${message.endPoint} does not exist or is not an event type") )
                return
            }

            endPoint.sendMessage(message.response)
            sendResponse(session, MessageResponse(true, null) )
        } catch (e: Exception) {
            logger.info("Failed to process send message for ($payload), reason: ${e.message}")
            sendResponse(session, MessageResponse(false, "Failed to process send message for ($payload), reason: ${e.message}") )
        }
    }

    private fun processEndpointResponse(payload: String) {
        try {
            val response = jacksonObjectMapper().readValue(
                payload,
                EndpointResponse::class.java
            )
            responseSequence.onNext(response)
        } catch (e: Exception) {
            logger.info("Failed to forward response($payload) to endpoint, reason: ${e.message}")
        }
    }

    private fun <T>sendResponse(session: WebSocketSession, response: T) {
        session.sendMessage( TextMessage(jacksonObjectMapper().writeValueAsString(response)))
    }

    @Synchronized
    private fun nextSequence() {
        if( activeTest.get() == null && testQueue.isNotEmpty() ) {
            startTestSequence.onNext(LocalDateTime.now())
        }
    }

    private fun clearActiveTest() {
        logger.info("Clearing active session")
        activeTest.set(null)
        activeTestSession.set(null)
    }

    private class TestSession(
        val test: Test,
        val session: WebSocketSession
    )
}