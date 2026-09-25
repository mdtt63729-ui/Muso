package com.muso.music.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.datastore.preferences.core.Preferences
import androidx.media3.exoplayer.ExoPlayer
import com.muso.music.constants.AudioEffectsEnabledKey
import com.muso.music.constants.BassBoostLevelKey
import com.muso.music.constants.EqualizerEnabledKey
import com.muso.music.constants.EqualizerLevelsKey
import com.muso.music.constants.EqualizerPresetKey
import com.muso.music.constants.ReverbPresetKey
import com.muso.music.constants.VirtualizerLevelKey
import com.muso.music.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Applies the device's built-in audio effects (Equalizer, BassBoost, Virtualizer,
 * PresetReverb from android.media.audiofx) to the player's audio session.
 *
 * The UI never touches the effect objects directly: it only writes preferences, and this
 * manager collects the preferences and applies them to the live effects. That keeps the
 * settings screen crash-free (no shared mutable state) and lag-free (toggles write a
 * DataStore entry, nothing else).
 *
 * ExoPlayer's audio session id can change (device/route changes reconfigure the audio
 * sink), so while effects are enabled a lightweight watcher re-attaches them if the
 * session ever moves. Every audiofx call is wrapped so that a device without support can
 * never crash the service.
 */
class AudioEffectsManager(
    context: Context,
    private val player: ExoPlayer,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var enabled = false
    private var latestPrefs: Preferences? = null

    private var sessionId = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null

    fun start() {
        scope.launch {
            context.dataStore.data.collect { prefs ->
                latestPrefs = prefs
                enabled = prefs[AudioEffectsEnabledKey] ?: false
                if (enabled) {
                    attachIfNeeded()
                    applyPrefs(prefs)
                } else {
                    releaseEffects()
                }
            }
        }
        // Re-attach when the audio session changes (cheap int comparison every 2s, only
        // while effects are on).
        scope.launch {
            while (isActive) {
                delay(2000)
                if (enabled && (equalizer == null || sessionId != player.audioSessionId)) {
                    attachIfNeeded()
                    latestPrefs?.let { applyPrefs(it) }
                }
            }
        }
    }

    private fun attachIfNeeded() {
        val sid = player.audioSessionId
        if (sid == 0) return
        if (sid == sessionId && equalizer != null) return
        releaseEffects()
        runCatching {
            equalizer = Equalizer(0, sid)
            bassBoost = BassBoost(0, sid)
            virtualizer = Virtualizer(0, sid)
            reverb = PresetReverb(0, sid)
            sessionId = sid
        }.onFailure {
            releaseEffects()
        }
    }

    private fun applyPrefs(prefs: Preferences) {
        val eq = equalizer ?: return
        runCatching {
            val eqEnabled = prefs[EqualizerEnabledKey] ?: false
            if (eqEnabled) {
                eq.enabled = true
                val levels = prefs[EqualizerLevelsKey]
                if (levels != null) {
                    val bands = eq.numberOfBands.toInt()
                    levels.split(',').forEachIndexed { index, level ->
                        if (index < bands) {
                            level.toShortOrNull()?.let { eq.setBandLevel(index.toShort(), it) }
                        }
                    }
                } else {
                    val preset = prefs[EqualizerPresetKey] ?: 0
                    if (preset in 0 until eq.numberOfPresets.toInt()) {
                        eq.usePreset(preset.toShort())
                    }
                }
            } else {
                eq.enabled = false
            }

            bassBoost?.let { boost ->
                val strength = (prefs[BassBoostLevelKey] ?: 0).coerceIn(0, 1000)
                boost.enabled = strength > 0
                boost.setStrength(strength.toShort())
            }
            virtualizer?.let { virt ->
                val strength = (prefs[VirtualizerLevelKey] ?: 0).coerceIn(0, 1000)
                virt.enabled = strength > 0
                virt.setStrength(strength.toShort())
            }
            reverb?.let { rv ->
                val preset = prefs[ReverbPresetKey] ?: 0
                rv.enabled = preset > 0
                if (preset in 1..6) {
                    rv.setPreset(preset.toShort())
                }
            }
        }
    }

    private fun releaseEffects() {
        listOf(equalizer, bassBoost, virtualizer, reverb).forEach { effect ->
            runCatching {
                effect?.enabled = false
            }
            runCatching {
                effect?.release()
            }
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
        sessionId = 0
    }

    fun release() {
        releaseEffects()
        scope.cancel()
    }
}
