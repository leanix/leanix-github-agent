package net.leanix.githubagent.client

import net.leanix.githubagent.dto.JwtDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(
    name = "authentication",
    url = "\${leanix.auth.access-token-uri}",
)
fun interface AuthClient {

    @PostMapping(value = ["/oauth2/token"], consumes = [APPLICATION_FORM_URLENCODED_VALUE])
    fun getToken(
        @RequestHeader(name = AUTHORIZATION) authorization: String,
        @RequestBody body: String,
    ): JwtDto
}
