package net.leanix.githubagent.services

import net.leanix.githubagent.exceptions.UnableToSendMessageException
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class FullScanService(
    private val gitHubScanningService: GitHubScanningService,
    private val syncLogService: SyncLogService,
    private val gitHubAuthenticationService: GitHubAuthenticationService
) {

    companion object {
        var requireScan: Boolean = false
    }

    @Async
    fun verifyAndStartScan() {
        if (requireScan) {
            runCatching {
                requireScan = false
                gitHubAuthenticationService.generateAndCacheJwtToken()
                syncLogService.sendFullScanStart(null)
                gitHubScanningService.scanGitHubResources()
            }.onSuccess {
                syncLogService.sendFullScanSuccess()
            }.onFailure {
                if (it is UnableToSendMessageException) requireScan = true
                syncLogService.sendFullScanFailure(it.message)
            }
        }
    }
}
