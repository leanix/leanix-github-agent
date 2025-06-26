package net.leanix.githubagent.exceptions

import com.expediagroup.graphql.client.types.GraphQLClientError

class GitHubEnterpriseConfigurationMissingException(properties: String) : RuntimeException(
    "Github Enterprise properties '$properties' are not set"
)
class GitHubAppInsufficientPermissionsException(message: String) : RuntimeException(message)
class FailedToCreateJWTException(message: String) : RuntimeException(message)
class UnableToConnectToGitHubEnterpriseException(message: String) : RuntimeException(message)
class JwtTokenNotFound : RuntimeException("JWT token not found")
class GraphQLApiException(errors: List<GraphQLClientError>) :
    RuntimeException("Errors: ${errors.joinToString(separator = "\n") { it.message }}")
class WebhookSecretNotSetException : RuntimeException("Webhook secret not set")
class InvalidEventSignatureException : RuntimeException("Invalid event signature")
class ManifestFileNotFoundException : RuntimeException("Manifest File Not Found")
class UnableToSendMessageException(exception: Exception) :
    RuntimeException(
        "Unable to send message to the backend",
        exception
    )
