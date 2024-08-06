package net.leanix.githubagent.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.exceptions.FailedToCreateJWTException
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
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

@Service
class GitHubAuthenticationService(
    private val cachingService: CachingService,
    private val githubEnterpriseProperties: GitHubEnterpriseProperties,
    private val resourceLoader: ResourceLoader,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val gitHubClient: GitHubClient,
) {

    companion object {
        private const val JWT_EXPIRATION_DURATION = 600000L
        private const val pemPrefix = "-----BEGIN RSA PRIVATE KEY-----"
        private const val pemSuffix = "-----END RSA PRIVATE KEY-----"
        private val logger = LoggerFactory.getLogger(GitHubAuthenticationService::class.java)
    }

    fun refreshTokens() {
        generateAndCacheJwtToken()
        val jwtToken = cachingService.get("jwtToken")
        generateAndCacheInstallationTokens(
            gitHubClient.getInstallations("Bearer $jwtToken"),
            jwtToken.toString()
        )
    }

    fun generateAndCacheJwtToken() {
        runCatching {
            logger.info("Generating JWT token")
            Security.addProvider(BouncyCastleProvider())
            val rsaPrivateKey: String = readPrivateKey()
            val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(rsaPrivateKey))
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            val jwt = createJwtToken(privateKey)
            gitHubEnterpriseService.verifyJwt(jwt.getOrThrow())
            cachingService.set("jwtToken", jwt.getOrThrow(), JWT_EXPIRATION_DURATION)
        }.onFailure {
            logger.error("Failed to generate/validate JWT token", it)
            if (it is InvalidKeySpecException) {
                throw IllegalArgumentException("The provided private key is not in a valid PKCS8 format.", it)
            } else {
                throw it
            }
        }
    }

    private fun createJwtToken(privateKey: PrivateKey): Result<String> {
        return runCatching {
            Jwts.builder()
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() + JWT_EXPIRATION_DURATION))
                .setIssuer(cachingService.get("githubAppId").toString())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact()
        }.onFailure {
            throw FailedToCreateJWTException("Failed to generate a valid JWT token")
        }
    }

    fun generateAndCacheInstallationTokens(
        installations: List<Installation>,
        jwtToken: String
    ) {
        installations.forEach { installation ->
            val installationToken = gitHubClient.createInstallationToken(installation.id, "Bearer $jwtToken").token
            cachingService.set("installationToken:${installation.id}", installationToken, 3600L)
        }
    }

    @Throws(IOException::class)
    private fun readPrivateKey(): String {
        val pemFile = File(resourceLoader.getResource("file:${githubEnterpriseProperties.pemFile}").uri)
        val fileContent = String(Files.readAllBytes(pemFile.toPath()), Charset.defaultCharset()).trim()

        require(fileContent.startsWith(pemPrefix) && fileContent.endsWith(pemSuffix)) {
            "The provided file is not a valid PEM file."
        }

        return fileContent
            .replace(pemPrefix, "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace(pemSuffix, "")
    }
}
