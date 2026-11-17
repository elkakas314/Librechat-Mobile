package com.garfiec.librechat.feature.agents.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.core.common.result.Result
import com.garfiec.librechat.core.data.repository.AgentRepository
import com.garfiec.librechat.core.model.Agent
import com.garfiec.librechat.feature.agents.resources.Res
import com.garfiec.librechat.feature.agents.resources.nostromo_agent_ambiguous
import com.garfiec.librechat.feature.agents.resources.nostromo_agent_configured
import com.garfiec.librechat.feature.agents.resources.nostromo_agent_missing
import com.garfiec.librechat.feature.agents.resources.nostromo_agent_not_live
import com.garfiec.librechat.feature.agents.resources.nostromo_agent_tools
import com.garfiec.librechat.feature.agents.resources.nostromo_agent_tools_unknown
import com.garfiec.librechat.feature.agents.resources.nostromo_back
import com.garfiec.librechat.feature.agents.resources.nostromo_device_control
import com.garfiec.librechat.feature.agents.resources.nostromo_error
import com.garfiec.librechat.feature.agents.resources.nostromo_open_profile
import com.garfiec.librechat.feature.agents.resources.nostromo_prepare_audit
import com.garfiec.librechat.feature.agents.resources.nostromo_refresh
import com.garfiec.librechat.feature.agents.resources.nostromo_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private const val SELF_AUDIT_PROMPT = """NOSTROMO, inicia tu autodiagnóstico operativo. Comprueba tu identidad, modelo, herramientas MCP, servicios, repositorio, capacidades de ejecución y estado persistente. Distingue lo instalado de lo verdaderamente funcional. Devuélveme los fallos concretos, las reparaciones posibles, los permisos necesarios y un plan ordenado con criterios de aceptación. No inventes resultados ni ejecutes operaciones destructivas."""

private sealed interface ScreenState {
    data object Loading : ScreenState
    data class Ready(val lookup: NostromoAgentLookup) : ScreenState
    data class Failed(val message: String?) : ScreenState
}

/** Native, touch-first entry to the existing NOSTROMO agent; never starts a chat implicitly. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NostromoHubScreen(
    onBack: () -> Unit,
    onOpenAgent: (String) -> Unit,
    onStartChatWithPrompt: (agentId: String, prompt: String) -> Unit,
    onOpenDeviceControl: () -> Unit,
) {
    val repository: AgentRepository = koinInject()
    var refreshGeneration by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ScreenState>(ScreenState.Loading) }

    LaunchedEffect(refreshGeneration) {
        state = ScreenState.Loading
        state = when (val result = repository.getAgents()) {
            is Result.Success -> ScreenState.Ready(findNostromoAgent(result.data))
            is Result.Error -> ScreenState.Failed(result.message)
            Result.Loading -> ScreenState.Loading
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.nostromo_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.nostromo_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshGeneration++ },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.nostromo_refresh)) }
                },
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.nostromo_agent_not_live), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpenDeviceControl,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.nostromo_device_control))
            }
            when (val current = state) {
                ScreenState.Loading -> Text(stringResource(Res.string.nostromo_refresh))
                is ScreenState.Failed -> Text(
                    current.message ?: stringResource(Res.string.nostromo_error),
                    color = MaterialTheme.colorScheme.error,
                )
                is ScreenState.Ready -> when (val lookup = current.lookup) {
                    NostromoAgentLookup.Missing -> Text(stringResource(Res.string.nostromo_agent_missing))
                    NostromoAgentLookup.Ambiguous -> Text(stringResource(Res.string.nostromo_agent_ambiguous))
                    is NostromoAgentLookup.Found -> AgentCard(
                        agent = lookup.agent,
                        onOpenAgent = { onOpenAgent(lookup.agent.id) },
                        onPrepareAudit = { onStartChatWithPrompt(lookup.agent.id, SELF_AUDIT_PROMPT) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentCard(agent: Agent, onOpenAgent: () -> Unit, onPrepareAudit: () -> Unit) {
    val tools = agent.tools
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(agent.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
            Text(
                listOfNotNull(agent.provider, agent.model).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(Res.string.nostromo_agent_configured), style = MaterialTheme.typography.labelLarge)
            if (tools == null) {
                Text(stringResource(Res.string.nostromo_agent_tools_unknown))
            } else {
                Text(stringResource(Res.string.nostromo_agent_tools, tools.size))
            }
            tools.orEmpty().forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.heightIn(min = 4.dp))
            Button(
                onClick = onPrepareAudit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(Res.string.nostromo_prepare_audit)) }
            OutlinedButton(
                onClick = onOpenAgent,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(Res.string.nostromo_open_profile)) }
        }
    }
}
