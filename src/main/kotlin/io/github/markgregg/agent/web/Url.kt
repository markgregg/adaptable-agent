package io.github.markgregg.agent.web

class Url(url: String) {
    private val urlElements: List<UrlElement>

    init {
        urlElements = url.split('/')
            .filter { it.trim().isNotEmpty() }
            .map {
            val trimmed = it.trim()
            if(trimmed == "*") {
                Wildcard()
            } else if(trimmed == "**" ) {
                WildcardEnd()
            } else if( trimmed.length > 2 &&
                trimmed[0] == '{' &&
                trimmed[trimmed.length-1] == '}') {
                Parameter(trimmed.substring(1, trimmed.length-1))
            } else {
                Matcher(it)//not trimmed for a reason
            }
        }
    }

    fun matches(url: String): Boolean {
        val elements = url.split('/')
            .filter { it.trim().isNotEmpty() }
        for( index in urlElements.indices) {
            if( index >= url.length) {
                return false
            }
            if( urlElements[index] is Matcher) {
                if( elements.size <= index ) {
                    return false
                }
                if( (urlElements[index] as Matcher).name != elements[index] ) {
                    return false
                }
            }
            if( urlElements[index] is WildcardEnd) {
                return true
            }
        }
        if( elements.size > urlElements.size ) {
            return false
        }
        return true
    }

    fun parameters(url: String): Map<String, String> {
        val parameters = HashMap<String, String>()
        val elements = url.split('/')
            .filter { it.trim().isNotEmpty() }
        for( index in urlElements.indices) {
            if( index < elements.size &&
                urlElements[index] is Parameter) {
                parameters[(urlElements[index] as Parameter).name] = elements[index]
            }
        }
        return parameters
    }
}