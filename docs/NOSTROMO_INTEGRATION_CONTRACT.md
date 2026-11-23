# NOSTROMO Mobile Integration Contract

**Status:** contract only. This file does not claim the mobile cockpit, backend routes, provider consent, or device E2E are installed.

## Boundary and authority

- Switchboard is a client of the one authenticated LibreChat backend. LibreChat owns the conversation, session, agent catalog, consent, tool approvals, MCP, receipts, and MongoDB.
- ChatGPT SIWC is the primary conversational model and is invoked by the server. The mobile app does not implement model routing or hold provider credentials.
- The server may start the configured Qwen3 4B model only for an admitted NOSTROMO agent/router request. It must use the physical GPU, reject CPU fallback, and unload after the request. The 4 GB mock is test-only.
- NOSTROMO MCP and all external connectors run server-side. Mobile does not call MCP directly or store GitHub, Google, Microsoft, Meta, or MANOJOS3 credentials.
- A mobile selector, card, artifact, or model response cannot authorize an action. Approval stays in the authenticated LibreChat tool-agreement flow.

## Presentation contract

Render NOSTROMO HUD, Tentáculo Hub, Context Inspector, Artifact Dock, Interactive Artifacts, Plan Graph, Active Flow, Interflow Map, Resource Tree, Decision Inbox, Background Tasks, Capability Lens, Evidence Viewer, and Status Rail as client views over authenticated server projections. Reuse the existing chat, artifact host, approval UI, and MCP Apps host; do not add a second agent, scheduler, store, or execution path.

Each view must:
- use the logged-in account and current server session;
- request only its required read capability and stop polling/subscriptions when hidden or signed out;
- display UNKNOWN, STALE, or NO CONFIGURADO when the server cannot provide a fresh authorized readback;
- show provenance, freshness, and receipt identifiers for material state;
- route any action back through the server's existing authenticated agreement and approval flow;
- keep local snapshots account-scoped and non-sensitive; opening a snapshot must refresh from the server.

## Compatibility gates

- Keep the normal chat usable when NOSTROMO projections or MCP Apps are unavailable.
- The reconciled mobile candidate pins LibreChat 0.8.9, but MCP Apps must remain disabled until the connected backend advertises the compatible API and authenticated capability readback succeeds.
- Agent selection uses the exact server agent ID and displays server configuration separately from live health.
- Do not report a mobile build or NOSTROMO integration as complete from SOURCE inspection alone.

## Required verification before release

1. Run common tests and Android build on the exact candidate commit; run iOS compile/tests and produce the intended signed or explicitly unsigned artifact.
2. Test against the actual LibreChat backend version and account using login, streaming, reconnect, approval, agent selection, artifacts, and graceful capability-unavailable behavior.
3. Verify each cockpit view against a fresh authenticated server response; test sign-out/account-switch cache isolation and hidden-panel resource release.
4. Complete one harmless authenticated NOSTROMO status turn and reconcile the same conversation, agent ID, approval state, MCP receipt, and Mongo persistence.
5. Record commit, backend compatibility readback, CI/device evidence, and remaining gates in the mobile release receipt.

## Shared server contract

The canonical architecture, data context order, OAuth scopes, connector boundaries, resource lifecycle, and local installation plan live in the private LibreChat VX repository at docs/NOSTROMO_WEB_MOBILE_CANONICAL_INTEGRATION_20261123.md. This mobile document defines only the client boundary and acceptance contract; the private server remains the single authority.
