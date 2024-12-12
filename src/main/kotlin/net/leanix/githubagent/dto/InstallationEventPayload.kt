package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationEventPayload(
    val action: String,
    val installation: InstallationEventInstallation,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationEventInstallation(
    val id: Int,
    val account: InstallationEventInstallationAccount
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationEventInstallationAccount(
    val login: String,
    val id: Int
)
