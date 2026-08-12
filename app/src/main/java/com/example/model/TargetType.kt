package com.example.model

enum class TargetType(val displayName: String) {
    SINGLE_TAP("Tap"),
    DOUBLE_TAP("Double Tap"),
    LONG_PRESS("Long Press"),
    SWIPE("Swipe"),
    WAIT("Wait"),
    TEXT_INPUT("Text Input"),
    CLIPBOARD_PASTE("Clipboard Paste"),
    OPEN_UNREAD_CHATS("Open Unread Chats"),
    PLAY_VIDEO_AUDIO("Gallery Video Audio"),
    SYSTEM_BACK("Back"),
    SYSTEM_HOME("Home"),
    SYSTEM_RECENTS("Recent Apps")
}
