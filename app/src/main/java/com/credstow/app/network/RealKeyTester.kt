package com.credstow.app.network

import com.credstow.app.model.KeyStatus
import com.credstow.app.model.Provider

/**
 * [KeyTester] used in production: a real [OkHttpClient] hitting the providers'
 * own endpoints. Constructed once in the ServiceLocator; never logs the key.
 */
fun realKeyTester(client: okhttp3.OkHttpClient): KeyTester =
    OkHttpKeyTester(client, endpoints = ApiRequest.realEndpoints())

