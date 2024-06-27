package net.leanix.githubagent.client

import net.leanix.githubagent.dto.GithubAppResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(name = "githubClient", url = "\${github-enterprise.baseUrl}")
interface GithubClient {

    @GetMapping("/api/v3/app")
    fun getApp(
        @RequestHeader("Authorization") jwt: String,
        @RequestHeader("Accept") accept: String = "application/vnd.github.v3+json"
    ): GithubAppResponse
}
