package com.example.gesture

import android.graphics.Path
import com.example.accessibility.AutoClickerAccessibilityService
import com.example.model.ClickTarget
import com.example.model.TargetType
import kotlin.random.Random

object GestureExecutor {

    suspend fun executeTarget(
        target: ClickTarget,
        randomOffsetPx: Int = 0
    ): Boolean {
        val service = AutoClickerAccessibilityService.instance ?: return false

        // Anti-detection random offset jitter calculation
        val offsetX = if (randomOffsetPx > 0) Random.nextInt(-randomOffsetPx, randomOffsetPx + 1) else 0
        val offsetY = if (randomOffsetPx > 0) Random.nextInt(-randomOffsetPx, randomOffsetPx + 1) else 0

        val startX = (target.xPx + offsetX).coerceAtLeast(0f)
        val startY = (target.yPx + offsetY).coerceAtLeast(0f)

        return when (target.type) {
            TargetType.SINGLE_TAP -> {
                service.performTap(startX, startY, target.durationMs.coerceAtLeast(10L))
            }
            TargetType.LONG_PRESS -> {
                val duration = target.durationMs.coerceAtLeast(300L)
                service.performLongPress(startX, startY, duration)
            }
            TargetType.DOUBLE_TAP -> {
                service.performDoubleTap(startX, startY)
            }
            TargetType.SWIPE -> {
                val endX = (target.swipeEndXPx + offsetX).coerceAtLeast(0f)
                val endY = (target.swipeEndYPx + offsetY).coerceAtLeast(0f)
                service.performSwipe(startX, startY, endX, endY, target.durationMs.coerceAtLeast(50L))
            }
            TargetType.WAIT -> {
                if (target.delayMs > 0) {
                    kotlinx.coroutines.delay(target.delayMs)
                }
                true
            }
            TargetType.TEXT_INPUT -> {
                service.performTap(startX, startY, 50L)
                kotlinx.coroutines.delay(200L)
                val resolvedText = com.example.automation.MacroContext.resolveVariables(target.textContent)
                service.inputText(resolvedText)
            }
            TargetType.CLIPBOARD_PASTE -> {
                service.performTap(startX, startY, 50L)
                kotlinx.coroutines.delay(200L)
                service.pasteClipboard()
            }
            TargetType.OPEN_UNREAD_CHATS -> {
                val unreadSettings = com.example.model.UnreadChatSettings(
                    minUnreadCount = target.minUnreadCount,
                    processOrder = target.processOrder,
                    maxChatsToOpen = target.maxChatsToOpen,
                    skipPinnedChats = target.skipPinnedChats,
                    skipMutedChats = target.skipMutedChats,
                    autoScroll = target.autoScroll,
                    stopAtEnd = target.stopAtEnd
                )
                com.example.automation.UnreadChatDetector.openNextUnreadChat(service, unreadSettings)
            }
            TargetType.PLAY_VIDEO_AUDIO -> {
                if (target.mediaUri.isNotEmpty()) {
                    var mediaPlayer: android.media.MediaPlayer? = null
                    try {
                        val context = service.applicationContext
                        mediaPlayer = com.example.utils.MediaStorageManager.playAudio(context, target.mediaUri)
                        val playDurationMs = target.durationMs.coerceAtLeast(100L)
                        kotlinx.coroutines.delay(playDurationMs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val fallbackDuration = target.durationMs.coerceAtLeast(100L)
                        kotlinx.coroutines.delay(fallbackDuration)
                    } finally {
                        try {
                            if (mediaPlayer?.isPlaying == true) {
                                mediaPlayer.stop()
                            }
                            mediaPlayer?.release()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    true
                } else {
                    val context = service.applicationContext
                    com.example.utils.MediaStorageManager.playFallbackBeep()
                    val fallbackDuration = target.durationMs.coerceAtLeast(100L)
                    kotlinx.coroutines.delay(fallbackDuration)
                    true
                }
            }
            TargetType.SYSTEM_BACK -> {
                service.performBack()
            }
            TargetType.SYSTEM_HOME -> {
                service.performHome()
            }
            TargetType.SYSTEM_RECENTS -> {
                service.performRecents()
            }
            TargetType.AUTO_CLICK_SEND -> {
                service.clickSendButton(startX, startY)
            }
        }
    }
}
