package com.keyvault.app.network

import com.keyvault.app.model.KeyStatus
import com.keyvault.app.model.Provider

/**
 * [KeyTester] used in production: a real [OkHttpClient] hitting the providers'
 * own endpoints. Constructed once in the ServiceLocator; never logs the key.
 */
fun realKeyTester(client: okhttp3.OkHttpClient): KeyTester =
    OkHttpKeyTester(client, endpoints = ApiRequest.realEndpoints())
