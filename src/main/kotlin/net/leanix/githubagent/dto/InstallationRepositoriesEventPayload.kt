package net.leanix.githubagent.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstallationRepositoriesEventPayload(
    val installation: InstallationRepositoriesInstallation,
    @JsonProperty("repositories_added") val repositoriesAdded: List<InstallationRepositoriesRepository>
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
    @JsonProperty("node_id") val nodeId: String,
    val name: String,
    @JsonProperty("full_name") val fullName: String,
    val private: Boolean
)

fun InstallationRepositoriesInstallation.toInstallation(): Installation {
    return Installation(
        id = id.toLong(),
        account = Account(login = account.login),
        events = emptyList(),
        permissions = emptyMap()
    )
}