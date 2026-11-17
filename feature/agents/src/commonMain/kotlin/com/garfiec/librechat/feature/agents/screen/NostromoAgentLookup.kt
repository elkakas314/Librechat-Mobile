package com.garfiec.librechat.feature.agents.screen

import com.garfiec.librechat.core.model.Agent

/** Exact-name discovery: ambiguity is surfaced instead of routing to an arbitrary agent. */
internal sealed interface NostromoAgentLookup {
    data object Missing : NostromoAgentLookup
    data object Ambiguous : NostromoAgentLookup
    data class Found(val agent: Agent) : NostromoAgentLookup
}

internal fun findNostromoAgent(agents: List<Agent>): NostromoAgentLookup {
    val matches = agents.filter { it.name?.trim()?.equals("NOSTROMO", ignoreCase = true) == true }
    return when (matches.size) {
        0 -> NostromoAgentLookup.Missing
        1 -> NostromoAgentLookup.Found(matches.single())
        else -> NostromoAgentLookup.Ambiguous
    }
}
