package net.leanix.githubagent.services

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.Organization
import org.springframework.stereotype.Service

@Service
class GitHubAPIService(
    private val gitHubClient: GitHubClient,
) {

    companion object {
        private const val PER_PAGE = 30 // Maximum allowed by GitHub API is 100
    }

    fun getPaginatedInstallations(jwtToken: String): List<Installation> {
        val installations = mutableListOf<Installation>()
        var page = 1
        var currentInstallations: List<Installation>

        do {
            currentInstallations = gitHubClient.getInstallations("Bearer $jwtToken", PER_PAGE, page)
            if (currentInstallations.isNotEmpty()) installations.addAll(currentInstallations) else break
            page++
        } while (currentInstallations.size == PER_PAGE)
        return installations
    }

    fun getPaginatedOrganizations(installationToken: String): List<Organization> {
        val organizations = mutableListOf<Organization>()
        var since = 1
        var currentOrganizations: List<Organization>

        do {
            currentOrganizations = gitHubClient.getOrganizations("Bearer $installationToken", PER_PAGE, since)
            if (currentOrganizations.isNotEmpty()) organizations.addAll(currentOrganizations) else break
            since = currentOrganizations.last().id
        } while (currentOrganizations.size == PER_PAGE)
        return organizations
    }
}
