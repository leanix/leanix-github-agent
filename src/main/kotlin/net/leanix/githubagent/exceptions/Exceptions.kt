package net.leanix.githubagent.exceptions

class GitHubEnterpriseConfigurationMissingException(properties: String) : RuntimeException(
    "Github Enterprise properties '$properties' are not set"
)
class GitHubAppInsufficientPermissionsException(message: String) : RuntimeException(message)
class FailedToCreateJWTException(message: String) : RuntimeException(message)
class UnableToConnectToGitHubEnterpriseException(message: String) : RuntimeException(message)
class JwtTokenNotFound : RuntimeException("JWT token not found")
