package net.leanix.githubagent.services

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.exceptions.GitHubAppInsufficientPermissionsException
import net.leanix.githubagent.exceptions.UnableToConnectToGitHubEnterpriseException
import net.leanix.githubagent.shared.GITHUB_APP_LABEL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GitHubEnterpriseService(
    private val githubClient: GitHubClient,
    private val syncLogService: SyncLogService,
) {

    companion object {
        val expectedPermissions = listOf("administration", "contents", "metadata")
        val expectedEvents = listOf("label", "public", "repository", "push")
    }
    private val logger = LoggerFactory.getLogger(GitHubEnterpriseService::class.java)

    fun verifyJwt(jwt: String) {
        runCatching {
            val githubApp = getGitHubApp(jwt)
            validateEnabledPermissionsAndEvents(GITHUB_APP_LABEL, githubApp.permissions, githubApp.events)
            logger.info("Authenticated as GitHub App: '${githubApp.slug}'")
        }.onFailure {
            when (it) {
                is GitHubAppInsufficientPermissionsException -> {
                    logger.error(it.message)
                    syncLogService.sendErrorLog(it.message!!)
                }
                else -> {
                    logger.error("Failed to verify JWT token", it)
                    throw UnableToConnectToGitHubEnterpriseException("Failed to verify JWT token")
                }
            }
        }
    }

    fun validateEnabledPermissionsAndEvents(type: String, permissions: Map<String, String>, events: List<String>) {
        val missingPermissions = expectedPermissions.filterNot { permissions.containsKey(it) }
        val missingEvents = expectedEvents.filterNot { events.contains(it) }

        if (missingPermissions.isNotEmpty() || missingEvents.isNotEmpty()) {
            var message = "$type missing the following "
            if (missingPermissions.isNotEmpty()) {
                message = message.plus("permissions: $missingPermissions")
            }
            if (missingEvents.isNotEmpty()) {
                if (missingPermissions.isNotEmpty()) {
                    message = message.plus(", and the following ")
                }
                message = message.plus("events: $missingEvents")
            }
            throw GitHubAppInsufficientPermissionsException(message)
        }
    }

    fun getGitHubApp(jwt: String) = githubClient.getApp("Bearer $jwt")
}
