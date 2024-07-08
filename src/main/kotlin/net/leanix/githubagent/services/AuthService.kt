package net.leanix.githubagent.services

import net.leanix.githubagent.client.AuthClient
import net.leanix.githubagent.config.LeanIXProperties
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthService(
    private val authClient: AuthClient,
    private val leanIXProperties: LeanIXProperties
) {

    fun getBearerToken(): String {
        return authClient.getToken(
            authorization = getBasicAuthHeader(),
            body = "grant_type=client_credentials",
        ).accessToken
    }

    private fun getBasicAuthHeader(): String =
        "Basic " + Base64.getEncoder().encodeToString(
            "apitoken:${leanIXProperties.auth.apiUserToken}".toByteArray(),
        )
}
