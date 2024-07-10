package net.leanix.githubagent.shared

import jakarta.annotation.PostConstruct
import net.leanix.githubagent.shared.GitHubAgentProperties.GITHUB_AGENT_VERSION
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class GradlePropertiesConfiguration {

    private val logger: Logger = LoggerFactory.getLogger(GradlePropertiesConfiguration::class.java)

    @PostConstruct
    fun loadVersion() {
        try {
            val gradleProperties = Properties()
            gradleProperties.load(this::class.java.getResourceAsStream("/gradle.properties"))
            GITHUB_AGENT_VERSION = gradleProperties.getProperty("version")
            logger.info("Running GitHub agent on version: $GITHUB_AGENT_VERSION")
        } catch (e: RuntimeException) {
            GITHUB_AGENT_VERSION = "unknown"
            logger.error("Unable to load GitHub agent version")
        }
    }
}