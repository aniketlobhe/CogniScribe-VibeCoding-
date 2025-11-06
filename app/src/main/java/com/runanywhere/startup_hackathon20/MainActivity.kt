package com.runanywhere.startup_hackathon20

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Startup_hackathon20Theme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "start"
                ) {
                    composable("start") {
                        StartScreen(navController = navController)
                    }
                    composable("chat/{ageGroup}") { backStackEntry ->
                        val ageGroup = backStackEntry.arguments?.getString("ageGroup") ?: ""
                        val context = LocalContext.current
                        val viewModel: ChatViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return ChatViewModel(context) as T
                                }
                            }
                        )
                        ChatScreen(viewModel = viewModel, ageGroup = ageGroup)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, ageGroup: String) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val currentModelId by viewModel.currentModelId.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val aiName by viewModel.aiName.collectAsState()

    LaunchedEffect(ageGroup) {
        viewModel.initializeChat(ageGroup)
    }

    var inputText by remember { mutableStateOf("") }
    var showModelSelector by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val isSpeechMode by viewModel.isSpeechMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (ageGroup) {
                                "Ages 4-7" -> "📚 The Storyteller"
                                "Ages 8-12" -> "🔍 The Explorer"
                                "Ages 13-16+" -> "🎓 The Problem Solver"
                                else -> "CogniScribe"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (ageGroup.isNotEmpty()) {
                            Text(
                                text = "for $ageGroup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // 3-dot menu
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Show current model info or download progress
                            if (downloadProgress != null) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                "Downloading Model...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { downloadProgress ?: 0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "${((downloadProgress ?: 0f) * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    onClick = { }
                                )
                                Divider()
                            } else if (currentModelId != null) {
                                val currentModel = availableModels.firstOrNull { it.id == currentModelId }
                                if (currentModel != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    "Current Model",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    currentModel.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = { }
                                    )
                                    Divider()
                                }
                            }
                            // Speech/Text Mode Toggle
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeechMode) Icons.Default.Mic else Icons.Default.Stop,
                                            contentDescription = null,
                                            tint = if (isSpeechMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(if (isSpeechMode) "Speech Mode (On)" else "Text Mode")
                                    }
                                },
                                onClick = {
                                    viewModel.toggleSpeechMode()
                                }
                            )
                            Divider()
                            // Models Option
                            DropdownMenuItem(
                                text = { Text("Manage Models") },
                                onClick = {
                                    showMenu = false
                                    showModelSelector = !showModelSelector
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Model selector (collapsible)
                AnimatedVisibility(
                    visible = showModelSelector,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ModelSelector(
                        models = availableModels,
                        currentModelId = currentModelId,
                        onDownload = { modelId -> viewModel.downloadModel(modelId) },
                        onLoad = { modelId -> viewModel.loadModel(modelId) },
                        onRefresh = { viewModel.refreshModels() }
                    )
                }

                // Messages List
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = false
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message,
                            viewModel,
                            aiName = aiName,
                            onReply = { message -> viewModel.replyToMessage(message) }
                        )
                    }
                }

                // Auto-scroll to bottom when new messages arrive
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                }

                // 1. Get context and isListening state
                val context = LocalContext.current
                val isListening by viewModel.isListening.collectAsState()
                val replyingTo by viewModel.replyingToMessage.collectAsState()

                // 2. Create a new permission launcher.
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        // Permission is granted, NOW start listening
                        Log.d("STT", "Permission granted! Starting listening.")
                        viewModel.startListening()
                    } else {
                        // Permission was denied
                        Toast.makeText(
                            context,
                            "Permission denied. STT will not work.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // 3. WhatsApp-style input section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Speech recognition preview (shown when listening or recognized text is available)
                    val speechText by viewModel.speechRecognitionText.collectAsState()
                    AnimatedVisibility(
                        visible = speechText.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = if (isListening) "Listening: $speechText" else "You said: $speechText",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )

                                // Send button for recognized text
                                if (!isListening && speechText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.sendMessage(speechText)
                                            // Clear speech recognition text after sending
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Close button
                                    IconButton(
                                        onClick = {
                                            viewModel.clearSpeechRecognitionText()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reply preview (shown when replying to a message)
                    AnimatedVisibility(
                        visible = replyingTo != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Reply indicator line
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Reply content
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (replyingTo?.isUser == true) "You" else aiName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    Text(
                                        text = replyingTo?.text ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Close button
                                IconButton(
                                    onClick = { viewModel.cancelReply() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Cancel reply",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Input row: TextField + dynamic Send/Mic button
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                placeholder = { Text("Type a message…") },
                                singleLine = true,
                                enabled = !isListening,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                            if (inputText.isNotBlank()) {
                                // Show Send button when typing with subtle animation
                                val scale by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = FastOutSlowInEasing
                                    ),
                                    label = "sendScale"
                                )

                                IconButton(
                                    onClick = {
                                        val text = inputText.trim()
                                        if (text.isNotEmpty() && currentModelId != null) {
                                            viewModel.sendMessage(text)
                                            inputText = ""
                                        }
                                    },
                                    enabled = !isLoading && currentModelId != null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .scale(scale)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentModelId != null)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = if (currentModelId != null)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                // Show Mic/Stop button with pulsing animation when listening
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (isListening) 1.05f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulseScale"
                                )

                                IconButton(
                                    onClick = {
                                        Log.d(
                                            "STT",
                                            "Mic button clicked. isListening = $isListening"
                                        )
                                        if (isListening) {
                                            viewModel.stopListening()
                                        } else {
                                            // Check for permission FIRST.
                                            when (ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.RECORD_AUDIO
                                            )) {
                                                PackageManager.PERMISSION_GRANTED -> {
                                                    Log.d(
                                                        "STT",
                                                        "Permission was already granted. Starting listening."
                                                    )
                                                    viewModel.startListening()
                                                }

                                                else -> {
                                                    Log.d(
                                                        "STT",
                                                        "Permission not granted. Launching request."
                                                    )
                                                    requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(
                                            if (isListening)
                                                MaterialTheme.colorScheme.errorContainer
                                            else
                                                MaterialTheme.colorScheme.primaryContainer
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = if (isListening) "Stop listening" else "Start listening",
                                        tint = if (isListening)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    viewModel: ChatViewModel? = null,
    aiName: String,
    onReply: ((ChatMessage) -> Unit)? = null
) {
    // Subtle fade-in animation instead of bounce
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearOutSlowInEasing
        ),
        label = "alpha"
    )

    val slideOffset by animateDpAsState(
        targetValue = if (visible) 0.dp else if (message.isUser) 20.dp else (-20).dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "slide"
    )

    // Swipe state
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeLeft = -120f
    val maxSwipeRight = 120f

    // Animated offset with spring physics for smooth return
    val animatedOffsetX by animateDpAsState(
        targetValue = offsetX.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipeOffset"
    )

    // TTS state
    val currentlyPlaying by (viewModel?.currentlyPlayingMessageId?.collectAsState()
        ?: remember { mutableStateOf(null) })
    val ttsState = viewModel?.ttsPlaybackState?.collectAsState()?.value?.get(message.id)
    val isPlaying = ttsState?.isPlaying == true
    val isPaused = ttsState?.isPaused == true
    val isSpeechMode by (viewModel?.isSpeechMode?.collectAsState()
        ?: remember { mutableStateOf(true) })

    // Show TTS controls only for AI messages
    var showTTSControls by remember { mutableStateOf(!message.isUser && !isSpeechMode) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = slideOffset)
            .alpha(alpha)
            .padding(horizontal = 8.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box {
            // Swipeable message card
            Card(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .offset(x = animatedOffsetX)
                    .pointerInput(Unit) {
                        if (!message.isUser && viewModel != null) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        offsetX < -50 -> {
                                            // Swipe left - Toggle TTS (Pause/Resume)
                                            viewModel.toggleTTSForMessage(message.id, message.text)
                                        }

                                        offsetX > 50 -> {
                                            // Swipe right - Reply
                                            onReply?.invoke(message)
                                        }
                                    }
                                    offsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val newOffset = offsetX + dragAmount
                                    offsetX = newOffset.coerceIn(maxSwipeLeft, maxSwipeRight)
                                }
                            )
                        }
                    },
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isUser) 20.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 20.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header with avatar and name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (message.isUser)
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (message.isUser) "👤" else "🤖",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = if (message.isUser) "You" else aiName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (message.isUser)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // TTS Controls Bar (for text mode or manual control)
                    if (!message.isUser && showTTSControls && viewModel != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Pause button
                            IconButton(
                                onClick = {
                                    when {
                                        isPlaying -> viewModel.pauseTTS(message.id)
                                        isPaused -> viewModel.resumeTTS(message.id, message.text)
                                        else -> viewModel.playTTS(message.id, message.text)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = if (isPlaying)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Replay from start button
                            IconButton(
                                onClick = {
                                    viewModel.playTTS(message.id, message.text, 0)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Replay from start",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Playing indicator
                            if (isPlaying) {
                                Text(
                                    text = "🔊 Playing...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                            } else if (isPaused) {
                                Text(
                                    text = "⏸️ Paused",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Divider(
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Message text with selection for "play from here"
                    if (!message.isUser && viewModel != null) {
                        var showSpeakMenu by remember { mutableStateOf(false) }
                        var longPressOffset by remember { mutableStateOf(0) }
                        val context = LocalContext.current

                        Box {
                            SelectionContainer {
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(message.text) {
                                            detectTapGestures(
                                                onLongPress = { offset ->
                                                    // Simpler approach: use percentage of height to estimate position
                                                    val textLength = message.text.length
                                                    val boxHeight = size.height.toFloat()
                                                    val boxWidth = size.width.toFloat()

                                                    // Calculate what percentage of the text box was tapped
                                                    val verticalPercentage =
                                                        (offset.y / boxHeight).coerceIn(0f, 1f)
                                                    val horizontalPercentage =
                                                        (offset.x / boxWidth).coerceIn(0f, 1f)

                                                    // Estimate position based on vertical percentage primarily
                                                    // Add a small adjustment based on horizontal position
                                                    val estimatedPosition =
                                                        (textLength * verticalPercentage +
                                                                textLength * horizontalPercentage * 0.1f).toInt()
                                                    val clampedPosition =
                                                        estimatedPosition.coerceIn(
                                                            0,
                                                            textLength - 1
                                                        )

                                                    longPressOffset = clampedPosition

                                                    Log.d("LongPress", "=== Long Press Debug ===")
                                                    Log.d(
                                                        "LongPress",
                                                        "Touch: x=${offset.x}, y=${offset.y}"
                                                    )
                                                    Log.d(
                                                        "LongPress",
                                                        "Box: width=$boxWidth, height=$boxHeight"
                                                    )
                                                    Log.d(
                                                        "LongPress",
                                                        "Percentages: vertical=$verticalPercentage, horizontal=$horizontalPercentage"
                                                    )
                                                    Log.d("LongPress", "Text length: $textLength")
                                                    Log.d(
                                                        "LongPress",
                                                        "Estimated position: $estimatedPosition -> clamped: $clampedPosition"
                                                    )

                                                    val previewStart = clampedPosition
                                                    val previewEnd =
                                                        minOf(clampedPosition + 40, textLength)
                                                    if (previewEnd > previewStart) {
                                                        Log.d(
                                                            "LongPress",
                                                            "Text at position: '${
                                                                message.text.substring(
                                                                    previewStart,
                                                                    previewEnd
                                                                )
                                                            }'"
                                                        )
                                                    }

                                                    showSpeakMenu = true
                                                }
                                            )
                                        }
                                )
                            }

                            // Dropdown menu for "Speak from here"
                            DropdownMenu(
                                expanded = showSpeakMenu,
                                onDismissRequest = { showSpeakMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("Speak from here")
                                        }
                                    },
                                    onClick = {
                                        // Find the start of the word at longPressOffset
                                        val wordStart = findWordStart(message.text, longPressOffset)
                                        val textToSpeak = message.text.substring(wordStart)
                                        val wordPreview = textToSpeak.take(40)

                                        Log.d("SpeakFromHere", "=== Speak From Here ===")
                                        Log.d("SpeakFromHere", "longPressOffset: $longPressOffset")
                                        Log.d("SpeakFromHere", "wordStart: $wordStart")
                                        Log.d(
                                            "SpeakFromHere",
                                            "Full text length: ${message.text.length}"
                                        )
                                        Log.d(
                                            "SpeakFromHere",
                                            "Text to speak length: ${textToSpeak.length}"
                                        )
                                        Log.d("SpeakFromHere", "Preview: '$wordPreview'")
                                        
                                        Toast.makeText(
                                            context,
                                            "Pos: $wordStart/${message.text.length} - $wordPreview",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        viewModel.playTTS(message.id, message.text, wordStart)
                                        showSpeakMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Replay,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("Speak full text")
                                        }
                                    },
                                    onClick = {
                                        viewModel.playTTS(message.id, message.text, 0)
                                        showSpeakMenu = false
                                    }
                                )
                            }
                        }
                    } else if (!message.isUser) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isUser)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Swipe hint icons (shown while swiping)
            if (!message.isUser && abs(offsetX) > 20) {
                val hintAlpha by animateFloatAsState(
                    targetValue = (abs(offsetX) / 100f).coerceIn(0f, 0.7f),
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    ),
                    label = "hintAlpha"
                )

                if (offsetX < 0) {
                    // Left swipe - Toggle TTS (Pause/Resume)
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(24.dp)
                            .alpha(hintAlpha)
                    )
                } else {
                    // Right swipe - Reply
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(24.dp)
                            .alpha(hintAlpha)
                    )
                }
            }
        }
    }
}

fun findWordStart(text: String, position: Int): Int {
    if (position < 0 || position >= text.length) return 0

    var start = position

    // If we're on a space or punctuation, move forward to the next word
    while (start < text.length && (text[start] == ' ' || text[start] == '\n' || text[start] in ".,!?;:")) {
        start++
    }

    // If we went past the end, reset to original position
    if (start >= text.length) {
        start = position
    }

    // Now move back to find the start of the current word
    while (start > 0 && text[start - 1] != ' ' && text[start - 1] != '\n' && text[start - 1] !in ".,!?;:") {
        start--
    }

    Log.d("WordStart", "Input position: $position, WordStart: $start, Text length: ${text.length}")
    Log.d(
        "WordStart",
        "Character at position: '${if (position < text.length) text[position] else "N/A"}'"
    )
    Log.d("WordStart", "Word at start: ${text.substring(start, minOf(start + 30, text.length))}")

    return start
}

@Composable
fun ModelSelector(
    models: List<com.runanywhere.sdk.models.ModelInfo>,
    currentModelId: String?,
    onDownload: (String) -> Unit,
    onLoad: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Available Models",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (models.isEmpty()) {
                Text(
                    text = "No models available. Initializing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(models) { model ->
                        ModelItem(
                            model = model,
                            isLoaded = model.id == currentModelId,
                            onDownload = { onDownload(model.id) },
                            onLoad = { onLoad(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelItem(
    model: com.runanywhere.sdk.models.ModelInfo,
    isLoaded: Boolean,
    onDownload: () -> Unit,
    onLoad: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLoaded)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleSmall
            )

            if (isLoaded) {
                Text(
                    text = "✓ Currently Loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        enabled = !model.isDownloaded
                    ) {
                        Text(if (model.isDownloaded) "Downloaded" else "Download")
                    }

                    Button(
                        onClick = onLoad,
                        modifier = Modifier.weight(1f),
                        enabled = model.isDownloaded
                    ) {
                        Text("Load")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Startup_hackathon20Theme {
        val context = LocalContext.current
        val viewModel: ChatViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(context) as T
                }
            }
        )
        ChatScreen(viewModel = viewModel, ageGroup = "")
    }
}