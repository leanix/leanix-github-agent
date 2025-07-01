package net.leanix.githubagent.helper

import net.leanix.githubagent.handler.SupportedWebhookEvent

fun defaultSupportedWebhookEvents() =
    listOf(
        SupportedWebhookEvent("WORKFLOW_RUN", listOf("completed")),
        SupportedWebhookEvent("REPOSITORY", listOf("archived", "unarchived", "renamed", "edited")),
        SupportedWebhookEvent("INSTALLATION_REPOSITORIES", listOf("removed", "added")),
        SupportedWebhookEvent("PUSH", emptyList()),
        SupportedWebhookEvent("INSTALLATION", listOf("created")),
        SupportedWebhookEvent("ORGANIZATION", listOf("created"))
    )
