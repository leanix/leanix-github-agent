package net.leanix.githubagent.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import net.leanix.githubagent.client.GithubClient
import net.leanix.githubagent.config.GithubEnterpriseProperties
import net.leanix.githubagent.dto.GithubAppResponse
import net.leanix.githubagent.exceptions.AuthenticationFailedException
import net.leanix.githubagent.exceptions.ConnectingToGithubEnterpriseFailedException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

@Service
class GithubAuthenticationService(
    private val cachingService: CachingService,
    private val githubClient: GithubClient,
    private val githubEnterpriseProperties: GithubEnterpriseProperties,
    private val resourceLoader: ResourceLoader
) {

    companion object {
        private const val JWT_EXPIRATION_DURATION = 600000L
        private val logger = LoggerFactory.getLogger(GithubAuthenticationService::class.java)
    }

    fun generateJwtToken() {
        runCatching {
            logger.info("Generating JWT token")
            Security.addProvider(BouncyCastleProvider())
            val rsaPrivateKey: String = readPrivateKey(loadPemFile())
            val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(rsaPrivateKey))
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            createJwtToken(privateKey)?.also {
                cachingService.set("jwtToken", it)
                verifyJwt(it)
            } ?: throw AuthenticationFailedException("Failed to generate a valid JWT token")
        }
    }

    private fun createJwtToken(privateKey: PrivateKey): String? {
        return Jwts.builder()
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + JWT_EXPIRATION_DURATION))
            .setIssuer(cachingService.get("githubAppId"))
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact()
    }

    @Throws(IOException::class)
    private fun readPrivateKey(file: File): String {
        return String(Files.readAllBytes(file.toPath()), Charset.defaultCharset())
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END RSA PRIVATE KEY-----", "")
    }

    private fun verifyJwt(jwt: String) {
        runCatching {
            val githubApp = jacksonObjectMapper().readValue(
                githubClient.getApp("Bearer $jwt"),
                GithubAppResponse::class.java
            )
            logger.info("Authenticated as GitHub App: ${githubApp.name}")
        }.onFailure {
            throw ConnectingToGithubEnterpriseFailedException("Failed to verify JWT token")
        }
    }

    private fun loadPemFile() =
        File(resourceLoader.getResource("file:${githubEnterpriseProperties.pemFile}").uri)
}
