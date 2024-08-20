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
import net.leanix.githubagent.shared.ManifestFileName
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
                "HEAD:${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YAML.fileName}",
                "HEAD:${gitHubEnterpriseProperties.manifestFileDirectory}${ManifestFileName.YML.fileName}"
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
                        archived = it.isArchived,
                        visibility = it.visibility,
                        updatedAt = it.updatedAt,
                        languages = it.languages!!.nodes!!.map { language -> language!!.name },
                        topics = it.repositoryTopics.nodes!!.map { topic -> topic!!.topic.name },
                        manifestFileContent = if (it.manifestYaml != null) {
                            (it.manifestYaml as Blob).text.toString()
                        } else if (it.manifestYml != null) {
                            (it.manifestYml as Blob).text.toString()
                        } else {
                            null
                        }
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

        return (
            result.data?.repository?.manifestFile as?
                net.leanix.githubagent.graphql.`data`.getrepositorymanifestcontent.Blob
            )?.text
    }

    private fun buildGitHubGraphQLClient(
        token: String
    ) =
        GraphQLWebClient(
            url = "${cachingService.get("baseUrl")}/api/graphql",
            builder = WebClient.builder().defaultHeaders { it.setBearerAuth(token) }
        )
}
