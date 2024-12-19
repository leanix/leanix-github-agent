package net.leanix.githubagent.config

import net.leanix.githubagent.exceptions.GitHubEnterpriseConfigurationMissingException
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AgentSetupValidation(
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties,
    private val leanIXProperties: LeanIXProperties
) {

    @EventListener
    @SuppressWarnings("UnusedParameter")
    fun onApplicationEvent(event: ContextRefreshedEvent) {
        val missingProperties = mutableListOf<String>()

        if (gitHubEnterpriseProperties.baseUrl.isBlank()) {
            missingProperties.add("GITHUB_ENTERPRISE_BASE_URL")
        }
        if (gitHubEnterpriseProperties.gitHubAppId.isBlank()) {
            missingProperties.add("GITHUB_ENTERPRISE_GITHUB_APP_ID")
        }
        if (gitHubEnterpriseProperties.pemFile.isBlank()) {
            missingProperties.add("GITHUB_ENTERPRISE_PEM_FILE")
        }

        if (leanIXProperties.wsBaseUrl.isBlank()) {
            missingProperties.add("LEANIX_WS_BASE_URL")
        }

        if (leanIXProperties.auth.accessTokenUri.isBlank()) {
            missingProperties.add("LEANIX_AUTH_ACCESS_TOKEN_URI")
        }

        if (missingProperties.isNotEmpty()) {
            throw GitHubEnterpriseConfigurationMissingException(missingProperties.joinToString(", "))
        }
    }
}
