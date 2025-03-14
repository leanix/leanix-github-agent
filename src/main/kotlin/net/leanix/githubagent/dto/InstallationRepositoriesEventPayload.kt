package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationRepositoriesEventPayload(
    val installation: InstallationRepositoriesInstallation,
    val repositoriesAdded: List<InstallationRepositoriesRepository>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationRepositoriesInstallation(
    val id: Int,
    val account: InstallationEventRepositoriesInstallationAccount
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationEventRepositoriesInstallationAccount(
    val login: String,
    val id: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationRepositoriesRepository(
    val id: Int,
    val nodeId: String,
    val name: String,
    val fullName: String,
    val private: Boolean
)