package io.github.markgregg.agent

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AgentApplicationKtTest : FunSpec({

    test("main") {
        main(arrayOf(AgentApplicationTests::class.java.classLoader.getResource("config.json")!!.path))
        configPath shouldBe AgentApplicationTests::class.java.classLoader.getResource("config.json")!!.path
    }
})