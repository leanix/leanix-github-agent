package net.leanix.githubagent.client

import feign.Response
import io.github.resilience4j.retry.annotation.Retry
import net.leanix.githubagent.config.FeignClientConfig
import net.leanix.githubagent.dto.ArtifactsListResponse
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
import org.springframework.web.bind.annotation.RequestBody
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

    @Retry(name = "secondary_rate_limit")
    @GetMapping("/api/v3/app/installations")
    fun getInstallations(
        @RequestHeader("Authorization") jwt: String,
        @RequestParam("per_page", defaultValue = "30") perPage: Int,
        @RequestParam("page", defaultValue = "1") page: Int
    ): List<Installation>

    @Retry(name = "secondary_rate_limit")
    @GetMapping("/api/v3/app/installations/{installationId}")
    fun getInstallation(
        @PathVariable("installationId") installationId: Long,
        @RequestHeader("Authorization") jwt: String
    ): Installation

    @Retry(name = "secondary_rate_limit")
    @PostMapping("/api/v3/app/installations/{installationId}/access_tokens")
    fun createInstallationToken(
        @PathVariable("installationId") installationId: Long,
        @RequestHeader("Authorization") jwt: String,
        @RequestBody emptyBody: String = ""
    ): InstallationTokenResponse

    @Retry(name = "secondary_rate_limit")
    @GetMapping("/api/v3/organizations")
    fun getOrganizations(
        @RequestHeader("Authorization") jwt: String,
        @RequestParam("per_page", defaultValue = "30") perPage: Int,
        @RequestParam("since", defaultValue = "1") since: Int
    ): List<Organization>

    @Retry(name = "secondary_rate_limit")
    @GetMapping("/api/v3/orgs/{org}/repos")
    fun getRepositories(
        @PathVariable("org") org: String,
        @RequestHeader("Authorization") token: String
    ): List<Repository>

    @Retry(name = "secondary_rate_limit")
    @GetMapping("/api/v3/search/code")
    fun searchManifestFiles(
        @RequestHeader("Authorization") token: String,
        @RequestParam("q") query: String,
    ): GitHubSearchResponse

    @Retry(name = "secondary_rate_limit")
    @GetMapping("/api/v3/repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    fun getRunArtifacts(
        @PathVariable("owner") owner: String,
        @PathVariable("repo") repo: String,
        @PathVariable("runId") runId: Long,
        @RequestHeader("Authorization") token: String
    ): ArtifactsListResponse

    @Retry(name = "secondary_rate_limit")
    @GetMapping("/api/v3/repos/{owner}/{repo}/actions/artifacts/{artifactId}/zip")
    fun downloadArtifact(
        @PathVariable("owner") owner: String,
        @PathVariable("repo") repo: String,
        @PathVariable("artifactId") artifactId: Long,
        @RequestHeader("Authorization") token: String
    ): Response
}
