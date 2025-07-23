package net.leanix.githubagent.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.Installation
import net.leanix.githubagent.exceptions.FailedToCreateJWTException
import net.leanix.githubagent.exceptions.JwtTokenNotFound
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
class GitHubAuthenticationService(
    private val cachingService: CachingService,
    private val githubEnterpriseProperties: GitHubEnterpriseProperties,
    private val resourceLoader: ResourceLoader,
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    private val gitHubClient: GitHubClient,
    private val gitHubAPIService: GitHubAPIService,
) {

    companion object {
        private const val JWT_EXPIRATION_DURATION_IN_MILLISECONDS = 600000L
        private const val INSTALLATION_JWT_EXPIRATION_DURATION_IN_MILLISECONDS = 300000L
        private const val pemPrefix = "-----BEGIN RSA PRIVATE KEY-----"
        private const val pemSuffix = "-----END RSA PRIVATE KEY-----"
        private val logger = LoggerFactory.getLogger(GitHubAuthenticationService::class.java)
    }

    fun refreshTokens() {
        generateAndCacheJwtToken()
        val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
        val installations = gitHubAPIService.getPaginatedInstallations(jwtToken.toString())
        generateAndCacheInstallationTokens(installations, jwtToken.toString())
    }

    fun generateAndCacheJwtToken() {
        runCatching {
            logger.info("Generating JWT token")
            val privateKey = loadPrivateKey()
            val jwt = createJwtToken(privateKey)
            verifyAndCacheJwtToken(jwt)
        }.onFailure {
            logger.error("Failed to generate a valid jwt token")
            throw it
        }
    }

    private fun loadPrivateKey(): PrivateKey {
        return runCatching {
            Security.addProvider(BouncyCastleProvider())
            val rsaPrivateKey: String = readPrivateKey()
            val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(rsaPrivateKey))
            KeyFactory.getInstance("RSA").generatePrivate(keySpec)
        }.getOrElse {
            logger.error("Failed to load private key", it)
            throw IllegalArgumentException(
                "Failed to load private key, " +
                    "the provided private key is not a valid PKCS8 key."
            )
        }
    }

    private fun createJwtToken(privateKey: PrivateKey): String {
        return runCatching {
            Jwts.builder()
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() + JWT_EXPIRATION_DURATION_IN_MILLISECONDS))
                .setIssuer(cachingService.get("githubAppId").toString())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact()
        }.getOrElse {
            logger.error("Failed to generate a JWT token", it)
            throw FailedToCreateJWTException("Failed to generate a JWT token")
        }
    }

    private fun verifyAndCacheJwtToken(jwt: String) {
        runCatching {
            gitHubEnterpriseService.verifyJwt(jwt)
            cachingService.set("jwtToken", jwt, JWT_EXPIRATION_DURATION_IN_MILLISECONDS)
            logger.info("JWT token generated and cached successfully")
        }.onFailure {
            logger.error("Failed to verify and cache JWT token", it)
            throw it
        }
    }

    fun generateAndCacheInstallationTokens(
        installations: List<Installation>,
        jwtToken: String
    ) {
        installations.forEach { installation ->
            val installationToken = gitHubClient.createInstallationToken(installation.id, "Bearer $jwtToken").token
            cachingService.set(
                "installationToken:${installation.id}",
                installationToken,
                INSTALLATION_JWT_EXPIRATION_DURATION_IN_MILLISECONDS
            )
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

    fun getInstallationToken(installationId: Int): String {
        var installationToken = cachingService.get("installationToken:$installationId")?.toString()
        if (installationToken == null) {
            refreshTokens()
            installationToken = cachingService.get("installationToken:$installationId")?.toString()
            require(installationToken != null) { "Installation token not found/ expired" }
        }
        return installationToken
    }
}
