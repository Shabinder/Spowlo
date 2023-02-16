package com.bobbyesp.spowlo.ui.common

object Route {
    const val HOME = "home"
    const val DOWNLOADER = "downloader"
    const val DOWNLOADS_HISTORY = "download_history"
    const val PLAYLIST = "playlist"
    const val SETTINGS = "settings"
    const val FORMAT_SELECTION = "format"
    const val TASK_LIST = "task_list"
    const val TASK_LOG = "task_log"
    const val PLAYLIST_METADATA_PAGE = "playlist_metadata_page"
    const val MODS_DOWNLOADER = "mods_downloader"
    const val SEARCHER = "searcher"
    const val MEDIA_PLAYER = "media_player"

    const val APPEARANCE = "appearance"
    const val APP_THEME = "app_theme"
    const val GENERAL_DOWNLOAD_PREFERENCES = "general_download_preferences"
    const val SPOTIFY_PREFERENCES = "spotify_preferences"
    const val ABOUT = "about"
    const val DOWNLOAD_DIRECTORY = "download_directory"
    const val CREDITS = "credits"
    const val LANGUAGES = "languages"
    const val TEMPLATE = "template"
    const val TEMPLATE_EDIT = "template_edit"
    const val DARK_THEME = "dark_theme"
    const val DOWNLOAD_QUEUE = "queue"
    const val DOWNLOAD_FORMAT = "download_format"
    const val NETWORK_PREFERENCES = "network_preferences"
    const val COOKIE_PROFILE = "cookie_profile"
    const val COOKIE_GENERATOR_WEBVIEW = "cookie_webview"
    const val SUBTITLE_PREFERENCES = "subtitle_preferences"

    const val TASK_HASHCODE = "task_hashcode"
    const val TEMPLATE_ID = "template_id"
}

infix fun String.arg(arg: String) = "$this/{$arg}"
infix fun String.id(id: Int) = "$this/$id"