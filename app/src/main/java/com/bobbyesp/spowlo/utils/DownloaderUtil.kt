package com.bobbyesp.spowlo.utils

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.CheckResult
import com.bobbyesp.library.SpotDL
import com.bobbyesp.library.SpotDLRequest
import com.bobbyesp.library.dto.Song
import com.bobbyesp.spowlo.App.Companion.audioDownloadDir
import com.bobbyesp.spowlo.App.Companion.context
import com.bobbyesp.spowlo.Downloader
import com.bobbyesp.spowlo.Downloader.onProcessEnded
import com.bobbyesp.spowlo.Downloader.onProcessStarted
import com.bobbyesp.spowlo.Downloader.onTaskEnded
import com.bobbyesp.spowlo.Downloader.onTaskError
import com.bobbyesp.spowlo.Downloader.onTaskStarted
import com.bobbyesp.spowlo.Downloader.toNotificationId
import com.bobbyesp.spowlo.R
import com.bobbyesp.spowlo.database.DownloadedSongInfo
import com.bobbyesp.spowlo.ui.pages.settings.cookies.Cookie
import com.bobbyesp.spowlo.utils.FilesUtil.getCookiesFile
import com.bobbyesp.spowlo.utils.FilesUtil.getSdcardTempDir
import com.bobbyesp.spowlo.utils.FilesUtil.moveFilesToSdcard
import com.bobbyesp.spowlo.utils.PreferencesUtil.getInt
import com.bobbyesp.spowlo.utils.PreferencesUtil.getString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.util.UUID

object DownloaderUtil {

    private const val COOKIE_HEADER =
        "# Netscape HTTP Cookie File\n" + "# Auto-generated by Spowlo built-in WebView\n"

    private const val TAG = "DownloaderUtil"

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
    }

    val settings = PreferencesUtil

    //SONGS FLOW
    private val mutableSongsState = MutableStateFlow(listOf<Song>())
    val songsState = mutableSongsState.asStateFlow()

    data class DownloadPreferences(
        val downloadPlaylist: Boolean = PreferencesUtil.getValue(PLAYLIST),
        val customPath: Boolean = PreferencesUtil.getValue(CUSTOM_PATH),
        val maxFileSize: String = MAX_FILE_SIZE.getString(),
        val cookies: Boolean = PreferencesUtil.getValue(COOKIES),
        val cookiesContent: String = PreferencesUtil.getCookies(),
        val audioFormat: Int = PreferencesUtil.getAudioFormat(),
        val audioQuality: Int = PreferencesUtil.getAudioQuality(),
        val preserveOriginalAudio: Boolean = PreferencesUtil.getValue(ORIGINAL_AUDIO),
        val useSpotifyPreferences: Boolean = PreferencesUtil.getValue(USE_SPOTIFY_CREDENTIALS),
        val spotifyClientID: String = SPOTIFY_CLIENT_ID.getString(),
        val spotifyClientSecret: String = SPOTIFY_CLIENT_SECRET.getString(),
        val useYtMetadata: Boolean = PreferencesUtil.getValue(USE_YT_METADATA),
        val useCookies: Boolean = PreferencesUtil.getValue(COOKIES),
        val useSyncedLyrics: Boolean = PreferencesUtil.getValue(SYNCED_LYRICS),
        val useCaching: Boolean = PreferencesUtil.getValue(USE_CACHING),
        val dontFilter: Boolean = PreferencesUtil.getValue(DONT_FILTER_RESULTS),
        val formatId: String = "",
        val privateMode: Boolean = PreferencesUtil.getValue(PRIVATE_MODE),
        val sdcard: Boolean = PreferencesUtil.getValue(SDCARD_DOWNLOAD),
        val sdcardUri: String = SDCARD_URI.getString(),
        val extraDirectory: String = PreferencesUtil.getExtraDirectory(),
        val threads: Int = THREADS.getInt()
    )

    object CookieScheme {
        const val NAME = "name"
        const val VALUE = "value"
        const val SECURE = "is_secure"
        const val EXPIRY = "expires_utc"
        const val HOST = "host_key"
        const val PATH = "path"
    }

    fun getCookiesContentFromDatabase(): Result<String> = runCatching {
        CookieManager.getInstance().run {
            if (!hasCookies()) throw Exception("There is no cookies in the database!")
            flush()
        }
        SQLiteDatabase.openDatabase(
            "/data/data/com.bobbyesp.spowlo/app_webview/Default/Cookies",
            null,
            SQLiteDatabase.OPEN_READONLY
        ).run {
            val projection = arrayOf(
                CookieScheme.HOST,
                CookieScheme.EXPIRY,
                CookieScheme.PATH,
                CookieScheme.NAME,
                CookieScheme.VALUE,
                CookieScheme.SECURE
            )
            val cookieList = mutableListOf<Cookie>()
            query(
                "cookies", projection, null, null, null, null, null
            ).run {
                while (moveToNext()) {
                    val expiry = getLong(getColumnIndexOrThrow(CookieScheme.EXPIRY))
                    val name = getString(getColumnIndexOrThrow(CookieScheme.NAME))
                    val value = getString(getColumnIndexOrThrow(CookieScheme.VALUE))
                    val path = getString(getColumnIndexOrThrow(CookieScheme.PATH))
                    val secure = getLong(getColumnIndexOrThrow(CookieScheme.SECURE)) == 1L
                    val hostKey = getString(getColumnIndexOrThrow(CookieScheme.HOST))

                    val host = if (hostKey[0] != '.') ".$hostKey" else hostKey
                    cookieList.add(
                        Cookie(
                            domain = host,
                            name = name,
                            value = value,
                            path = path,
                            secure = secure,
                            expiry = expiry
                        )
                    )
                }
                close()
            }
            Log.d(TAG, "Loaded ${cookieList.size} cookies from database!")
            cookieList.fold(StringBuilder(COOKIE_HEADER)) { acc, cookie ->
                acc.append(cookie.toNetscapeCookieString()).append("\n")
            }.toString()
        }
    }

    //Get a random UUID and return it as a string
    private fun getRandomUUID(): String {
        return UUID.randomUUID().toString()
    }

    //Use cookies from the database
    private fun SpotDLRequest.useCookies(): SpotDLRequest = this.apply {
        if (PreferencesUtil.getValue(COOKIES)) {
            addOption(
                "--cookie-file", context.getCookiesFile().absolutePath
            )
        }
    }

    @CheckResult
    private fun getSongInfo(
        url: String? = null,
    ): Result<List<Song>> = kotlin.runCatching {
        val response: List<Song> = SpotDL.getInstance().getSongInfo(url ?: "")
        mutableSongsState.update {
            response
        }
        response
    }

    @CheckResult
    fun fetchSongInfoFromUrl(
        url: String
    ): Result<List<Song>> = kotlin.run {
        getSongInfo(url)
    }

    fun updateSongsState(songs: List<Song>) {
        mutableSongsState.update {
            songs
        }
    }

    //get the audio format
    private fun SpotDLRequest.addAudioFormat(): SpotDLRequest = this.apply {
        when (PreferencesUtil.getAudioFormat()) {
            0 -> addOption("--format", "mp3")
            1 -> addOption("--format", "flac")
            2 -> addOption("--format", "ogg")
            3 -> addOption("--format", "opus")
            4 -> addOption("--format", "m4a")
            5 -> null
        }
    }

    //get the audio quality
    private fun SpotDLRequest.addAudioQuality(): SpotDLRequest = this.apply {
        when (PreferencesUtil.getAudioQuality()) {
            0 -> addOption("--bitrate", "auto")
            1 -> addOption("--bitrate", "8k")
            2 -> addOption("--bitrate", "16k")
            3 -> addOption("--bitrate", "24k")
            4 -> addOption("--bitrate", "32k")
            5 -> addOption("--bitrate", "40k")
            6 -> addOption("--bitrate", "48k")
            7 -> addOption("--bitrate", "64k")
            8 -> addOption("--bitrate", "80k")
            9 -> addOption("--bitrate", "96k")
            10 -> addOption("--bitrate", "112k")
            11 -> addOption("--bitrate", "128k")
            12 -> addOption("--bitrate", "160k")
            13 -> addOption("--bitrate", "192k")
            14 -> addOption("--bitrate", "224k")
            15 -> addOption("--bitrate", "256k")
            16 -> addOption("--bitrate", "320k")
            17 -> addOption("--bitrate", "disable")
        }
    }

    private fun SpotDLRequest.addAudioProvider(): SpotDLRequest = this.apply {
        when (PreferencesUtil.getAudioProvider()) {
            0 -> null
            1 -> {
                addOption("--audio", "youtube-music")
                addOption("youtube")
            }

            2 -> addOption("--audio", "youtube-music")
            3 -> addOption("--audio", "youtube")
        }
    }

    //HERE GOES ALL THE DOWNLOADER OPTIONS
    private fun commonRequest(
        downloadPreferences: DownloadPreferences,
        url: String,
        request: SpotDLRequest,
        pathBuilder: StringBuilder
    ): SpotDLRequest {
        with(downloadPreferences) {
            request.apply {
                addOption("download", url)

                pathBuilder.append(audioDownloadDir)

                if (extraDirectory.isNotEmpty()) {
                    pathBuilder.append("/").append(extraDirectory)
                }

                Log.d(TAG, "downloadSong: $pathBuilder")

                addOption("--output", pathBuilder.toString())

                if (useCookies) {
                    useCookies()
                }

                if (!useCaching) {
                    addOption("--no-cache")
                }

                if (useYtMetadata) {
                    addOption("--ytm-data")
                }

                if (dontFilter) {
                    addOption("--dont-filter-results")
                }

                if (useSyncedLyrics) {
                    addOption("--lyrics", "synced")
                }

                if (preserveOriginalAudio) {
                    addOption("--bitrate", "disable")
                    addAudioFormat()
                } else {
                    addAudioQuality()
                    addAudioFormat()
                }

                addAudioProvider()

                for (s in request.buildCommand()) Log.d(TAG, s)
            }
        }
        return request
    }

    @CheckResult
    fun downloadSong(
        songInfo: Song = Song(),
        taskId: String,
        downloadPreferences: DownloadPreferences,
        progressCallback: ((Float, Long, String) -> Unit)?
    ): Result<List<String>> {
        if (songInfo == Song()) return Result.failure(Throwable(context.getString(R.string.fetch_info_error_msg)))
        with(downloadPreferences) {
            val url = songInfo.url

            val request = SpotDLRequest()
            val pathBuilder = StringBuilder()
            commonRequest(downloadPreferences, url, request, pathBuilder)
                .apply {
                    if (useSpotifyPreferences) {
                        if (spotifyClientID.isEmpty() || spotifyClientSecret.isEmpty()) return Result.failure(
                            Throwable("Spotify client ID or secret is empty while you have the custom credentials option enabled! \n Please check your settings.")
                        )
                        addOption("--client-id", spotifyClientID)
                        addOption("--client-secret", spotifyClientSecret)
                    }
                }.runCatching {
                    SpotDL.getInstance().execute(this, taskId, callback = progressCallback)
                }.onFailure { th ->
                    return if (th.message?.contains("No such file or directory") == true) {
                        th.printStackTrace()
                        onFinishDownloading(
                            this,
                            songInfo = songInfo,
                            downloadPath = pathBuilder.toString(),
                            sdcardUri = sdcardUri
                        )
                    } else {
                        return Result.failure(th)
                    }
                }.onSuccess { response ->
                    return when {
                        response.output.contains("LookupError") -> Result.failure(Throwable("A LookupError occurred. The song wasn't found. Try changing the audio provider in the settings and also disabling the 'Don't filter results' option."))
                        response.output.contains("YT-DLP") -> Result.failure(Throwable("An error occurred to yt-dlp while downloading the song. Please, report this issue in GitHub."))
                        else -> onFinishDownloading(
                            this,
                            songInfo = songInfo,
                            downloadPath = pathBuilder.toString(),
                            sdcardUri = sdcardUri
                        )

                    }
                }
            return onFinishDownloading(
                this,
                songInfo = songInfo,
                downloadPath = pathBuilder.toString(),
                sdcardUri = sdcardUri
            )
        }
    }

    private fun onFinishDownloading(
        preferences: DownloadPreferences, songInfo: Song, downloadPath: String, sdcardUri: String
    ): Result<List<String>> = preferences.run {
        if (privateMode) {
            Result.success(emptyList())
        } else if (sdcard) {
            Result.success(moveFilesToSdcard(
                sdcardUri = sdcardUri, tempPath = context.getSdcardTempDir(songInfo.song_id)
            ).apply {
                insertInfoIntoDownloadHistory(songInfo, this)
            })
        } else {
            Result.success(
                scanVideoIntoDownloadHistory(
                    songInfo = songInfo,
                    downloadPath = downloadPath,
                )
            )
        }
    }

    @CheckResult
    private fun scanVideoIntoDownloadHistory(
        songInfo: Song,
        downloadPath: String,
    ): List<String> = FilesUtil.scanFileToMediaLibraryPostDownload(
        title = songInfo.name, downloadDir = downloadPath
    ).apply {
        Log.d(TAG, "scanVideoIntoDownloadHistory: $downloadPath")
        insertInfoIntoDownloadHistory(songInfo, this)
    }

    private fun insertInfoIntoDownloadHistory(
        songInfo: Song, filePaths: List<String>
    ) {
        filePaths.forEach { filePath ->
            val fullString = StringBuilder()
            fullString.append(songInfo.name)
            fullString.append(filePath)
            DatabaseUtil.insertInfo(
                DownloadedSongInfo(
                    id = createIntFromString(fullString.toString()),
                    songName = songInfo.name,
                    songAuthor = songInfo.artist,
                    songUrl = songInfo.url,
                    thumbnailUrl = songInfo.cover_url,
                    songPath = filePath,
                    songDuration = songInfo.duration,
                    extractor = "Youtube Music",
                )
            )
        }
    }

    private fun createIntFromString(string: String): Int {
        var int = 0
        for (i in string.indices) {
            int += string[i].code
        }
        return int
    }


    fun executeParallelDownload(url: String, name: String) {
        val taskId = Downloader.makeKey(url, url.reversed())
        ToastUtil.makeToastSuspend(context.getString(R.string.download_started_msg))

        val pathBuilder = StringBuilder()
        val downloadPreferences = DownloadPreferences()
        val request = commonRequest(downloadPreferences, url, SpotDLRequest(), pathBuilder).apply {
            addOption("--threads", downloadPreferences.threads.toString())
        }

        val isPlaylist = url.contains("playlist")

        onProcessStarted()
        onTaskStarted(url, name)
        kotlin.runCatching {
            val response = SpotDL.getInstance().execute(
                request = request,
                processId = taskId,
                forceProcessDestroy = true,
                callback = { progress, _, text ->
                    NotificationsUtil.makeNotificationForParallelDownloads(
                        notificationId = taskId.toNotificationId(),
                        taskId = taskId,
                        progress = progress.toInt(),
                        text = text,
                        extraString = name + " - " + context.getString(R.string.parallel_download),
                        taskUrl = url,
                    )
                    Downloader.updateTaskOutput(
                        url = url, line = text, progress = progress, isPlaylist = isPlaylist
                    )
                })
            //clear all the lines that contains a "…" on it
            val finalResponse = removeDuplicateLines(clearLinesWithEllipsis(response.output))
            onTaskEnded(url, finalResponse, name)
        }.onFailure {
            Log.d("Canceled?", "Exception: $it")
            it.printStackTrace()
            ToastUtil.makeToastSuspend(context.getString(R.string.download_error_msg))
            if (it is SpotDL.CanceledException) return@onFailure
            it.message.run {
                if (isNullOrEmpty()) onTaskEnded(url)
                else onTaskError(this, url)
            }
        }
        onProcessEnded()
        ToastUtil.makeToastSuspend(context.getString(R.string.download_finished_msg))
    }

    fun clearLinesWithEllipsis(input: String): String {
        val lines = input.split("\n")
            .filterNot { it.contains("…") }
            .joinToString("\n")
        return lines
    }

    fun removeDuplicateLines(input: String): String {
        val lines = input.split("\n")
            .distinct()
            .joinToString("\n")
        return lines
    }
}