package net.leanix.githubagent.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableAsync

@Profile("!test")
@EnableAsync
@Configuration
class AsyncConfig
