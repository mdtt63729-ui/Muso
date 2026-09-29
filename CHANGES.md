# Round 170 (v0.5.187, code 194) — startup hardening II + visible Downloads logs

User report: app STILL crash-loops on open; no Muso folder visible in Files.

1. Onboarding moved off the startup composition (Round 170 fix):
   - It no longer constructs OnboardingViewModel + kit DataStore flow while
     the app is still booting. MainActivity field-injects the kit
     OnboardingRepository (DataStore-only) and, AFTER the main UI has fully
     rendered, reads the completion flag once (runCatching - any failure just
     skips the onboarding) and only then composes OnboardingRoute as an
     overlay. The startup composition is now byte-for-byte the same shape as
     the last known-good build (v0.5.179) plus two trivial dialogs.
2. Logs are now ALWAYS visible with zero permissions:
   - Every crash is written to Downloads/Muso/crash_log.txt via MediaStore
     (Android 10+, no permission needed; direct write on older versions) -
     visible in ANY file manager, even while the app crash-loops before the
     All Files Access prompt could ever show.
   - Each crash entry carries the last ~300 lines of logcat - the actual
     crash reason with the exact stack trace.
   - Downloads/Muso/app_log.txt gets one heartbeat line per app start, so it
     is possible to tell whether Application.onCreate ran at all (written off
     the main thread).
   - The Muso folder (public when All Files Access is granted) and the
     FileProvider share button from Round 169 remain.
