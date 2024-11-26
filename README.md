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
    - `WEBHOOK_SECRET`: The secret used to validate incoming webhook events from GitHub. (Optional, but recommended. [Needs to be set in the GitHub App settings first](https://docs.github.com/en/enterprise-server@3.8/webhooks/using-webhooks/validating-webhook-deliveries).)
    - `JAVA_OPTS`: Java options for the agent. Use this to set proxy settings if required.

5. **Start the agent**: To start the agent, run the following Docker command. Replace the variables in angle brackets with your actual values.

    ```bash
    docker run -p 8000:8080 \
    -v $(pwd)/path/to/your/privateKey.pem:/privateKey.pem \
    -e GITHUB_ENTERPRISE_BASE_URL=<github_enterprise_base_url> \
    -e GITHUB_APP_ID=<github_app_id> \
    -e PEM_FILE=/privateKey.pem \
    -e WEBHOOK_SECRET=<webhook_secret> \
    ghcr.io/leanix/leanix-github-agent:dev
    ```

   This command starts the agent and exposes it on port 8000. The agent starts scanning your organizations and repositories.


6. The container hosts a live service that runs continuously.
   - It provides a health endpoint at `/actuator/health`, which can be used to monitor the service's health.

**Note**: The Docker image for the agent is currently unavailable. It will become available for download once a new version is released. Please check the [Releases](https://github.com/leanix/leanix-github-agent/releases) page for updates.

### Troubleshooting

#### Using over a http proxy system

Add the following properties on the command:

```console
docker run 
           ...
           -e JAVA_OPTS="-Dhttp.proxyHost=<HTTP_HOST> -Dhttp.proxyPort=<HTTP_PORT> -Dhttps.proxyHost=<HTTPS_HOST> -Dhttps.proxyPort=<HTTPS_PORT>" \
        ghcr.io/leanix/leanix-github-agent:dev
```
> Note: Basic authentication is not currently supported.

#### Using over SSL Intercepting proxy

Build your own docker image adding the certificate:

```console
FROM ghcr.io/leanix/leanix-github-agent:dev


USER root

RUN apk update && apk add ca-certificates && rm -rf /var/cache/apk/*
COPY YOUR-CERTIFICATE-HERE /usr/local/share/ca-certificates/YOUR-CERTIFICATE-HERE
RUN update-ca-certificates 
RUN keytool -import -trustcacerts -keystore $JAVA_HOME/lib/security/cacerts  -storepass changeit -noprompt -alias YOUR-CERTIFICATE-HERE -file /usr/local/share/ca-certificates/YOUR-CERTIFICATE-HERE

```

> Note: You should add an additional COPY and the final RUN for each certificate you need to insert into the image.

#### Using amd64 images on Apple M1

Just run the container by providing the following command:

```console

docker run --platform linux/amd64 \
           ...
        ghcr.io/leanix/leanix-github-agent:dev
```

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
