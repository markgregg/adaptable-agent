package io.github.markgregg.agent.configuration

import io.github.markgregg.agent.utils.TypeDiscovery
import io.github.markgregg.common.api.Rule
import io.github.markgregg.common.api.ServiceDefinition
import io.github.markgregg.common.api.annotations.EndPointType
import io.github.markgregg.common.api.endpoints.EventEndPoint
import io.github.markgregg.common.api.interfaces.EndPoint
import io.github.markgregg.common.api.interfaces.MessageConverter
import org.slf4j.LoggerFactory


class ServiceConfigurationImpl(
    serviceDefinition: ServiceDefinition,
    typeDiscovery: TypeDiscovery,
) : io.github.markgregg.agent.configuration.ServiceConfiguration {
    companion object {
        private val logger = LoggerFactory.getLogger(io.github.markgregg.agent.configuration.ServiceConfigurationImpl::class.java)
    }
    override val keyStoreType: String? = serviceDefinition.keyStoreType
    override val keyStorePath: String? =  serviceDefinition.keyStorePath
    override val keySorePassword: String? = serviceDefinition.keySorePassword
    override val keyAlias: String? = serviceDefinition.keyAlias
    override val ports: List<Int>? = serviceDefinition.ports
    override val securePorts: List<Int>? = serviceDefinition.securePorts
    override val restEndPoints: Map<String, Map<String,EndPoint>>
    override val socketEndPoints: Map<String, Map<String, EventEndPoint>>
    override val testCaseTimeout: Long? = serviceDefinition.testCaseTimeout
    override val endPoints = HashMap<String, EndPoint>()

    init {
        typeDiscovery.initialise(serviceDefinition.extensionDir)
        val restEndPoints = HashMap<String, MutableMap<String, EndPoint>>()
        val socketEndPoints = HashMap<String, MutableMap<String, EventEndPoint>>()
        val types = typeDiscovery.getEndPointTypes()
        val converters = typeDiscovery.getConverterTypes()
        for (endPointDef in serviceDefinition.endPoints!!) {
            val type = try {
                types[endPointDef.type!!.lowercase()] ?: throw ClassNotFoundException(endPointDef.type)
            } catch (e1: ClassNotFoundException) {
                throw EndPointDefinitionException("No endpoint class has an annotation with ${endPointDef.type}", e1)
            }

            val constructor = try {
                type.getDeclaredConstructor(String::class.java, List::class.java, Map::class.java, MessageConverter::class.java)
            } catch(e2: Exception ) {
                throw EndPointDefinitionException(
                    "${endPointDef.type} class does not have a matching constructor (id, rules, properties, converter, timeout)",
                    e2
                )
            }

            val messageConverter = endPointDef.messageConverter?.let {
                converters[it.lowercase()] ?:  throw ClassNotFoundException("Message converter class (${endPointDef.messageConverter}) not found")
            }

            io.github.markgregg.agent.configuration.ServiceConfigurationImpl.Companion.logger.info("Constructing endpoint id: ${endPointDef.id} of type: ${endPointDef.type}")
            val endPoint = constructor.newInstance(
                endPointDef.id,
                endPointDef.rules ?: ArrayList<Rule>(),
                endPointDef.properties,
                messageConverter
            ) as EndPoint

            endPoint.initialise()
            if( endPoints.containsKey(endPoint.id)) {
                throw DuplicateEndPointException("${endPoint.id} endpoint already exists")
            }
            endPoints[endPoint.id] = endPoint

            val endPointMeta = type.getAnnotation(io.github.markgregg.common.api.annotations.EndPoint::class.java)
            if( endPointMeta.endPointType == EndPointType.UseRest) {
                restEndPoints.getOrPut(endPointMeta.url) { HashMap() }[endPoint.id] = endPoint
            }
            if( endPointMeta.endPointType == EndPointType.UseSocket) {
                val eventEndPoint = endPoint as? EventEndPoint
                    ?: throw InvalidClassException("End point (${endPoint.id} does not implement the EventEndPoint interface")
                socketEndPoints.getOrPut(endPointMeta.url) { HashMap() }[endPoint.id] = eventEndPoint
            }
        }
        this.restEndPoints = restEndPoints
        this.socketEndPoints = socketEndPoints
    }

    override fun <T : EndPoint> getEndPoint(id: String, type: Class<T>): T? =
        endPoints.values.filterIsInstance(type).firstOrNull { it.id == id }

    override fun <T : EndPoint> getEndPoints(type: Class<T>): List<T> =
        endPoints.values.filterIsInstance(type)

}