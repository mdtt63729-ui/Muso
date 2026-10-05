package com.muso.music.ui.screens.settings

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.SpotifyClientIDKey
import com.muso.music.constants.SpotifyDisplayNameKey
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.utils.backToMain
import com.muso.music.utils.SpotifyClient
import com.muso.music.utils.dataStore
import androidx.datastore.preferences.core.edit
import com.muso.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Spotify OAuth page in an in-app WebView (same approach as the Discord login).
 * The redirect to our non-routable callback URI is intercepted before the system ever
 * sees it, the PKCE code is exchanged for tokens off the main thread, and the screen
 * closes itself when the session is stored. A failed exchange pops the user back
 * without crashing.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyLoginScreen(
    navController: NavController,
) {
    val scope = rememberCoroutineScope()
    val (clientId, _) = rememberPreference(SpotifyClientIDKey, "")

    var webView: WebView? = null

    AndroidView(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        webView: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val url = request.url
                        // Spotify redirects to our callback with ?code=... (or ?error=...).
                        if (url.host == "localhost" && url.path == "/muso_callback") {
                            val code = url.getQueryParameter("code")
                            if (code != null) {
                                scope.launch {
                                    val name = SpotifyClient.completeLogin(context, code)
                                    if (!name.isNullOrEmpty()) {
                                        context.dataStore.edit {
                                            it[SpotifyDisplayNameKey] = name
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        navController.navigateUp()
                                    }
                                }
                            } else {
                                // denied / error - just go back to the settings screen
                                scope.launch(Dispatchers.Main) {
                                    navController.navigateUp()
                                }
                            }
                            return true
                        }
                        return false
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }
                WebStorage.getInstance().deleteAllData()

                webView = this
                if (clientId.isNotEmpty()) {
                    loadUrl(SpotifyClient.authorizeUrl(clientId))
                }
            }
        },
    )

    TopAppBar(
        title = { Text(stringResource(R.string.log_in_to_spotify)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
