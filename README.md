[![REUSE status](https://api.reuse.software/badge/github.com/leanix/leanix-github-agent)](https://api.reuse.software/info/github.com/leanix/leanix-github-agent)

# leanix-github-agent

## About this project

SAP LeanIX agent to discover self built software in self-hosted GitHub Enterprise setups and communicate back to the SAP LeanIX workspace.

## Requirements and Setup

### Requirements

- Docker: The agent is packaged as a Docker image and requires Docker to run.
- GitHub Enterprise Server: The agent is designed to interact with GitHub Enterprise Server. You need access to a GitHub Enterprise Server instance.
- GitHub App: The agent operates as a GitHub App. You need to create a GitHub App in your GitHub Enterprise Server instance.

### Setup

1. **Create a GitHub App**: Follow the instructions provided by GitHub to create a new GitHub App in your GitHub Enterprise Server instance.

2. **Generate a Private Key**: In your GitHub App settings, generate a private key. This will download a PEM file, which the agent will use to authenticate to the GitHub Enterprise environment.

3. **Install the App**: Install the app on all organizations you want the agent to have access to.

4. **Configure the Agent**: The agent requires several environment variables to run. These could be passed to the Docker command when starting the agent. The required variables are:

    - `GITHUB_ENTERPRISE_BASE_URL`: The base URL of your GitHub Enterprise Server instance.
    - `GITHUB_APP_ID`: The ID of your GitHub App.
    - `PEM_FILE`: The path to your GitHub App's PEM file inside the Docker container.

5. **Start the Agent**: Run the Docker command to start the agent. Replace `<variable>` with your actual values:

    ```bash
    docker run -p 8000:8080 \
    -v $(pwd)/path/to/your/privateKey.pem:/privateKey.pem \
    -e GITHUB_ENTERPRISE_BASE_URL=<github_enterprise_base_url> \
    -e GITHUB_APP_ID=<github_app_id> \
    -e PEM_FILE=/privateKey.pem \
    trc-github-enterprise-broker
    ```

   This command starts the agent and exposes it on port 8000. The agent will start scanning your for organisations and repositories.

**Disclaimer**: The Docker image for the agent is not yet available. It will be available for pulling once a new version is published. Please check the [releases](https://github.com/leanix/leanix-github-agent/releases) page for updates.


## Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports etc. via [GitHub issues](https://github.com/leanix/leanix-github-agent/issues). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, as well as additional contribution information, see our [Contribution Guidelines](CONTRIBUTING.md).

## Security / Disclosure
If you find any bug that may be a security problem, please follow our instructions at [in our security policy](https://github.com/leanix/leanix-github-agent/security/policy) on how to report it. Please do not create GitHub issues for security-related doubts or problems.

## Code of Conduct

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to abide by its [Code of Conduct](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md) at all times.

## Licensing

Copyright 2024 SAP SE or an SAP affiliate company and leanix-github-agent contributors. Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/leanix/leanix-github-agent).
