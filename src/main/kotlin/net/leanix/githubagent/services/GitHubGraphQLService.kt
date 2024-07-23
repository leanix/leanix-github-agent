package net.leanix.githubagent.services

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import kotlinx.coroutines.runBlocking
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.PagedRepositories
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.dto.RepositoryOrganizationDto
import net.leanix.githubagent.exceptions.GraphQLApiException
import net.leanix.githubbroker.connector.adapter.graphql.data.GetRepositories
import net.leanix.githubbroker.connector.adapter.graphql.data.getrepositories.Blob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class GitHubGraphQLService(
    private val cachingService: CachingService,
    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitHubGraphQLService::class.java)
        private const val PAGE_COUNT = 20
        private const val MANIFEST_FILE_NAME = "leanix.yaml"
    }

    fun getRepositories(
        token: String,
        cursor: String? = null
    ): PagedRepositories {
        val client = buildGitHubGraphQLClient(token)

        val query = GetRepositories(
            GetRepositories.Variables(
                pageCount = PAGE_COUNT,
                cursor = cursor,
                expression = "HEAD:${gitHubEnterpriseProperties.manifestFileDirectory}$MANIFEST_FILE_NAME"
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
                        description = it.description,
                        url = it.url,
                        organization = RepositoryOrganizationDto(
                            id = it.owner.id,
                            name = it.owner.login
                        ),
                        languages = it.languages!!.nodes!!.map { language -> language!!.name },
                        topics = it.repositoryTopics.nodes!!.map { topic -> topic!!.topic.name },
                        manifest = (it.`object` as Blob?)?.text
                    )
                }
            )
        }
    }

    private fun buildGitHubGraphQLClient(
        token: String
    ) =
        GraphQLWebClient(
            url = "${cachingService.get("baseUrl")}/api/graphql",
            builder = WebClient.builder().defaultHeaders { it.setBearerAuth(token) }
        )
}
