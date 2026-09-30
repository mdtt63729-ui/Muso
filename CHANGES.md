# Round 178 (v0.5.195, code 202) — frame-jank instrumentation

User report: the WHOLE app lags (settings, navigation, bottom-nav switches,
lyrics screen) even at 30fps.

The named screens are all different composables with different renderers;
fixing blind has fixed the lyrics word-by-word path already, but a
whole-app jank report needs MEASUREMENT, not another guess. This build adds
FrameJankMonitor: a Choreographer frame callback that writes a line every
10 seconds to Downloads/Muso/jank_log.txt with the measured fps, the
number of frames over 40ms, and the worst frame. One session of that file
says exactly which class of problem it is (global burner vs navigation
spikes vs power-save 30fps), and the next round fixes the real renderer.
