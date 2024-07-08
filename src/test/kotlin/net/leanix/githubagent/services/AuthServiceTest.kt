package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.client.AuthClient
import net.leanix.githubagent.config.LeanIXProperties
import net.leanix.githubagent.dto.JwtDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthServiceTest {

    private lateinit var authService: AuthService
    private val authClient: AuthClient = mockk()
    private val leanIXProperties: LeanIXProperties = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        authService = AuthService(authClient, leanIXProperties)
        every { leanIXProperties.auth.apiUserToken } returns "apiUserToken"
        every { authClient.getToken(any(), any()) } returns JwtDto(
            "valid_access_token",
            false,
            "valid_access_token",
            "access_token",
            3600
        )
    }

    @Test
    fun `getBearerToken should return valid token`() {
        val token = authService.getBearerToken()

        assertEquals("valid_access_token", token)
        verify {
            authClient.getToken("Basic YXBpdG9rZW46YXBpVXNlclRva2Vu", "grant_type=client_credentials")
        }
    }
}
