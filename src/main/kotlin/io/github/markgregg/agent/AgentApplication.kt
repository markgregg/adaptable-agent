package io.github.markgregg.agent

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AgentApplication

var configPath: String? = null

fun main(args: Array<String>) {
	configPath = args.first()
	runApplication<AgentApplication>(*args)
}

