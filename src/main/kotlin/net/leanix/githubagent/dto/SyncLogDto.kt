package net.leanix.githubagent.dto

import java.util.UUID

data class SyncLogDto(
    val runId: UUID?,
    val trigger: Trigger,
    val logLevel: LogLevel,
    val message: String
)

enum class Trigger {
    START_FULL_SYNC,
    FINISH_FULL_SYNC,
    GENERIC
}

enum class LogLevel {
    OK,
    WARNING,
    INFO,
    ERROR
}
