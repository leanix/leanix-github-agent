package net.leanix.githubagent.services

import net.leanix.githubagent.client.LeanIXAuthClient
import net.leanix.githubagent.config.LeanIXProperties
import org.springframework.stereotype.Service
import java.util.*

@Service
class LeanIXAuthService(
    private val leanIXAuthClient: LeanIXAuthClient,
    private val leanIXProperties: LeanIXProperties
) {

    fun getBearerToken(): String {
        return leanIXAuthClient.getToken(
            authorization = getBasicAuthHeader(),
            body = "grant_type=client_credentials",
        ).accessToken
    }

    private fun getBasicAuthHeader(): String =
        "Basic " + Base64.getEncoder().encodeToString(
            "apitoken:${leanIXProperties.auth.technicalUserToken}".toByteArray(),
        )
}
