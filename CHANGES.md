# Round 171 (v0.5.188, code 195) — THE startup crash-loop root cause fixed

The crash logs from the user (Downloads/Muso/crash_log.txt - the Round 170
logging worked) finally showed the exact cause:

    java.lang.NoClassDefFoundError: io.ktor.client.plugins.HttpTimeout
      at io.ktor.client.engine.okhttp.OkHttpEngine.<init>
      at io.ktor.client.HttpClientKt.HttpClient
      at com.muso.music.utils.Updater (a3b).<clinit>
      at com.muso.music.App.onCreate

Root cause chain:
1. The kit's translator dependency (com.github.therealbush:translator, jitpack)
   declares api(io.ktor:ktor-client-core:3.0.1) + ktor-client-cio:3.0.1.
2. Gradle conflict resolution picks the NEWEST version, so ktor-client-core
   resolved to 3.0.1 while ktor-client-okhttp stayed at muso's pinned 2.3.12.
3. Ktor 3.0 REMOVED io.ktor.client.plugins.HttpTimeout (renamed to
   HttpTimeoutConfig/HttpTimeoutCapability). Verified against the actual
   Maven artifacts: 2.3.12 core-jvm has the class, 3.0.1/3.0.3/3.1.3 do not.
4. The 2.3.12 OkHttpEngine therefore could not resolve the class at runtime
   -> NoClassDefFoundError the moment App.onCreate touched the Updater
   object -> crash-loop, app never opened (since v0.5.182).

Fixes:
1. Root build.gradle.kts now FORCEs every io.ktor artifact (client core/
   okhttp/cio/websockets/content-negotiation/encoding, serialization-kotlinx-
   json, server core/cio/websockets/content-negotiation, http, http-cio,
   utils, io, events, network, network-tls, serialization,
   websocket-serialization, websockets) to 2.3.12 in every project and
   configuration. No transitive pull can flip any ktor artifact to 3.x again.
2. Updater's HttpClient is now built lazily (by lazy) - App.onCreate's
   WorkManager scheduling no longer constructs a network client during
   process startup at all.
3. Everything from Rounds 167-170 (kit settings port, MusoLog with
   Downloads/Muso visibility, KitSettingsHost lazy kit DI, R8 keeps) stays.
