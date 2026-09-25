package com.muso.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.R
import com.muso.music.constants.AIApiKeyKey
import com.muso.music.constants.AICustomBaseURLKey
import com.muso.music.constants.AICustomModelKey
import com.muso.music.constants.AIProvider
import com.muso.music.constants.AIProviderKey
import com.muso.music.constants.AITargetLanguageKey
import com.muso.music.constants.UseAITranslationKey
import com.muso.music.ui.component.DefaultDialog
import com.muso.music.ui.component.EnumListPreference
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.ListPreference
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.component.SwitchPreference
import com.muso.music.ui.utils.backToMain
import com.muso.music.utils.AITranslator
import com.muso.music.utils.rememberEnumPreference
import com.muso.music.utils.rememberPreference

/**
 * SimpMusic's AI settings, as its own category: the user's own API key, provider and
 * model drive a real lyrics translator. The "Use AI translation" switch stays disabled
 * until a key is set, so the toggle can never be turned on into a broken state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (aiProvider, onAIProviderChange) = rememberEnumPreference(key = AIProviderKey, defaultValue = AIProvider.OPENAI)
    val (apiKey, onApiKeyChange) = rememberPreference(key = AIApiKeyKey, defaultValue = "")
    val (customModel, onCustomModelChange) = rememberPreference(key = AICustomModelKey, defaultValue = "")
    val (customBaseUrl, onCustomBaseUrlChange) = rememberPreference(key = AICustomBaseURLKey, defaultValue = "")
    val (useAITranslation, onUseAITranslationChange) = rememberPreference(key = UseAITranslationKey, defaultValue = false)
    val (targetLanguage, onTargetLanguageChange) = rememberPreference(key = AITargetLanguageKey, defaultValue = "")

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showBaseUrlDialog by remember { mutableStateOf(false) }

    if (showApiKeyDialog) {
        var value by rememberSaveable { mutableStateOf(apiKey) }
        DefaultDialog(
            onDismiss = { showApiKeyDialog = false },
            content = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.ai_api_key)) },
                    supportingText = { Text(stringResource(R.string.ai_api_key_desc)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onApiKeyChange(value.trim())
                        showApiKeyDialog = false
                    },
                ) {
                    Text(stringResource(R.string.set))
                }
            },
        )
    }

    if (showModelDialog) {
        var value by rememberSaveable { mutableStateOf(customModel) }
        DefaultDialog(
            onDismiss = { showModelDialog = false },
            content = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.ai_custom_model)) },
                    supportingText = { Text(stringResource(R.string.ai_custom_model_desc)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onCustomModelChange(value.trim())
                        showModelDialog = false
                    },
                ) {
                    Text(stringResource(R.string.set))
                }
            },
        )
    }

    if (showBaseUrlDialog) {
        var value by rememberSaveable { mutableStateOf(customBaseUrl) }
        DefaultDialog(
            onDismiss = { showBaseUrlDialog = false },
            content = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.ai_base_url)) },
                    supportingText = { Text(stringResource(R.string.ai_base_url_desc)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showBaseUrlDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onCustomBaseUrlChange(value.trim())
                        showBaseUrlDialog = false
                    },
                ) {
                    Text(stringResource(R.string.set))
                }
            },
        )
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(scrollState),
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.ai),
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.ai_provider)) },
            icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
            selectedValue = aiProvider,
            onValueSelected = onAIProviderChange,
            valueText = {
                when (it) {
                    AIProvider.OPENAI -> stringResource(R.string.ai_provider_openai)
                    AIProvider.GEMINI -> stringResource(R.string.ai_provider_gemini)
                    AIProvider.CUSTOM_OPENAI -> stringResource(R.string.ai_provider_custom)
                }
            },
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.ai_api_key)) },
            description = if (apiKey.isEmpty()) null else "••••••••",
            icon = { Icon(painterResource(R.drawable.security), null) },
            onClick = { showApiKeyDialog = true },
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.ai_custom_model)) },
            description = customModel.ifEmpty { null },
            icon = { Icon(painterResource(R.drawable.tune), null) },
            onClick = { showModelDialog = true },
        )

        if (aiProvider == AIProvider.CUSTOM_OPENAI) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.ai_base_url)) },
                description = customBaseUrl.ifEmpty { "https://api.openai.com/v1/" },
                icon = { Icon(painterResource(R.drawable.language), null) },
                onClick = { showBaseUrlDialog = true },
            )
        }

        ListPreference(
            title = { Text(stringResource(R.string.ai_translation_language)) },
            icon = { Icon(painterResource(R.drawable.translate), null) },
            selectedValue = targetLanguage,
            values = AITranslator.TARGET_LANGUAGES.map { it.first },
            valueText = { code ->
                if (code.isEmpty()) stringResource(R.string.system_default)
                else AITranslator.TARGET_LANGUAGES.firstOrNull { it.first == code }?.second ?: code
            },
            onValueSelected = onTargetLanguageChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.use_ai_translation)) },
            description = stringResource(R.string.use_ai_translation_desc),
            icon = { Icon(painterResource(R.drawable.translate), null) },
            checked = useAITranslation,
            onCheckedChange = onUseAITranslationChange,
            isEnabled = apiKey.isNotEmpty(),
        )

        Text(
            text = stringResource(R.string.use_ai_translation_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }

    TopAppBar(
        title = {
            // Echo-style collapse: the big in-content title hands over to the top bar while scrolling.
                            Text(stringResource(R.string.ai))

        },
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
        scrollBehavior = scrollBehavior,
    )
}
