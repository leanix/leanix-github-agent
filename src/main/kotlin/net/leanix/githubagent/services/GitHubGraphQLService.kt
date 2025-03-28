package net.leanix.githubagent.services

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import kotlinx.coroutines.runBlocking
import net.leanix.githubagent.dto.PagedRepositories
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.exceptions.GraphQLApiException
import net.leanix.githubagent.graphql.data.GetRepositories
import net.leanix.githubagent.graphql.data.GetRepository
import net.leanix.githubagent.graphql.data.GetRepositoryManifestContent
import net.leanix.githubagent.interceptor.RateLimitInterceptor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.util.retry.Retry
import java.time.Duration
import io.github.resilience4j.retry.annotation.Retry as ResilienceRetry

@Component
class GitHubGraphQLService(
    private val cachingService: CachingService,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitHubGraphQLService::class.java)
        private const val PAGE_COUNT = 20
    }

    @ResilienceRetry(name = "secondary_rate_limit")
    fun getRepositories(
        token: String,
        cursor: String? = null
    ): PagedRepositories {
        val client = buildGitHubGraphQLClient(token)

        val query = GetRepositories(
            GetRepositories.Variables(
                pageCount = PAGE_COUNT,
                cursor = cursor,
            )
        )

        val result = runBlocking {
            client.execute(query)
        }

        return if (result.errors != null && result.errors!!.isNotEmpty()) {
            logger.error("Error getting repositories: ${result.errors}")
            throw GraphQLApiException(result.errors!!)
        } else {
            PagedRepositories(
                hasNextPage = result.data!!.viewer.repositories.pageInfo.hasNextPage,
                cursor = result.data!!.viewer.repositories.pageInfo.endCursor,
                repositories = result.data!!.viewer.repositories.nodes!!.map {
                    RepositoryDto(
                        id = it!!.id,
                        name = it.name,
                        organizationName = it.owner.login,
                        description = it.description,
                        url = it.url,
                        defaultBranch = it.defaultBranchRef?.name,
                        archived = it.isArchived,
                        visibility = it.visibility,
                        updatedAt = it.updatedAt,
                        languages = it.languages!!.nodes!!.map { language -> language!!.name },
                        topics = it.repositoryTopics.nodes!!.map { topic -> topic!!.topic.name },
                    )
                }
            )
        }
    }

    @ResilienceRetry(name = "secondary_rate_limit")
    fun getManifestFileContent(
        owner: String,
        repositoryName: String,
        filePath: String,
        token: String
    ): String? {
        val client = buildGitHubGraphQLClient(token)

        val query = GetRepositoryManifestContent(
            GetRepositoryManifestContent.Variables(
                owner = owner,
                repositoryName = repositoryName,
                manifestFilePath = "HEAD:$filePath"
            )
        )

        val result = runBlocking {
            client.execute(query)
        }

        if (!result.errors.isNullOrEmpty()) {
            logger.error("Error getting file content: ${result.errors}")
            throw GraphQLApiException(result.errors!!)
        }

        return (result.data?.repository?.manifestFile as? RepositoryManifestBlob)?.text
    }

    @ResilienceRetry(name = "secondary_rate_limit")
    fun getRepository(
        owner: String,
        repositoryName: String,
        token: String
    ): RepositoryDto? {
        val client = buildGitHubGraphQLClient(token)

        val query = GetRepository(
            GetRepository.Variables(
                owner = owner,
                repositoryName = repositoryName
            )
        )

        val result = runBlocking {
            client.execute(query)
        }

        val repository = result.data?.repositoryOwner?.repository

        if (repository == null) {
            logger.error("Error getting repository: ${result.errors}")
            throw GraphQLApiException(result.errors!!)
        }

        return RepositoryDto(
            id = repository.id,
            name = repository.name,
            organizationName = repository.owner.login,
            description = repository.description,
            url = repository.url,
            defaultBranch = repository.defaultBranchRef?.name,
            archived = repository.isArchived,
            visibility = repository.visibility,
            updatedAt = repository.updatedAt,
            languages = repository.languages!!.nodes!!.map { language -> language!!.name },
            topics = repository.repositoryTopics.nodes!!.map { topic -> topic!!.topic.name },
        )
    }

    private fun buildGitHubGraphQLClient(
        token: String
    ) =
        GraphQLWebClient(
            url = "${cachingService.get("baseUrl")}/api/graphql",
            builder = WebClient.builder().defaultHeaders { it.setBearerAuth(token) }
                .filter(RateLimitInterceptor())
                .filter { request, next ->
                    next.exchange(request)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                }
        )
}

typealias RepositoryManifestBlob = net.leanix.githubagent.graphql.`data`.getrepositorymanifestcontent.Blob
