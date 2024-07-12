package net.leanix.githubagent.config

// class WebSocketClientConfigTests {
//
//    private lateinit var webSocketClientConfig: WebSocketClientConfig
//    private val authService: AuthService = mockk()
//    private val stompClient: WebSocketStompClient = mockk(relaxed = true)
//    private val stompSession: StompSession = mockk(relaxed = true)
//    private val brokerStompSessionHandler: BrokerStompSessionHandler = mockk(relaxed = true)
//    private val objectMapper: ObjectMapper = mockk(relaxed = true)
//    private val leanIXProperties: LeanIXProperties = mockk(relaxed = true)
//    private val gitHubEnterpriseProperties: GitHubEnterpriseProperties = mockk(relaxed = true)
//
//    @BeforeEach
//    fun setUp() {
//        webSocketClientConfig = WebSocketClientConfig(
//            brokerStompSessionHandler,
//            objectMapper,
//            authService,
//            leanIXProperties,
//            gitHubEnterpriseProperties
//        )
//        webSocketClientConfig.stompClient = stompClient
//    }
//
//    @Test
//    fun `initSession should establish connection with valid headers`() = runBlocking {
//        coEvery { authService.getBearerToken() } returns "validToken"
//        coEvery { leanIXProperties.wsBaseUrl } returns "ws://test.url"
//        coEvery { gitHubEnterpriseProperties.baseUrl } returns "http://github.enterprise.url"
//        coEvery { gitHubEnterpriseProperties.gitHubAppId } returns "appId"
//        coEvery { stompClient.connect(any(), any(), any(), any()) } returns stompSession
//
//        val session = webSocketClientConfig.initSession()
//
//        assertEquals(stompSession, session)
//    }
//
//    @Test
//    fun `initSession should handle connection failure gracefully`() = runBlocking {
//        coEvery { authService.getBearerToken() } returns "validToken"
//        coEvery { leanIXProperties.wsBaseUrl } returns "ws://test.url"
//        coEvery { gitHubEnterpriseProperties.baseUrl } returns "http://github.enterprise.url"
//        coEvery { gitHubEnterpriseProperties.gitHubAppId } returns "appId"
//        coEvery { stompClient.connect(any(), any(), any(), any()) } throws RuntimeException("Connection failed")
//
//        val session = runCatching { webSocketClientConfig.initSession() }.getOrNull()
//
//        assertEquals(null, session)
//    }
// }
