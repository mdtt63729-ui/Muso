# Muso v0.5.218 — True Endless Queue + Scroll Fix

- Endless queue now follows queue continuations instead of appending only one related-song batch.
- When the selected queue is exhausted, playback switches to a continuation-backed radio source and keeps extending it; if a radio continuation ends, it is restarted from the current track.
- Endless mode is the sole automatic queue extender for this feature, avoiding competing pagination jobs.
- Turning Endless off trims the active queue to a maximum 15-track window and stops automatic extension.
- Initial queue creation also respects the 15-track finite window when Endless is off.
- Home/Library/search scroll containers now reserve terminal space for the floating mini-player/navigation overlay, so the last content can be scrolled fully above the controls.
- Online search suggestions receive equivalent terminal clearance.
