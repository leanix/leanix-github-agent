package net.leanix.githubagent.services

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import kotlinx.coroutines.runBlocking
import net.leanix.githubagent.config.GitHubEnterpriseProperties
import net.leanix.githubagent.dto.PagedRepositories
import net.leanix.githubagent.dto.RepositoryDto
import net.leanix.githubagent.exceptions.GraphQLApiException
import net.leanix.githubagent.graphql.data.GetRepositories
import net.leanix.githubagent.graphql.data.GetRepositoryManifestContent
import net.leanix.githubagent.graphql.data.getrepositories.Blob
import net.leanix.githubagent.shared.MANIFEST_FILE_NAME
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
                        organizationName = it.owner.login,
                        description = it.description,
                        url = it.url,
                        isArchived = it.isArchived,
                        visibility = it.visibility,
                        updatedAt = it.updatedAt,
                        languages = it.languages!!.nodes!!.map { language -> language!!.name },
                        topics = it.repositoryTopics.nodes!!.map { topic -> topic!!.topic.name },
                        manifestFileContent = (it.`object` as Blob?)?.text
                    )
                }
            )
        }
    }

    fun getManifestFileContent(
        owner: String,
        repositoryName: String,
        filePath: String,
        token: String
    ): String {
        val client = buildGitHubGraphQLClient(token)

        val query = GetRepositoryManifestContent(
            GetRepositoryManifestContent.Variables(
                owner = owner,
                repositoryName = repositoryName,
                filePath = filePath
            )
        )

        val result = runBlocking {
            client.execute(query)
        }

        return if (result.errors != null && result.errors!!.isNotEmpty()) {
            logger.error("Error getting file content: ${result.errors}")
            throw GraphQLApiException(result.errors!!)
        } else {
            (
                result.data!!.repository!!.`object`
                    as net.leanix.githubagent.graphql.`data`.getrepositorymanifestcontent.Blob
                ).text.toString()
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
