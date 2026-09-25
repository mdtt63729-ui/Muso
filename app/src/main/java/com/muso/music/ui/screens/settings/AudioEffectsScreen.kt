package com.muso.music.ui.screens.settings

import android.media.audiofx.Equalizer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muso.music.LocalPlayerAwareWindowInsets
import com.muso.music.LocalPlayerConnection
import com.muso.music.R
import com.muso.music.constants.AudioEffectsEnabledKey
import com.muso.music.constants.BassBoostLevelKey
import com.muso.music.constants.EqualizerEnabledKey
import com.muso.music.constants.EqualizerLevelsKey
import com.muso.music.constants.EqualizerPresetKey
import com.muso.music.constants.ReverbPresetKey
import com.muso.music.constants.VirtualizerLevelKey
import com.muso.music.ui.component.ChipsRow
import com.muso.music.ui.component.IconButton
import com.muso.music.ui.component.PreferenceEntry
import com.muso.music.ui.component.PreferenceGroupTitle
import com.muso.music.ui.component.SwitchPreference
import com.muso.music.ui.utils.backToMain
import com.muso.music.utils.rememberPreference

private data class EqualizerInfo(
    val bands: Int,
    val minLevel: Int,
    val maxLevel: Int,
    val centerFreqs: List<Int>, // in milliHertz
    val presets: List<String>,
)

private fun formatFreq(milliHz: Int): String =
    if (milliHz >= 1_000_000) {
        "${milliHz / 100000 / 10.0} kHz"
    } else {
        "${milliHz / 1000} Hz"
    }

/**
 * Real device audio effects: Equalizer, BassBoost, Virtualizer and PresetReverb from
 * android.media.audiofx, applied by MusicService's AudioEffectsManager to the player's
 * audio session. This screen only writes preferences — the service does the audio work —
 * so nothing here can crash or block playback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (enabled, onEnabledChange) = rememberPreference(AudioEffectsEnabledKey, defaultValue = false)
    val (eqEnabled, onEqEnabledChange) = rememberPreference(EqualizerEnabledKey, defaultValue = false)
    val (eqPreset, onEqPresetChange) = rememberPreference(EqualizerPresetKey, defaultValue = -1)
    val (eqLevels, onEqLevelsChange) = rememberPreference(EqualizerLevelsKey, defaultValue = "")
    val (bassLevel, onBassLevelChange) = rememberPreference(BassBoostLevelKey, defaultValue = 0)
    val (virtualizerLevel, onVirtualizerLevelChange) = rememberPreference(VirtualizerLevelKey, defaultValue = 0)
    val (reverbPreset, onReverbPresetChange) = rememberPreference(ReverbPresetKey, defaultValue = 0)

    val playerConnection = LocalPlayerConnection.current

    // Probe the device equalizer once: band count, level range and preset names. Attached
    // to the player's own session (no extra permission needed) and released immediately.
    val eqInfo = remember(playerConnection) {
        runCatching {
            val sessionId = playerConnection?.player?.audioSessionId ?: 0
            val probe = Equalizer(0, sessionId)
            val info = EqualizerInfo(
                bands = probe.numberOfBands.toInt(),
                minLevel = probe.bandLevelRange[0].toInt(),
                maxLevel = probe.bandLevelRange[1].toInt(),
                centerFreqs = List(probe.numberOfBands.toInt()) { probe.getCenterFreq(it.toShort()) },
                presets = List(probe.numberOfPresets.toInt()) { probe.getPresetName(it.toShort()) },
            )
            probe.release()
            info
        }.getOrNull()
    }

    // Custom band levels (in milli-bel, exactly what Equalizer.setBandLevel takes).
    var bandLevels by remember(eqLevels, eqInfo) {
        val parsed = eqLevels.split(',').mapNotNull { it.toIntOrNull() }
        val defaults = List(eqInfo?.bands ?: 0) { 0 }
        mutableStateOf(if (parsed.size == defaults.size) parsed else defaults)
    }

    val customLabel = stringResource(R.string.custom)
    val presetChips = remember(eqInfo, customLabel) {
        buildList {
            add(-1 to customLabel)
            eqInfo?.presets?.forEachIndexed { index, name -> add(index to name) }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        SwitchPreference(
            title = { Text(stringResource(R.string.audio_effects)) },
            description = stringResource(R.string.audio_effects_desc),
            icon = { Icon(painterResource(R.drawable.equalizer), null) },
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )

        PreferenceGroupTitle(title = stringResource(R.string.equalizer))

        if (eqInfo == null) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.equalizer_unavailable)) },
                icon = { Icon(painterResource(R.drawable.equalizer), null) },
            )
        } else {
            SwitchPreference(
                title = { Text(stringResource(R.string.equalizer)) },
                description = stringResource(R.string.equalizer_desc),
                icon = { Icon(painterResource(R.drawable.equalizer), null) },
                checked = eqEnabled,
                onCheckedChange = onEqEnabledChange,
                isEnabled = enabled,
            )

            // Preset chips: "Custom" + the device's own preset names.
            ChipsRow(
                chips = presetChips,
                currentValue = eqPreset,
                onValueUpdate = { index ->
                    onEqPresetChange(index)
                    if (index != -1) onEqLevelsChange("")
                },
            )

            // One horizontal slider per band — vertical sliders fight with gesture
            // navigation, horizontal ones stay smooth and reliable.
            eqInfo.centerFreqs.forEachIndexed { index, freq ->
                val level = bandLevels.getOrNull(index) ?: 0
                PreferenceEntry(
                    title = {
                        Text(
                            formatFreq(freq),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    icon = null,
                    isEnabled = enabled && eqEnabled && eqPreset == -1,
                    content = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { value ->
                                    bandLevels = bandLevels.toMutableList().also { levels ->
                                        if (index < levels.size) levels[index] = value.toInt()
                                    }
                                },
                                onValueChangeFinished = {
                                    onEqPresetChange(-1)
                                    onEqLevelsChange(bandLevels.joinToString(","))
                                },
                                valueRange = eqInfo.minLevel.toFloat()..eqInfo.maxLevel.toFloat(),
                                enabled = enabled && eqEnabled && eqPreset == -1,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "%.1f dB".format(level / 100f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    },
                )
            }
        }

        PreferenceGroupTitle(title = stringResource(R.string.bass_boost))
        PreferenceEntry(
            title = { Text("${bassLevel / 10}%") },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            isEnabled = enabled,
            content = {
                Slider(
                    value = bassLevel / 10f,
                    onValueChange = { onBassLevelChange((it * 10).toInt()) },
                    valueRange = 0f..100f,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )

        PreferenceGroupTitle(title = stringResource(R.string.virtualizer))
        PreferenceEntry(
            title = { Text("${virtualizerLevel / 10}%") },
            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
            isEnabled = enabled,
            content = {
                Slider(
                    value = virtualizerLevel / 10f,
                    onValueChange = { onVirtualizerLevelChange((it * 10).toInt()) },
                    valueRange = 0f..100f,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )

        PreferenceGroupTitle(title = stringResource(R.string.reverb))
        ChipsRow(
            chips = listOf(
                0 to stringResource(R.string.reverb_off),
                1 to stringResource(R.string.reverb_small_room),
                2 to stringResource(R.string.reverb_medium_room),
                3 to stringResource(R.string.reverb_large_room),
                4 to stringResource(R.string.reverb_medium_hall),
                5 to stringResource(R.string.reverb_large_hall),
                6 to stringResource(R.string.reverb_plate),
            ),
            currentValue = reverbPreset,
            onValueUpdate = onReverbPresetChange,
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.audio_effects)) },
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
