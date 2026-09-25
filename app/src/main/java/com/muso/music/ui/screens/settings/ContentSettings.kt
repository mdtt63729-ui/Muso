package com.muso.music.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zionhuang.innertube.utils.parseCookieString
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.AccountChannelHandleKey
import com.muso.music.constants.AccountEmailKey
import com.muso.music.constants.AccountNameKey
import com.muso.music.constants.ContentCountryKey
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.muso.music.constants.AppLanguageKey
import com.muso.music.constants.AppLanguageToName
import com.muso.music.constants.ContentLanguageKey
import com.muso.music.constants.CountryCodeToName
import com.muso.music.constants.EnableBetterLyricsKey
import com.muso.music.constants.EnableKugouKey
import com.muso.music.constants.EnableLrcLibKey
import com.muso.music.constants.EnablePaxsenixKey
import com.muso.music.constants.EnableSimpMusicKey
import com.muso.music.constants.EnableUnisonKey
import com.muso.music.constants.EnableYouLyPlusKey
import com.muso.music.constants.HideExplicitKey
import com.muso.music.constants.InnerTubeCookieKey
import com.muso.music.constants.LyricsProviderOrderKey
import com.muso.music.lyrics.LyricsProviderRegistry
import com.muso.music.constants.LanguageCodeToName
import com.muso.music.constants.ProxyEnabledKey
import com.muso.music.constants.ProxyTypeKey
import com.muso.music.constants.ProxyUrlKey
import com.muso.music.constants.SYSTEM_DEFAULT
import com.muso.music.ui.component.EditTextPreference
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.ListPreference
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.component.SwitchPreference
import com.muso.music.ui.utils.backToMain
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference
import java.net.Proxy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (contentLanguage, onContentLanguageChange) = rememberPreference(key = ContentLanguageKey, defaultValue = "system")
    val (appLanguage, onAppLanguageChange) = rememberPreference(AppLanguageKey, defaultValue = SYSTEM_DEFAULT)
    val (contentCountry, onContentCountryChange) = rememberPreference(key = ContentCountryKey, defaultValue = "system")
    val (hideExplicit, onHideExplicitChange) = rememberPreference(key = HideExplicitKey, defaultValue = false)
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrcLib, onEnableLrcLibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(key = EnableBetterLyricsKey, defaultValue = true)
    val defaultProviderOrder = remember { LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder()) }
    val (lyricsProviderOrder, onLyricsProviderOrderChange) = rememberPreference(key = LyricsProviderOrderKey, defaultValue = defaultProviderOrder)
    val (enableSimpMusic, onEnableSimpMusicChange) = rememberPreference(key = EnableSimpMusicKey, defaultValue = true)
    val (enableYouLyPlus, onEnableYouLyPlusChange) = rememberPreference(key = EnableYouLyPlusKey, defaultValue = true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(key = EnablePaxsenixKey, defaultValue = true)
    val (enableUnison, onEnableUnisonChange) = rememberPreference(key = EnableUnisonKey, defaultValue = true)

    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(key = ProxyEnabledKey, defaultValue = false)
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(key = ProxyTypeKey, defaultValue = Proxy.Type.HTTP)
    val (proxyUrl, onProxyUrlChange) = rememberPreference(key = ProxyUrlKey, defaultValue = "host:port")


    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        PreferenceEntry(
            title = { Text(if (isLoggedIn) accountName else stringResource(R.string.login)) },
            description = if (isLoggedIn) {
                accountEmail.takeIf { it.isNotEmpty() }
                    ?: accountChannelHandle.takeIf { it.isNotEmpty() }
            } else null,
            icon = { Icon(painterResource(R.drawable.person), null) },
            onClick = { navController.navigate("login") }
        )
        val activity = LocalContext.current as? Activity
        ListPreference(
            title = { Text(stringResource(R.string.app_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = appLanguage,
            values = listOf(SYSTEM_DEFAULT) + AppLanguageToName.keys.toList(),
            valueText = {
                AppLanguageToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = { language ->
                onAppLanguageChange(language)
                // Recreate so every stringResource re-resolves with the new locale.
                activity?.recreate()
            },
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_language)) },
            icon = { Icon(painterResource(R.drawable.language), null) },
            selectedValue = contentLanguage,
            values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
            valueText = {
                LanguageCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentLanguageChange
        )
        ListPreference(
            title = { Text(stringResource(R.string.content_country)) },
            icon = { Icon(painterResource(R.drawable.location_on), null) },
            selectedValue = contentCountry,
            values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
            valueText = {
                CountryCodeToName.getOrElse(it) {
                    stringResource(R.string.system_default)
                }
            },
            onValueSelected = onContentCountryChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_explicit)) },
            icon = { Icon(painterResource(R.drawable.explicit), null) },
            checked = hideExplicit,
            onCheckedChange = onHideExplicitChange
        )

        ListPreference(
            title = { Text(stringResource(R.string.preferred_lyrics_provider)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            selectedValue = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder).firstOrNull() ?: "",
            values = LyricsProviderRegistry.providerNames,
            valueText = { LyricsProviderRegistry.getDisplayName(it) },
            onValueSelected = { providerName ->
                val order = listOf(providerName) + LyricsProviderRegistry.getDefaultProviderOrder().filter { it != providerName }
                onLyricsProviderOrderChange(LyricsProviderRegistry.serializeProviderOrder(order))
            }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_lrclib)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableLrcLib,
            onCheckedChange = onEnableLrcLibChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_kugou)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableKugou,
            onCheckedChange = onEnableKugouChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_youlyplus)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableYouLyPlus,
            onCheckedChange = onEnableYouLyPlusChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_paxsenix)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enablePaxsenix,
            onCheckedChange = onEnablePaxsenixChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_unison)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableUnison,
            onCheckedChange = onEnableUnisonChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_better_lyrics)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableBetterLyrics,
            onCheckedChange = onEnableBetterLyricsChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_simpmusic)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = enableSimpMusic,
            onCheckedChange = onEnableSimpMusicChange
        )

        PreferenceGroupTitle(
            title = "PROXY"
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_proxy)) },
            icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
            checked = proxyEnabled,
            onCheckedChange = onProxyEnabledChange
        )

        AnimatedVisibility(proxyEnabled) {
            Column {
                ListPreference(
                    title = { Text(stringResource(R.string.proxy_type)) },
                    selectedValue = proxyType,
                    values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                    valueText = { it.name },
                    onValueSelected = onProxyTypeChange
                )
                EditTextPreference(
                    title = { Text(stringResource(R.string.proxy_url)) },
                    value = proxyUrl,
                    onValueChange = onProxyUrlChange
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.content)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
