package io.github.markgregg.agent

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AgentApplicationTests {
	init {
		configPath = AgentApplicationTests::class.java.classLoader.getResource("config.json")!!.path
	}

	@Test
	fun contextLoads() {

	}

}
