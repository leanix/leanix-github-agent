[![REUSE status](https://api.reuse.software/badge/github.com/leanix/leanix-github-agent)](https://api.reuse.software/info/github.com/leanix/leanix-github-agent)

# leanix-github-agent

## About This Project

The SAP LeanIX agent discovers self-built software in self-hosted GitHub Enterprise setups and communicates this information to an SAP LeanIX workspace.

## Prerequisites and Installation

### Prerequisites

- **Docker**: The agent is distributed as a Docker image. Docker is required to run it.
- **GitHub Enterprise Server**: The agent interacts with GitHub Enterprise Server. You need access to a GitHub Enterprise Server instance.
- **GitHub App**: The agent operates as a GitHub App. You need to create a GitHub App in your GitHub Enterprise Server instance.

### Installation

1. **Create a GitHub App**: Create a new GitHub App in your GitHub Enterprise Server instance. For details, refer to the [GitHub documentation](https://docs.github.com/en/apps/creating-github-apps/about-creating-github-apps/about-creating-github-apps).

2. **Generate a private key**: In your GitHub App settings, generate a private key. For instructions, refer to the [GitHub documentation](https://docs.github.com/en/enterprise-cloud@latest/apps/creating-github-apps/authenticating-with-a-github-app/managing-private-keys-for-github-apps). The agent will use the downloaded PEM file to authenticate with the GitHub Enterprise environment.

3. **Install the GitHub App**: Install the app on all organizations that the agent should access. For instructions, refer to the [GitHub documentation](https://docs.github.com/en/apps/using-github-apps/installing-your-own-github-app).

4. **Configure the agent**: The agent requires the following environment variables to run. Pass them to the Docker command when starting the agent.

    - `GITHUB_ENTERPRISE_BASE_URL`: The base URL of your GitHub Enterprise Server instance.
    - `GITHUB_APP_ID`: The ID of your GitHub App.
    - `PEM_FILE`: The path to your GitHub App's PEM file inside the Docker container.
    - `MANIFEST_FILE_DIRECTORY`: The directory path where the manifest files are stored in each repository. Manifest files are crucial for microservice discovery as they provide essential information about the service. For more information, see [Microservice Discovery Through a Manifest File](https://docs-eam.leanix.net/reference/microservice-discovery-manifest-file) in our documentation. **If both .yaml and .yml files exist in a repository, only the .yaml file will be used.**
    - `WEBHOOK_SECRET`: The secret used to validate incoming webhook events from GitHub. (Optional, but recommended. [Needs to be set in the GitHub App settings first](https://docs.github.com/en/enterprise-server@3.8/webhooks/using-webhooks/validating-webhook-deliveries).)

5. **Start the agent**: To start the agent, run the following Docker command. Replace the variables in angle brackets with your actual values.

    ```bash
    docker run -p 8000:8080 \
    -v $(pwd)/path/to/your/privateKey.pem:/privateKey.pem \
    -e GITHUB_ENTERPRISE_BASE_URL=<github_enterprise_base_url> \
    -e GITHUB_APP_ID=<github_app_id> \
    -e PEM_FILE=/privateKey.pem \
    -e MANIFEST_FILE_DIRECTORY=<manifest_file_directory> \
    -e WEBHOOK_SECRET=<webhook_secret> \
    leanix-github-agent
    ```

   This command starts the agent and exposes it on port 8000. The agent starts scanning your organizations and repositories.

**Note**: The Docker image for the agent is currently unavailable. It will become available for download once a new version is released. Please check the [Releases](https://github.com/leanix/leanix-github-agent/releases) page for updates.

## Support and Feedback

We welcome your feedback, feature suggestions, and bug reports via [GitHub issues](https://github.com/leanix/leanix-github-agent/issues).

## Contributing

We encourage contributions to this project. For details on how to contribute, the project structure, and other related information, refer to [Contributing](CONTRIBUTING.md).

## Reporting Security Issues

If you discover a potential security issue, follow our [Security Policy](https://github.com/leanix/leanix-github-agent/security/policy) for reporting. Please do not create GitHub issues for security-related matters.

## Code of Conduct

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to abide by its [Code of Conduct](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md) at all times.

## Licensing

Copyright 2024 SAP SE or an SAP affiliate company and leanix-github-agent contributors. Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/leanix/leanix-github-agent).
