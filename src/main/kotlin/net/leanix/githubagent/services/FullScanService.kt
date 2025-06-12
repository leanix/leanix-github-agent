package net.leanix.githubagent.services

import net.leanix.githubagent.exceptions.UnableToSendMessageException
import org.springframework.stereotype.Service

@Service
class FullScanService(
    private val gitHubScanningService: GitHubScanningService,
    private val syncLogService: SyncLogService
) {

    companion object {
        var requireScan: Boolean = false
    }

    fun verifyAndStartScan() {
        if (requireScan) {
            runCatching {
                requireScan = false
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
