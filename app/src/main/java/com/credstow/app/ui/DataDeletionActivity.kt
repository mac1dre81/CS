package com.credstow.app.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.credstow.app.R
import com.credstow.app.ui.theme.CredStowTheme

/**
 * Displays the bundled data-deletion instructions (from
 * [assets/data_deletion.html]) in an off-line, self-contained WebView.
 * No external URL is contacted.
 */
class DataDeletionActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CredStowTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                ) {
                    Scaffold(
                        topBar = { DataDeletionTopBar(onBack = { finish() }) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) { innerPadding ->
                        DeletionWebView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataDeletionTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.settings_data_deletion)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DeletionWebView(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    defaultFontSize = 16
                    loadWithOverviewMode = true
                    useWideViewPort = false
                }
                setBackgroundColor(android.graphics.Color.WHITE)
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, assetHtml(ctx), "text/html; charset=utf-8", "utf-8", null)
            }
        },
    )
}

/** Reads the bundled HTML asset into a string for in-memory loading. */
private fun assetHtml(ctx: android.content.Context): String =
    ctx.assets.open("data_deletion.html").bufferedReader().use { it.readText() }
