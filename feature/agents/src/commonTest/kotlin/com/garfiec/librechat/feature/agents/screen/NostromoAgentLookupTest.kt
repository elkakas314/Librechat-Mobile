package com.garfiec.librechat.feature.agents.screen

import com.garfiec.librechat.core.model.Agent
import kotlin.test.Test
import kotlin.test.assertEquals

class NostromoAgentLookupTest {
    @Test
    fun findsExactAgentNameIgnoringCaseAndWhitespace() {
        val agent = Agent(id = "agent-1", name = " NOSTROMO ")
        assertEquals(NostromoAgentLookup.Found(agent), findNostromoAgent(listOf(agent)))
    }

    @Test
    fun doesNotSelectAnUnrelatedAgent() {
        assertEquals(
            NostromoAgentLookup.Missing,
            findNostromoAgent(listOf(Agent(id = "agent-2", name = "NOSTROMO Backup"))),
        )
    }

    @Test
    fun reportsDuplicateNamesInsteadOfChoosingArbitrarily() {
        assertEquals(
            NostromoAgentLookup.Ambiguous,
            findNostromoAgent(listOf(Agent(id = "agent-1", name = "NOSTROMO"), Agent(id = "agent-2", name = "nostromo"))),
        )
    }
}
