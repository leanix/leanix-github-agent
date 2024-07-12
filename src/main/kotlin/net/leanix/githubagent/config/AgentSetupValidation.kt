package net.leanix.githubagent.config

import jakarta.annotation.PostConstruct
import net.leanix.githubagent.exceptions.GitHubEnterpriseConfigurationMissingException
import org.springframework.stereotype.Component

@Component
class AgentSetupValidation(
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties
) {

    @PostConstruct
    fun validateConfiguration() {
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

        if (missingProperties.isNotEmpty()) {
            throw GitHubEnterpriseConfigurationMissingException(missingProperties.joinToString(", "))
        }
    }
}
