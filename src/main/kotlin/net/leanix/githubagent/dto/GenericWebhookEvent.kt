package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class GenericWebhookEvent(
    val action: String? = null
)
