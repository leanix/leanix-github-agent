package net.leanix.githubagent.config

import jakarta.annotation.PostConstruct
import net.leanix.githubagent.exceptions.GithubEnterpriseConfigurationMissingException
import org.springframework.stereotype.Component

@Component
class AgentSetupValidation(
    private val githubEnterpriseProperties: GithubEnterpriseProperties
) {

    @PostConstruct
    fun validateConfiguration() {
        val missingProperties = mutableListOf<String>()

        if (githubEnterpriseProperties.baseUrl.isBlank()) {
            missingProperties.add("GITHUB_ENTERPRISE_BASE_URL")
        }
        if (githubEnterpriseProperties.githubAppId.isBlank()) {
            missingProperties.add("GITHUB_ENTERPRISE_GITHUB_APP_ID")
        }
        if (githubEnterpriseProperties.pemFile.isBlank()) {
            missingProperties.add("GITHUB_ENTERPRISE_PEM_FILE")
        }

        if (missingProperties.isNotEmpty()) {
            throw GithubEnterpriseConfigurationMissingException(missingProperties.joinToString(", "))
        }
    }
}
