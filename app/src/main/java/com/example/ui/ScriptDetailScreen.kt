package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClickTarget
import com.example.model.ScriptModel
import com.example.model.TargetType

@Composable
fun ScriptDetailScreen(
    initialScript: ScriptModel?,
    onSaveScript: (ScriptModel) -> Unit,
    onBack: () -> Unit
) {
    var scriptName by remember { mutableStateOf(initialScript?.name ?: "New Macro Script") }
    var repeatCountStr by remember { mutableStateOf((initialScript?.repeatCount ?: -1).toString()) }
    var repeatIntervalStr by remember { mutableStateOf((initialScript?.repeatIntervalMs ?: 500L).toString()) }
    var randomOffsetStr by remember { mutableStateOf((initialScript?.randomOffsetPx ?: 0).toString()) }

    var targets by remember {
        mutableStateOf(initialScript?.targets ?: listOf(
            ClickTarget(order = 1, type = TargetType.SINGLE_TAP, delayMs = 500L)
        ))
    }

    var showAddActionDialog by remember { mutableStateOf(false) }

    if (showAddActionDialog) {
        ActionTypePickerDialog(
            currentType = null,
            onTypeSelected = { selectedType ->
                val newOrder = targets.size + 1
                targets = targets + ClickTarget(
                    order = newOrder,
                    type = selectedType,
                    delayMs = 500L,
                    label = "${selectedType.displayName} #$newOrder"
                )
                showAddActionDialog = false
            },
            onDismiss = { showAddActionDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121824))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (initialScript == null) "Create Macro Script" else "Edit Macro Script",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val script = ScriptModel(
                            id = initialScript?.id ?: 0,
                            name = scriptName,
                            repeatCount = repeatCountStr.toIntOrNull() ?: -1,
                            repeatIntervalMs = repeatIntervalStr.toLongOrNull() ?: 500L,
                            randomOffsetPx = randomOffsetStr.toIntOrNull() ?: 0,
                            createdAt = initialScript?.createdAt ?: System.currentTimeMillis(),
                            isFavorite = initialScript?.isFavorite ?: false,
                            targets = targets
                        )
                        onSaveScript(script)
                        onBack()
                    },
                    modifier = Modifier.testTag("btn_save_script"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF0F172A))
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Save", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Script Name Input
            OutlinedTextField(
                value = scriptName,
                onValueChange = { scriptName = it },
                label = { Text("Script Name", color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_script_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Parameters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = repeatCountStr,
                    onValueChange = { repeatCountStr = it },
                    label = { Text("Repeat (-1=Inf)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_repeat_count"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = repeatIntervalStr,
                    onValueChange = { repeatIntervalStr = it },
                    label = { Text("Loop Delay (ms)", color = Color(0xFF94A3B8), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_repeat_interval"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Automation Queue Sequence Visualizer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Automation Queue Flow",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (targets.isEmpty()) {
                        Text(
                            text = "Queue is empty. Add action buttons below to build sequence.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            targets.forEachIndexed { idx, target ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (target.type) {
                                                    TargetType.SINGLE_TAP -> Color(0xFF00E5FF)
                                                    TargetType.DOUBLE_TAP -> Color(0xFF38BDF8)
                                                    TargetType.LONG_PRESS -> Color(0xFF818CF8)
                                                    TargetType.SWIPE -> Color(0xFFF59E0B)
                                                    TargetType.WAIT -> Color(0xFF94A3B8)
                                                    TargetType.TEXT_INPUT -> Color(0xFFA855F7)
                                                    TargetType.CLIPBOARD_PASTE -> Color(0xFF10B981)
                                                    TargetType.OPEN_UNREAD_CHATS -> Color(0xFF10B981)
                                                    TargetType.PLAY_VIDEO_AUDIO -> Color(0xFFF59E0B)
                                                    TargetType.AUTO_CLICK_SEND -> Color(0xFF0084FF)
                                                    else -> Color(0xFFEC4899)
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Button ${target.order}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "${target.type.displayName} (Wait ${target.delayMs}ms)",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }

                                if (idx < targets.size - 1) {
                                    Text(
                                        text = "  ↓ delayBefore ${targets[idx + 1].delayMs}ms",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target List Title & Add Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Action Buttons (${targets.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showAddActionDialog = true },
                        modifier = Modifier.testTag("btn_add_action_target"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Action Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.AUTO_CLICK_SEND,
                                delayMs = 300L,
                                label = "Auto Send #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_auto_send_target")
                    ) {
                        Text(text = "✈️ + Send Button", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0084FF))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.PLAY_VIDEO_AUDIO,
                                delayMs = 500L,
                                durationMs = 3000L,
                                label = "Gallery Video #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_gallery_video_target")
                    ) {
                        Text(text = "🎬 + Gallery Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.OPEN_UNREAD_CHATS,
                                delayMs = 500L,
                                label = "Open Unread Chats #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_unread_chats_target")
                    ) {
                        Text(text = "📩 + Unread Chats", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            val newOrder = targets.size + 1
                            targets = targets + ClickTarget(
                                order = newOrder,
                                type = TargetType.TEXT_INPUT,
                                delayMs = 500L,
                                label = "Text Input #$newOrder"
                            )
                        },
                        modifier = Modifier.testTag("btn_add_text_target")
                    ) {
                        Icon(imageVector = Icons.Default.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Text", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(targets) { index, target ->
                    TargetDetailCard(
                        target = target,
                        onUpdate = { updatedTarget ->
                            val mutable = targets.toMutableList()
                            mutable[index] = updatedTarget
                            targets = mutable
                        },
                        onDelete = {
                            val mutable = targets.toMutableList()
                            mutable.removeAt(index)
                            // Reorder remaining
                            targets = mutable.mapIndexed { idx, item -> item.copy(order = idx + 1) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionTypePickerDialog(
    currentType: TargetType? = null,
    onTypeSelected: (TargetType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartButton,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Action Type",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TargetType.values().forEach { typeOption ->
                    val isSelected = typeOption == currentType
                    val optionColor = when (typeOption) {
                        TargetType.SINGLE_TAP -> Color(0xFF00E5FF)
                        TargetType.DOUBLE_TAP -> Color(0xFF38BDF8)
                        TargetType.LONG_PRESS -> Color(0xFF818CF8)
                        TargetType.SWIPE -> Color(0xFFF59E0B)
                        TargetType.WAIT -> Color(0xFF94A3B8)
                        TargetType.TEXT_INPUT -> Color(0xFFA855F7)
                        TargetType.CLIPBOARD_PASTE -> Color(0xFF10B981)
                        TargetType.OPEN_UNREAD_CHATS -> Color(0xFF10B981)
                        TargetType.PLAY_VIDEO_AUDIO -> Color(0xFFF59E0B)
                        TargetType.AUTO_CLICK_SEND -> Color(0xFF0084FF)
                        else -> Color(0xFFEC4899)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTypeSelected(typeOption)
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) optionColor.copy(alpha = 0.2f) else Color(0xFF0F172A)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) optionColor else Color(0xFF334155)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(optionColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = typeOption.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = optionColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TargetDetailCard(
    target: ClickTarget,
    onUpdate: (ClickTarget) -> Unit,
    onDelete: () -> Unit
) {
    var showPickerModal by remember { mutableStateOf(false) }

    if (showPickerModal) {
        ActionTypePickerDialog(
            currentType = target.type,
            onTypeSelected = { selectedType ->
                onUpdate(target.copy(type = selectedType, label = "${selectedType.displayName} #${target.order}"))
                showPickerModal = false
            },
            onDismiss = { showPickerModal = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${target.order} ${target.type.displayName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (target.type) {
                        TargetType.SINGLE_TAP -> Color(0xFF00E5FF)
                        TargetType.DOUBLE_TAP -> Color(0xFF38BDF8)
                        TargetType.LONG_PRESS -> Color(0xFF818CF8)
                        TargetType.SWIPE -> Color(0xFFF59E0B)
                        TargetType.WAIT -> Color(0xFF94A3B8)
                        TargetType.TEXT_INPUT -> Color(0xFFA855F7)
                        TargetType.CLIPBOARD_PASTE -> Color(0xFF10B981)
                        TargetType.OPEN_UNREAD_CHATS -> Color(0xFF10B981)
                        TargetType.PLAY_VIDEO_AUDIO -> Color(0xFFF59E0B)
                        TargetType.AUTO_CLICK_SEND -> Color(0xFF0084FF)
                        else -> Color(0xFFEC4899)
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showPickerModal = true },
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(text = target.type.displayName, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.testTag("btn_delete_target_${target.order}")) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Target", tint = Color(0xFFEF4444))
                    }
                }
            }

            if (target.type == TargetType.PLAY_VIDEO_AUDIO) {
                Spacer(modifier = Modifier.height(8.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                var isSavingFile by remember { mutableStateOf(false) }

                val videoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: android.net.Uri? ->
                    uri?.let {
                        isSavingFile = true
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val savedPath = com.example.utils.MediaStorageManager.saveUriToInternalStorage(context, it)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isSavingFile = false
                                if (!savedPath.isNullOrEmpty()) {
                                    onUpdate(target.copy(mediaUri = savedPath))
                                    android.widget.Toast.makeText(context, "✓ Video Audio saved locally for 100% reliable playback!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onUpdate(target.copy(mediaUri = it.toString()))
                                    android.widget.Toast.makeText(context, "⚠️ Video attached via direct Uri", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🎬 Gallery Video Audio Player",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            text = "Selected video is saved to local app storage for 100% guaranteed speaker sound when button action triggers.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isSavingFile) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFF59E0B), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving video audio to app storage...", fontSize = 11.sp, color = Color(0xFFF59E0B))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { videoLauncher.launch(arrayOf("video/*", "audio/*")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF0F172A))
                            ) {
                                Text("📁 Select Gallery Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (target.mediaUri.isNotEmpty()) {
                                        val player = com.example.utils.MediaStorageManager.playAudio(context, target.mediaUri)
                                        android.widget.Toast.makeText(context, "▶️ Testing speaker audio...", android.widget.Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            kotlinx.coroutines.delay(3000L)
                                            try { player?.stop(); player?.release() } catch (_: Exception) {}
                                        }
                                    } else {
                                        com.example.utils.MediaStorageManager.playFallbackBeep()
                                        android.widget.Toast.makeText(context, "🔊 Testing speaker beep sound", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White)
                            ) {
                                Text("▶️ Test Sound", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (target.mediaUri.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { onUpdate(target.copy(mediaUri = "")) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                ) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                        }

                        if (target.mediaUri.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✓ Video Audio Ready: ${target.mediaUri.substringAfterLast('/')}",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                maxLines = 2
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ No video selected. Tap 'Select Gallery Video' above.",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            if (target.type == TargetType.AUTO_CLICK_SEND) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF0084FF))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "✈️ Auto Send Button Clicker",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0084FF)
                        )
                        Text(
                            text = "Detects and clicks the Send button icon in Messenger, Instagram, WhatsApp, SMS, or Facebook automatically.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            if (target.type == TargetType.TEXT_INPUT) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Text Content (Multi-line & Emoji supported):",
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = target.textContent,
                    onValueChange = { newText ->
                        // Auto-saves immediately
                        onUpdate(target.copy(textContent = newText))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_text_content_${target.order}"),
                    placeholder = { Text("Enter text or emojis to input...", color = Color(0xFF64748B)) },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Text(
                    text = "✓ Saved automatically",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = target.delayMs.toString(),
                    onValueChange = {
                        val delay = it.toLongOrNull() ?: target.delayMs
                        onUpdate(target.copy(delayMs = delay))
                    },
                    label = { Text("Delay Before (ms)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = target.durationMs.toString(),
                    onValueChange = {
                        val duration = it.toLongOrNull() ?: target.durationMs
                        onUpdate(target.copy(durationMs = duration))
                    },
                    label = { Text("Duration (ms)", fontSize = 10.sp, color = Color(0xFF94A3B8)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
        }
    }
}

