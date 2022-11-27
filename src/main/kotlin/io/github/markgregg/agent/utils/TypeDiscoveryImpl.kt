package io.github.markgregg.agent.utils

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.markgregg.agent.configuration.InvalidClassException
import io.github.markgregg.common.api.interfaces.MessageConverter
import java.io.File
import java.net.URLClassLoader

class TypeDiscoveryImpl : TypeDiscovery {
    private var classGraph: ClassGraph? = null

    override fun initialise(extensionDir: String?) {
        if( classGraph == null ) {
            classGraph = ClassGraph()
                .addClassLoader(loadExtensions(extensionDir))
                .enableClassInfo()
                .enableExternalClasses()
                .ignoreClassVisibility()
                .enableAnnotationInfo()
        }
    }

    override fun getEndPointTypes(): Map<String,Class<*>> {
        return discoverTypeMapByAnnotation<io.github.markgregg.common.api.annotations.EndPoint,String,Class<*>>(io.github.markgregg.common.api.annotations.EndPoint::class.java) {
            (it.getAnnotationInfo(io.github.markgregg.common.api.annotations.EndPoint::class.java).parameterValues.getValue("value") as String).lowercase() to it.loadClass()
        }
    }

    override fun getConverterTypes(): Map<String, MessageConverter> {
        return discoverTypeMapByAnnotation(io.github.markgregg.common.api.annotations.Converter::class.java) {
            val clazz = it.loadClass()
            val constructor = clazz.getDeclaredConstructor()
            if( constructor.parameterCount != 0) {
                throw InvalidClassException("Converter (${clazz.name}) does not have a parameterless constructor")
            }
            if( !MessageConverter::class.java.isAssignableFrom(clazz) ) {
                throw InvalidClassException("Converter (${clazz.name}) does not implement MessageConverter interface")
            }
            (it.getAnnotationInfo(io.github.markgregg.common.api.annotations.Converter::class.java).parameterValues.getValue("value") as String).lowercase() to
                    constructor.newInstance() as MessageConverter
        }
    }

    private fun <A : Annotation, T> discoverTypeListByAnnotation(annotation: Class<A>, transformer: (ClassInfo) -> T): List<T> {
        return classGraph?.scan().use { result ->
            result?.getClassesWithAnnotation(annotation)
                ?.map(transformer)
        } ?: emptyList()
    }

    private fun <A : Annotation, K, T> discoverTypeMapByAnnotation(annotation: Class<A>, associate: (ClassInfo) -> Pair<K, T>): Map<K, T> {
        return classGraph?.scan().use { result ->
            result?.getClassesWithAnnotation(annotation)
                ?.associate(associate)
        } ?: emptyMap()
    }

    private fun loadExtensions(extensionDir: String?): ClassLoader {
        return if( extensionDir == null ) {
            ClassLoader.getSystemClassLoader()
        } else {
            val urls = extensionDir.let { it ->
                File(it).listFiles()
                    ?.filter { it.extension.lowercase() =="jar" }
                    ?.map { it.toURI().toURL() }
                    ?.toTypedArray()
            }
            return URLClassLoader(urls,ClassLoader.getSystemClassLoader())
        }
    }
}