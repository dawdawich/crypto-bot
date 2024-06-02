package space.dawdawich.constants

/**
 * WebSocket URL for connecting to the Bybit streaming API.
 * The URL is used in the `BybitTickerWebSocketClient` class to establish a WebSocket connection.
 */
const val BYBIT_WEB_SOCKET_URL = "wss://stream.bybit.com/v5/public/linear"

/**
 * The base URL for the ByBit server.
 *
 * This constant stores the server URL for the ByBit API. It is used for making API requests to the ByBit server.
 */
const val BYBIT_SERVER_URL = "https://api.bybit.com/v5"

/**
 * BYBIT_TEST_WEB_SOCKET_URL is a constant variable that represents the WebSocket URL for the Bybit testnet.
 * It is used for establishing a WebSocket connection to the Bybit testnet server.
 */
const val BYBIT_TEST_WEB_SOCKET_URL = "wss://stream-testnet.bybit.com/v5/public/linear"

/**
 * The constant variable `BYBIT_TEST_SERVER_URL` holds the URL for the ByBit test server.
 * It is used in various classes and functions to make HTTP requests to the ByBit API for test purposes.
 */
const val BYBIT_TEST_SERVER_URL = "https://api-testnet.bybit.com/v5"
