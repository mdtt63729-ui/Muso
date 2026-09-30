# Round 172 (v0.5.189, code 196) — ktor 2.x API fix for the CI compile

The v0.5.188 CI log showed exactly two compile errors, both in the kit's
Music Together files: 'Unresolved reference pingIntervalMillis'
(TogetherClient.kt:118, TogetherOnlineHost.kt:68). Those lines were
written against ktor 3.x (which is exactly why the version mix happened in
the first place: the kit was developed on ktor 3). With all of ktor now
forced to muso's 2.3.12, the property is called `pingInterval` (Long,
millis) - same 25s value, same behavior.

These were the ONLY two errors in the whole :app compile pass, so nothing
else in the kit used ktor-3-only APIs.
