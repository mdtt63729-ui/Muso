package com.zionhuang.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val api_key: String,
    val userAgent: String,
    val osVersion: String? = null,
    val referer: String? = null,
    val osName: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: String? = null,
) {
    fun toContext(locale: YouTubeLocale, visitorData: String?) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            osVersion = osVersion,
            gl = locale.gl,
            hl = locale.hl,
            visitorData = visitorData,
            osName = osName,
            deviceMake = deviceMake,
            deviceModel = deviceModel,
            androidSdkVersion = androidSdkVersion
        )
    )

    companion object {
        private const val REFERER_YOUTUBE_MUSIC = "https://music.youtube.com/"

        private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
        private const val USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36"
        private const val USER_AGENT_IOS = "com.google.ios.youtube/21.03.1 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)"

        // InnerTube API key used by the web client, shared by the newer clients below
        // (matches the key the Echo-Music streaming stack sends for every request).
        private const val API_KEY_WEB = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3"

        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "5.01",
            api_key = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
            userAgent = USER_AGENT_ANDROID
        )

        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "17.13.3",
            api_key = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
            userAgent = USER_AGENT_ANDROID,
        )

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.2021111",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3C",
            userAgent = USER_AGENT_WEB
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260213.01.00",
            api_key = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30",
            userAgent = USER_AGENT_WEB,
            referer = REFERER_YOUTUBE_MUSIC
        )

        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            api_key = "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8",
            userAgent = "Mozilla/5.0 (PlayStation 4 5.55) AppleWebKit/601.2 (KHTML, like Gecko)"
        )

        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "21.03.1",
            api_key = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
            userAgent = USER_AGENT_IOS,
            osVersion = "18.2.22C152",
        )

        /**
         * Internal YT client for an unreleased YT client. Measured to serve complete files
         * (start to finish, ranged reads included), which is why it leads the fallback chain.
         */
        val VISIONOS = YouTubeClient(
            clientName = "VISIONOS",
            clientVersion = "0.1",
            api_key = API_KEY_WEB,
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            osName = "visionOS",
            osVersion = "1.3.21O771",
            deviceMake = "Apple",
            deviceModel = "RealityDevice14,1"
        )

        /**
         * Current ANDROID_VR pin, matching yt-dlp master and YouTube.js. Older ANDROID_VR
         * versions are bot-gated server-side (LOGIN_REQUIRED with zero formats), while 1.65.10
         * returns OK with direct-url formats. Requires a visitorData.
         */
        val ANDROID_VR_1_65_10 = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.65.10",
            api_key = API_KEY_WEB,
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osName = "Android",
            osVersion = "12L",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = "32"
        )

        /**
         * Older ANDROID_VR pin. Uses non adaptive bitrate, which fixes audio stuttering with
         * YT Music. Kept as a control against 1.65.10.
         */
        val ANDROID_VR_1_43_32 = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.43.32",
            api_key = API_KEY_WEB,
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.43.32 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)",
            osName = "Android",
            osVersion = "12",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = "32"
        )

        /**
         * The device machine id for the iPad 6th Gen (iPad7,6).
         */
        val IPADOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "21.03.3",
            api_key = API_KEY_WEB,
            userAgent = "com.google.ios.youtube/21.03.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)",
            osName = "iPadOS",
            osVersion = "17.7.10.21H450",
            deviceMake = "Apple",
            deviceModel = "iPad7,6"
        )

        /**
         * Echo-Music measured stream fallback order: the first client that returns an OK
         * playability status with usable audio formats wins.
         */
        val STREAM_FALLBACK_CLIENTS = arrayOf(
            VISIONOS,
            ANDROID_VR_1_65_10,
            ANDROID_VR_1_43_32,
            IPADOS,
            IOS
        )
    }
}
