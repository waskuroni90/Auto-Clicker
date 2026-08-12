package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AutoClickerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "AutoClickerAccessibilityService Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing if node inspection is required
    }

    override fun onInterrupt() {
        Log.d(TAG, "AutoClickerAccessibilityService Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    suspend fun performTap(x: Float, y: Float, durationMs: Long = 60L): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val strokeDuration = durationMs.coerceAtLeast(10L)
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + 0.5f, y + 0.5f)
        }
        performGesturePathWithRetry(path, strokeDuration, maxRetries = 3, targetX = x, targetY = y)
    }

    suspend fun performDoubleTap(x: Float, y: Float): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val path1 = Path().apply {
            moveTo(x, y)
            lineTo(x + 0.5f, y + 0.5f)
        }
        val res1 = performGesturePathWithRetry(path1, 50L, maxRetries = 3, targetX = x, targetY = y)
        kotlinx.coroutines.delay(80L)
        val path2 = Path().apply {
            moveTo(x, y)
            lineTo(x + 0.5f, y + 0.5f)
        }
        val res2 = performGesturePathWithRetry(path2, 50L, maxRetries = 3, targetX = x, targetY = y)
        res1 || res2
    }

    suspend fun performLongPress(x: Float, y: Float, durationMs: Long = 1000L): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + 0.5f, y + 0.5f)
        }
        performGesturePathWithRetry(path, durationMs.coerceAtLeast(300L), maxRetries = 2, targetX = x, targetY = y)
    }

    suspend fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        performGesturePathWithRetry(path, durationMs.coerceAtLeast(50L), maxRetries = 2)
    }

    private suspend fun performGesturePathWithRetry(
        path: Path,
        durationMs: Long,
        maxRetries: Int = 3,
        targetX: Float? = null,
        targetY: Float? = null
    ): Boolean {
        for (attempt in 1..maxRetries) {
            val success = performGesturePath(path, durationMs)
            if (success) return true
            if (attempt < maxRetries) {
                kotlinx.coroutines.delay(25L * attempt)
            }
        }
        // Fallback to Accessibility Node ACTION_CLICK if screen coordinate touch dispatch was rejected
        if (targetX != null && targetY != null) {
            return clickAccessibilityNodeAt(targetX, targetY)
        }
        return false
    }

    private fun clickAccessibilityNodeAt(x: Float, y: Float): Boolean {
        val root = rootInActiveWindow ?: return false
        val targetNode = findClickableNodeAt(root, x.toInt(), y.toInt())
        if (targetNode != null) {
            val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) return true
        }
        return false
    }

    private fun findClickableNodeAt(node: AccessibilityNodeInfo?, x: Int, y: Int): AccessibilityNodeInfo? {
        if (node == null) return null
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        if (!rect.contains(x, y)) return null

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findClickableNodeAt(child, x, y)
            if (result != null) return result
        }

        if (node.isClickable) return node
        return null
    }

    suspend fun performGesturePath(path: Path, durationMs: Long): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(1L))
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(stroke)
            val gesture = gestureBuilder.build()

            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            val dispatched = dispatchGesture(gesture, callback, null)
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    suspend fun inputText(text: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        if (text.isEmpty()) return@withContext true

        var success = false
        for (attempt in 1..3) {
            val root = rootInActiveWindow
            val focusedNode = findFocusedEditableNode(root)

            if (focusedNode != null) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                if (success) break
            }
            kotlinx.coroutines.delay(150L)
        }

        if (!success) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AutoClickerInput", text)
                clipboard.setPrimaryClip(clip)
                success = pasteClipboard()
            } catch (e: Exception) {
                Log.e(TAG, "Failed text input fallback", e)
            }
        }

        success
    }

    suspend fun pasteClipboard(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val root = rootInActiveWindow
        val focusedNode = findFocusedEditableNode(root)

        focusedNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE) ?: false
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        val inputFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (inputFocus != null) return inputFocus

        val accessibilityFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (accessibilityFocus != null) return accessibilityFocus

        return searchFocusedRecursively(root)
    }

    private fun searchFocusedRecursively(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused || node.isAccessibilityFocused) return node
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchFocusedRecursively(child)
            if (result != null) return result
        }
        return null
    }

    suspend fun clickSendButton(fallbackX: Float, fallbackY: Float): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        val root = rootInActiveWindow
        if (root != null) {
            val sendNode = findSendButtonNode(root)
            if (sendNode != null) {
                val rect = android.graphics.Rect()
                sendNode.getBoundsInScreen(rect)
                val centerX = rect.centerX().toFloat()
                val centerY = rect.centerY().toFloat()

                // 1. Try direct ACTION_CLICK on send node or its clickable parent
                var clicked = sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked && sendNode.parent?.isClickable == true) {
                    clicked = sendNode.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }

                if (clicked) {
                    Log.d(TAG, "Successfully clicked Send button via Accessibility ACTION_CLICK")
                    return@withContext true
                } else if (centerX > 0 && centerY > 0) {
                    Log.d(TAG, "Tapping Send button center coordinates ($centerX, $centerY)")
                    return@withContext performTap(centerX, centerY, 60L)
                }
            }
        }

        // 2. Fallback to coordinate tap at configured position
        Log.d(TAG, "Performing fallback tap for Send button at ($fallbackX, $fallbackY)")
        performTap(fallbackX, fallbackY, 60L)
    }

    private fun findSendButtonNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val isSendMatch = contentDesc.contains("send") || contentDesc.contains("পাঠান") || contentDesc.contains("পাঠাও") ||
                contentDesc.contains("পোস্ট") || contentDesc.contains("ارسال") || contentDesc.contains("enviar") ||
                contentDesc.contains("paper_plane") || contentDesc.contains("paperplane") ||
                text.equals("send", ignoreCase = true) || text.contains("পাঠান") || text.contains("পাঠাও") ||
                viewId.contains("send") || viewId.contains("composer_send") || viewId.contains("btn_send") || viewId.contains("send_button")

        if (isSendMatch) {
            if (node.isClickable) return node
            if (node.parent?.isClickable == true) return node.parent
            return node
        }

        // Recursively search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButtonNode(child)
            if (result != null) return result
        }

        return null
    }

    suspend fun performBack(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    suspend fun performHome(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    suspend fun performRecents(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    companion object {
        @Volatile
        var instance: AutoClickerAccessibilityService? = null
            private set

        private const val TAG = "AutoClickerAccessibility"
    }
}
