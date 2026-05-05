package net.leanix.githubagent.shared

import jakarta.annotation.PostConstruct
import net.leanix.githubagent.shared.GitHubAgentProperties.githubAgentVersion
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
            githubAgentVersion = gradleProperties.getProperty("version")
            logger.info("Running GitHub agent on version: $githubAgentVersion")
        } catch (e: RuntimeException) {
            githubAgentVersion = "unknown"
            logger.error("Unable to load GitHub agent version")
        }
    }
}
