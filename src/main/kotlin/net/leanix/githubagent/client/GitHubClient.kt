package net.leanix.githubagent.client

import net.leanix.githubagent.config.FeignClientConfig
import net.leanix.githubagent.dto.GitHubAppResponse
import net.leanix.githubagent.dto.GitHubSearchResponse
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.dto.InstallationTokenResponse
import net.leanix.githubagent.dto.Organization
import net.leanix.githubagent.dto.Repository
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "githubClient",
    url = "\${github-enterprise.baseUrl}",
    configuration = [FeignClientConfig::class]
)
interface GitHubClient {

    @GetMapping("/api/v3/app")
    fun getApp(
        @RequestHeader("Authorization") jwt: String,
        @RequestHeader("Accept") accept: String = "application/vnd.github.v3+json"
    ): GitHubAppResponse

    @GetMapping("/api/v3/app/installations")
    fun getInstallations(@RequestHeader("Authorization") jwt: String): List<Installation>

    @PostMapping("/api/v3/app/installations/{installationId}/access_tokens")
    fun createInstallationToken(
        @PathVariable("installationId") installationId: Long,
        @RequestHeader("Authorization") jwt: String
    ): InstallationTokenResponse

    @GetMapping("/api/v3/organizations")
    fun getOrganizations(@RequestHeader("Authorization") token: String): List<Organization>

    @GetMapping("/api/v3/orgs/{org}/repos")
    fun getRepositories(
        @PathVariable("org") org: String,
        @RequestHeader("Authorization") token: String
    ): List<Repository>

    @GetMapping("/api/v3/search/code")
    fun searchManifestFiles(
        @RequestHeader("Authorization") token: String,
        @RequestParam("q") query: String,
    ): GitHubSearchResponse
}
