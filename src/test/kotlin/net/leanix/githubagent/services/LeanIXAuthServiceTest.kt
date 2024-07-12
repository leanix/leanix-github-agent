package net.leanix.githubagent.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.client.LeanIXAuthClient
import net.leanix.githubagent.config.LeanIXProperties
import net.leanix.githubagent.dto.JwtDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LeanIXAuthServiceTest {

    private lateinit var leanIXAuthService: LeanIXAuthService
    private val leanIXAuthClient: LeanIXAuthClient = mockk()
    private val leanIXProperties: LeanIXProperties = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        leanIXAuthService = LeanIXAuthService(leanIXAuthClient, leanIXProperties)
        every { leanIXProperties.auth.technicalUserToken } returns "apiUserToken"
        every { leanIXAuthClient.getToken(any(), any()) } returns JwtDto(
            "valid_access_token",
            false,
            "valid_access_token",
            "access_token",
            3600
        )
    }

    @Test
    fun `getBearerToken should return valid token`() {
        val token = leanIXAuthService.getBearerToken()

        assertEquals("valid_access_token", token)
        verify {
            leanIXAuthClient.getToken("Basic YXBpdG9rZW46YXBpVXNlclRva2Vu", "grant_type=client_credentials")
        }
    }
}
