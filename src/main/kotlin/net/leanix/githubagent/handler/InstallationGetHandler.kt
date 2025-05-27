package net.leanix.githubagent.handler

import net.leanix.githubagent.client.GitHubClient
import net.leanix.githubagent.dto.InstallationRequestDTO
import net.leanix.githubagent.exceptions.JwtTokenNotFound
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.GitHubAuthenticationService
import net.leanix.githubagent.services.GitHubEnterpriseService
import net.leanix.githubagent.services.GitHubScanningService
import net.leanix.githubagent.services.SyncLogService
import net.leanix.githubagent.shared.INSTALLATION_LABEL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import java.lang.reflect.Type

@Component
class InstallationGetHandler(
    @Lazy @Autowired
    private val gitHubAuthenticationService: GitHubAuthenticationService,
    @Lazy @Autowired
    private val gitHubScanningService: GitHubScanningService,
    @Lazy @Autowired
    private val gitHubEnterpriseService: GitHubEnterpriseService,
    @Lazy @Autowired
    private val cachingService: CachingService,
    @Lazy @Autowired
    private val syncLogService: SyncLogService,
    @Lazy @Autowired
    private val gitHubClient: GitHubClient,
    @Value("\${webhookEventService.waitingTime}") private val waitingTime: Long,
) : StompFrameHandler {

    private val logger = LoggerFactory.getLogger(InstallationGetHandler::class.java)

    override fun getPayloadType(headers: StompHeaders): Type {
        return InstallationRequestDTO::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        payload?.let {
            val dto = payload as InstallationRequestDTO
            logger.info("Received installation get message from server for organisation: ${dto.account.login}")
            runCatching {
                fetchAndSendOrganisationData(dto)
            }
        }
    }

    fun fetchAndSendOrganisationData(installationRequestDTO: InstallationRequestDTO) {
        while (cachingService.get("runId") != null) {
            logger.info("A full scan is already in progress, waiting for it to finish.")
            Thread.sleep(waitingTime)
        }
        syncLogService.sendFullScanStart(installationRequestDTO.account.login)
        kotlin.runCatching {
            val jwtToken = cachingService.get("jwtToken") ?: throw JwtTokenNotFound()
            val installation = gitHubClient.getInstallation(
                installationRequestDTO.id,
                "Bearer $jwtToken"
            )
            gitHubEnterpriseService.validateEnabledPermissionsAndEvents(
                INSTALLATION_LABEL,
                installation.permissions,
                installation.events
            )
            gitHubAuthenticationService.refreshTokens()
            gitHubScanningService.fetchAndSendOrganisationsData(listOf(installation))
            gitHubScanningService.fetchAndSendRepositoriesData(installation).forEach { repository ->
                gitHubScanningService.fetchManifestFilesAndSend(installation, repository)
            }
        }.onSuccess {
            syncLogService.sendFullScanSuccess()
        }.onFailure {
            syncLogService.sendFullScanFailure(it.message)
        }
    }
}
