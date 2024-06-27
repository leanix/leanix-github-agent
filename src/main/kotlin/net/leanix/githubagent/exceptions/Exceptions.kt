package net.leanix.githubagent.exceptions

class GithubEnterpriseConfigurationMissingException(properties: String) : RuntimeException(
    "Github Enterprise properties '$properties' are not set"
)
class GithubAppInsufficientPermissionsException(message: String) : RuntimeException(message)
class FailedToCreateJWTException(message: String) : RuntimeException(message)
class UnableToConnectToGithubEnterpriseException(message: String) : RuntimeException(message)
