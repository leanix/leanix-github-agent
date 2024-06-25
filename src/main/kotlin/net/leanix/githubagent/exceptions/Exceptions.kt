package net.leanix.githubagent.exceptions

class GithubEnterpriseConfigurationMissingException(properties: String) : RuntimeException(
    "Github Enterprise properties '$properties' are not set"
)
class AuthenticationFailedException(message: String) : RuntimeException(message)
class ConnectingToGithubEnterpriseFailedException(message: String) : RuntimeException(message)
