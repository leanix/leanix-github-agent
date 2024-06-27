import io.mockk.every
import io.mockk.mockk
import net.leanix.githubagent.client.GithubClient
import net.leanix.githubagent.dto.GithubAppResponse
import net.leanix.githubagent.exceptions.GithubAppInsufficientPermissionsException
import net.leanix.githubagent.exceptions.UnableToConnectToGithubEnterpriseException
import net.leanix.githubagent.services.GitHubEnterpriseService
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class GitHubEnterpriseServiceTest {

    private val githubClient = mockk<GithubClient>()
    private val service = GitHubEnterpriseService(githubClient)

    @Test
    fun `verifyJwt with valid jwt should not throw exception`() {
        val jwt = "validJwt"
        val githubApp = GithubAppResponse(
            name = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read"),
            events = listOf("label", "public", "repository")
        )
        every { githubClient.getApp(any()) } returns githubApp

        assertDoesNotThrow { service.verifyJwt(jwt) }
    }

    @Test
    fun `verifyJwt with invalid jwt should throw exception`() {
        val jwt = "invalidJwt"
        every { githubClient.getApp(any()) } throws Exception()

        assertThrows(UnableToConnectToGithubEnterpriseException::class.java) { service.verifyJwt(jwt) }
    }

    @Test
    fun `validateGithubAppResponse with correct permissions should not throw exception`() {
        val response = GithubAppResponse(
            name = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read"),
            events = listOf("label", "public", "repository")
        )

        assertDoesNotThrow { service.validateGithubAppResponse(response) }
    }

    @Test
    fun `validateGithubAppResponse with missing permissions should throw exception`() {
        val response = GithubAppResponse(
            name = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read"),
            events = listOf("label", "public", "repository")
        )

        assertThrows(
            GithubAppInsufficientPermissionsException::class.java
        ) { service.validateGithubAppResponse(response) }
    }

    @Test
    fun `validateGithubAppResponse with missing events should throw exception`() {
        val response = GithubAppResponse(
            name = "validApp",
            permissions = mapOf("administration" to "read", "contents" to "read", "metadata" to "read"),
            events = listOf("label", "public")
        )

        assertThrows(
            GithubAppInsufficientPermissionsException::class.java
        ) { service.validateGithubAppResponse(response) }
    }
}