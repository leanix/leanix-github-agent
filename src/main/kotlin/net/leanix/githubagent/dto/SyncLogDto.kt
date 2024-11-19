package net.leanix.githubagent.dto

import java.util.UUID

data class SyncLogDto(
    val runId: UUID?,
    val trigger: Trigger,
    val logLevel: LogLevel,
    val synchronizationProgress: SynchronizationProgress,
    val message: String
)

enum class Trigger {
    FULL_SCAN,
    WEB_HOOK
}

enum class LogLevel {
    OK,
    WARNING,
    INFO,
    ERROR
}

enum class SynchronizationProgress {
    PENDING,
    RUNNING,
    ABORTION_PENDING,
    ABORTED,
    FINISHED
}
