package net.leanix.githubagent.services

import net.leanix.githubagent.client.GithubClient
import net.leanix.githubagent.dto.GithubAppResponse
import net.leanix.githubagent.exceptions.GithubAppInsufficientPermissionsException
import net.leanix.githubagent.exceptions.UnableToConnectToGithubEnterpriseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GitHubEnterpriseService(private val githubClient: GithubClient) {

    companion object {
        val expectedPermissions = listOf("administration", "contents", "metadata")
        val expectedEvents = listOf("label", "public", "repository")
    }
    private val logger = LoggerFactory.getLogger(GitHubEnterpriseService::class.java)

    fun verifyJwt(jwt: String) {
        runCatching {
            val githubApp = githubClient.getApp("Bearer $jwt")
            validateGithubAppResponse(githubApp)
            logger.info("Authenticated as GitHub App: '${githubApp.name}'")
        }.onFailure {
            when (it) {
                is GithubAppInsufficientPermissionsException -> throw it
                else -> throw UnableToConnectToGithubEnterpriseException("Failed to verify JWT token")
            }
        }
    }

    fun validateGithubAppResponse(response: GithubAppResponse) {
        val missingPermissions = expectedPermissions.filterNot { response.permissions.containsKey(it) }
        val missingEvents = expectedEvents.filterNot { response.events.contains(it) }

        if (missingPermissions.isNotEmpty() || missingEvents.isNotEmpty()) {
            var message = "GitHub App is missing the following "
            if (missingPermissions.isNotEmpty()) {
                message = message.plus("permissions: $missingPermissions")
            }
            if (missingEvents.isNotEmpty()) {
                if (missingPermissions.isNotEmpty()) {
                    message = message.plus(", and the following")
                }
                message = message.plus("events: $missingEvents")
            }
            throw GithubAppInsufficientPermissionsException(message)
        }
    }
}
