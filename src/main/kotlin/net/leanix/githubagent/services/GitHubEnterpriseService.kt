package net.leanix.githubagent.services

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.GitHubAppResponse
import net.leanix.githubagent.exceptions.GitHubAppInsufficientPermissionsException
import net.leanix.githubagent.exceptions.UnableToConnectToGitHubEnterpriseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GitHubEnterpriseService(private val githubClient: GitHubClient) {

    companion object {
        val expectedPermissions = listOf("administration", "contents", "metadata")
        val expectedEvents = listOf("label", "public", "repository", "push")
    }
    private val logger = LoggerFactory.getLogger(GitHubEnterpriseService::class.java)

    fun verifyJwt(jwt: String) {
        runCatching {
            val githubApp = getGitHubApp(jwt)
            validateGithubAppResponse(githubApp)
            logger.info("Authenticated as GitHub App: '${githubApp.slug}'")
        }.onFailure {
            logger.error("Failed to verify JWT token", it)
            when (it) {
                is GitHubAppInsufficientPermissionsException -> throw it
                else -> throw UnableToConnectToGitHubEnterpriseException("Failed to verify JWT token")
            }
        }
    }

    fun validateGithubAppResponse(response: GitHubAppResponse) {
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
            throw GitHubAppInsufficientPermissionsException(message)
        }
    }

    fun getGitHubApp(jwt: String) = githubClient.getApp("Bearer $jwt")
}
