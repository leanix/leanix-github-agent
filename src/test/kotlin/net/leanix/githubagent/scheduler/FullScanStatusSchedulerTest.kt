package net.leanix.githubagent.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.leanix.githubagent.dto.FullScanStatus
import net.leanix.githubagent.services.CachingService
import net.leanix.githubagent.services.WebSocketService
import org.junit.jupiter.api.Test

class FullScanStatusSchedulerTest {

    private val webSocketService: WebSocketService = mockk(relaxed = true)
    private val cachingService: CachingService = mockk()
    private val scheduler = FullScanStatusScheduler(webSocketService, cachingService)

    @Test
    fun `should send IN_PROGRESS status when runId is present`() {
        every { cachingService.get("runId") } returns "someRunId"

        scheduler.sendFullScanStatus()

        verify { webSocketService.sendMessage("fullScan/status", FullScanStatus.IN_PROGRESS.name) }
    }

    @Test
    fun `should send FINISHED status when runId is null`() {
        every { cachingService.get("runId") } returns null

        scheduler.sendFullScanStatus()

        verify { webSocketService.sendMessage("fullScan/status", FullScanStatus.FINISHED.name) }
    }
}
