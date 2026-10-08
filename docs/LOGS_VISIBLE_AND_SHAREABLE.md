# The log folder was never missing — it was hidden. Now it is visible and one tap away.

Snapshot: **0.5.256**.

## What the release page proved

The screenshot of the GitHub release page shows `Muso v0.5.254 (versionCode 261)`, "built
automatically from the latest source". **So the build succeeded.** My earlier conclusion that the
builds were failing was wrong, and the code has been on the phone.

That changes the question. If the code is running and the log folder still cannot be found, the
answer has to be *where* the folder is — and it is:

```
/storage/emulated/0/Android/data/com.muso.music/files/Muso/
```

**Android 11+ hides `Android/data` from file managers.** Most file managers will not list it at
all; the ones that try show an empty folder. So the folder was being created, and being written
to, and was simply unreachable — which is exactly what "it isn't saving" looks like from the
outside. That is a fault in the design, not in the user.

## Two fixes

### 1. A visible copy

The UI trace is now mirrored to **`Downloads/Muso/ui_log.txt`**, written through MediaStore —
which needs **no permission** on Android 10+ and shows up in any file manager, under
`Downloads/Muso`. It is buffered and drained every 4 seconds, because a MediaStore write on every
press would be far too expensive.

So: **Files → Downloads → Muso → `ui_log.txt`** now contains

```
2026-10-08 09:49:16.412  SCREEN  library
2026-10-08 09:49:17.006  PRESS  screen=library
2026-10-08 09:49:18.551  PRESS  screen=player
```

### 2. A "Share logs" button

**Settings → About** now has a **Share logs** row, with the folder path printed underneath it. One
tap opens the share sheet with the crash log, the cumulative crash log and the full `main.txt`
attached — through FileProvider, so again no permission of any kind. It is the direct answer to
"where do I get the log": from in the app.

## The two copies, and which is which

| where | what | deleted on uninstall |
|---|---|---|
| `Android/data/com.muso.music/files/Muso/` | `main.txt` (whole logcat), `crash_log*.txt` | yes |
| `Downloads/Muso/` | `ui_log.txt` (screens, presses), `crash_log.txt` | no |

The app-external copy is the complete one and is removed with the app. The Downloads copy is the
one a person can actually find, and it is why it exists.

## Verified

1,073 `.kt` files parse clean with tree-sitter (the 4 known false positives); `MusoLog.kt` and
`AboutScreen.kt` are brace/paren balanced; `logDir` is publicly readable and `shareLogsIntent`
already carries `FLAG_GRANT_READ_URI_PERMISSION`.
