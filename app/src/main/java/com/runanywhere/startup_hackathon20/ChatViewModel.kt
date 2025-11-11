package com.runanywhere.startup_hackathon20

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import com.runanywhere.sdk.models.enums.ModelCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Simple Message Data Class
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: String = java.util.UUID.randomUUID().toString()
)

// TTS Playback State Data Class
data class TTSPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPosition: Int = 0 // character position in text
)

// ViewModel
class ChatViewModel(private val context: Context) : ViewModel(), TextToSpeech.OnInitListener {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _availableModels = MutableStateFlow<List<ModelInfo>>(emptyList())
    val availableModels: StateFlow<List<ModelInfo>> = _availableModels

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    private val _statusMessage = MutableStateFlow<String>("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _isSpeechMode = MutableStateFlow(false)
    val isSpeechMode = _isSpeechMode.asStateFlow()

    private val _currentlyPlayingMessageId = MutableStateFlow<String?>(null)
    val currentlyPlayingMessageId = _currentlyPlayingMessageId.asStateFlow()

    private val _ttsPlaybackState = MutableStateFlow<Map<String, TTSPlaybackState>>(emptyMap())
    val ttsPlaybackState: StateFlow<Map<String, TTSPlaybackState>> = _ttsPlaybackState

    private val _speechRecognitionText = MutableStateFlow("")
    val speechRecognitionText = _speechRecognitionText.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage = _replyingToMessage.asStateFlow()

    private val _aiName = MutableStateFlow("AI Tutor")
    val aiName = _aiName.asStateFlow()

    private var tts: TextToSpeech? = null
    private var pendingGreeting: String? = null
    private var currentAgeGroup: String = ""
    private var systemPrompt: String = ""

    private var lastSpokenPosition = 0
    private var currentSpeakingMessageId: String? = null

    // Android's built-in speech recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initializeSpeechRecognizer()
        loadAvailableModels()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.ENGLISH

            // Set up utterance progress listener to track playback position
            tts?.setOnUtteranceProgressListener(object :
                android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("TTS", "Started speaking: $utteranceId")
                    currentSpeakingMessageId = utteranceId
                    utteranceId?.let { msgId ->
                        viewModelScope.launch {
                            withContext(Dispatchers.Main) {
                                _currentlyPlayingMessageId.value = msgId
                                _ttsPlaybackState.value =
                                    _ttsPlaybackState.value.toMutableMap().apply {
                                        val currentState = get(msgId) ?: TTSPlaybackState()
                                        put(
                                            msgId,
                                            currentState.copy(isPlaying = true, isPaused = false)
                                        )
                                    }
                            }
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    Log.d("TTS", "Finished speaking: $utteranceId")
                    utteranceId?.let { msgId ->
                        viewModelScope.launch {
                            withContext(Dispatchers.Main) {
                                _currentlyPlayingMessageId.value = null
                                _ttsPlaybackState.value =
                                    _ttsPlaybackState.value.toMutableMap().apply {
                                        put(
                                            msgId,
                                            TTSPlaybackState(
                                                isPlaying = false,
                                                isPaused = false,
                                                currentPosition = 0
                                            )
                                        )
                                    }
                                currentSpeakingMessageId = null
                                lastSpokenPosition = 0
                            }
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e("TTS", "Error speaking: $utteranceId")
                    utteranceId?.let { msgId ->
                        viewModelScope.launch {
                            withContext(Dispatchers.Main) {
                                _currentlyPlayingMessageId.value = null
                                _ttsPlaybackState.value =
                                    _ttsPlaybackState.value.toMutableMap().apply {
                                        put(
                                            msgId,
                                            TTSPlaybackState(
                                                isPlaying = false,
                                                isPaused = false,
                                                currentPosition = 0
                                            )
                                        )
                                    }
                                currentSpeakingMessageId = null
                            }
                        }
                    }
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    // Track the actual position being spoken
                    lastSpokenPosition = start
                    Log.d("TTS", "Speaking range: $start-$end for $utteranceId")
                }
            })

            pendingGreeting?.let {
                tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
                pendingGreeting = null
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("STT", "Ready for speech")
                    _statusMessage.value = "Listening... Speak now"
                }

                override fun onBeginningOfSpeech() {
                    Log.d("STT", "Speech started")
                    _statusMessage.value = "Listening..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - could update UI here
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Not used
                }

                override fun onEndOfSpeech() {
                    Log.d("STT", "Speech ended")
                    _statusMessage.value = "Processing..."
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    Log.e("STT", "Error: $errorMessage (code: $error)")
                    _isListening.value = false
                    _statusMessage.value = "Speech error: $errorMessage"
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Log.d("STT", "Final result: $text")
                        _isListening.value = false
                        _speechRecognitionText.value = text
                        _statusMessage.value = "Recognized: $text"

                        // Send the recognized text as a message
                        sendMessage(text)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Log.d("STT", "Partial result: $text")
                        _speechRecognitionText.value = text
                        _statusMessage.value = "Hearing: $text"
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Not used
                }
            })
        }
    }

    private fun loadAvailableModels() {
        viewModelScope.launch {
            try {
                val models = listAvailableModels()
                _availableModels.value = models
                _statusMessage.value = "Ready - Please download and load a model"
            } catch (e: Exception) {
                _statusMessage.value = "Error loading models: ${e.message}"
            }
        }
    }

    fun downloadModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ViewModel", "Starting download for model: $modelId")
                _statusMessage.value = "Downloading model..."
                _downloadProgress.value = 0f

                RunAnywhere.downloadModel(modelId).collect { progress ->
                    withContext(Dispatchers.Main) {
                        _downloadProgress.value = progress
                        _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                    }
                    Log.d("ViewModel", "Download progress: ${(progress * 100).toInt()}%")
                }

                withContext(Dispatchers.Main) {
                    _downloadProgress.value = null
                    _statusMessage.value = "Download complete! Loading model..."
                }
                Log.d("ViewModel", "Download completed for model: $modelId")

                // Refresh models list to update download status
                refreshModels()

                // Auto-load the model that was just downloaded
                loadModel(modelId)

            } catch (e: Exception) {
                Log.e("ViewModel", "Download failed for model $modelId: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Download failed: ${e.message}"
                    _downloadProgress.value = null
                }
            }
        }
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _statusMessage.value = "Loading model $modelId..."
            }
            Log.d("ViewModel", "Starting to load model: $modelId")
            try {
                val modelToLoad = _availableModels.value.firstOrNull { it.id == modelId }
                if (modelToLoad == null) {
                    Log.e("ViewModel", "Model not found: $modelId")
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error: Model not found."
                    }
                    return@launch
                }

                Log.d(
                    "ViewModel",
                    "Found model: ${modelToLoad.name}, category: ${modelToLoad.category.name}, downloaded: ${modelToLoad.isDownloaded}"
                )

                // Check if model is downloaded
                if (!modelToLoad.isDownloaded) {
                    Log.w("ViewModel", "Model not downloaded yet: ${modelToLoad.name}")
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error: Model not downloaded yet."
                    }
                    return@launch
                }

                // Load LLM models (check for both "LLM" and "LANGUAGE" categories)
                when (modelToLoad.category.name) {
                    "LLM", "LANGUAGE" -> {
                        Log.d("ViewModel", "Loading LLM model: ${modelToLoad.name}")
                        val success = RunAnywhere.loadModel(modelId)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                _currentModelId.value = modelId
                                _statusMessage.value = "Model Loaded: ${modelToLoad.name}"
                                Log.d("ViewModel", "Successfully loaded model: ${modelToLoad.name}")
                            } else {
                                Log.e("ViewModel", "Failed to load model: ${modelToLoad.name}")
                                _statusMessage.value = "Failed to load model"
                            }
                        }
                    }

                    else -> {
                        Log.w("ViewModel", "Skipping model with category: ${modelToLoad.category}")
                        withContext(Dispatchers.Main) {
                            _statusMessage.value =
                                "Skipped ${modelToLoad.category} model (using Android Speech)"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error loading model $modelId: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error loading model: ${e.message}"
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (_currentModelId.value == null) {
            _statusMessage.value = "Please load a model first"
            Log.e("ViewModel", "sendMessage failed: No model loaded. currentModelId is null")
            return
        }

        Log.d("ViewModel", "sendMessage called with text: ${text.take(50)}...")
        Log.d("ViewModel", "Current model ID: ${_currentModelId.value}")

        // Clear speech recognition text if this was sent via speech
        _speechRecognitionText.value = ""

        // Get replied message if any
        val repliedMessage = _replyingToMessage.value

        // Prepend system prompt to user message
        // If replying, add context about the replied message
        val messageWithPrompt = if (repliedMessage != null) {
            """$systemPrompt
            |User is replying to your previous message: "${repliedMessage.text}"
            |User's reply: $text""".trimMargin()
        } else {
            "$systemPrompt $text"
        }

        // Add user message (will include reply reference in UI)
        _messages.value += ChatMessage(text, isUser = true)

        // Clear reply state after sending
        _replyingToMessage.value = null

        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Generate response with streaming
                var assistantResponse = ""
                var responseMessageId: String? = null

                RunAnywhere.generateStream(messageWithPrompt).collect { token ->
                    assistantResponse += token

                    // Update assistant message in real-time
                    val currentMessages = _messages.value.toMutableList()
                    if (currentMessages.lastOrNull()?.isUser == false) {
                        val lastMessage = currentMessages.last()
                        responseMessageId = lastMessage.id
                        currentMessages[currentMessages.lastIndex] =
                            lastMessage.copy(text = assistantResponse)
                    } else {
                        val newMessage = ChatMessage(assistantResponse, isUser = false)
                        responseMessageId = newMessage.id
                        currentMessages.add(newMessage)
                    }
                    _messages.value = currentMessages
                }

                // Speak the complete AI response if in speech mode
                if (assistantResponse.isNotEmpty() && responseMessageId != null) {
                    if (_isSpeechMode.value) {
                        // Auto-play in speech mode
                        playTTS(responseMessageId!!, assistantResponse)
                    }
                }
            } catch (e: Exception) {
                _messages.value += ChatMessage("Error: ${e.message}", isUser = false)
            }

            _isLoading.value = false
        }
    }

    fun startListening() {
        Log.d("STT", "Starting Android speech recognition...")
        _isListening.value = true
        _statusMessage.value = "Initializing speech recognition..."

        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            Log.e("STT", "Error starting speech recognition: ${e.message}", e)
            _isListening.value = false
            _statusMessage.value = "Failed to start speech recognition"
        }
    }

    fun stopListening() {
        Log.d("STT", "Stopping speech recognition...")
        _isListening.value = false
        speechRecognizer?.stopListening()
        // Don't clear speech recognition text - let it stay visible
        _statusMessage.value = "Stopped listening"
    }

    fun clearSpeechRecognitionText() {
        _speechRecognitionText.value = ""
    }

    fun refreshModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ViewModel", "Refreshing models list...")
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Refreshing models..."
                }
                val models = listAvailableModels()
                withContext(Dispatchers.Main) {
                    _availableModels.value = models
                    _statusMessage.value = "Ready - Using Android Speech Recognition"
                }

                Log.d("ViewModel", "Found ${models.size} models")
                models.forEach { model ->
                    Log.d(
                        "ViewModel",
                        "Model: ${model.name}, Category: ${model.category.name}, Downloaded: ${model.isDownloaded}"
                    )
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error refreshing models: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Error: ${e.message}"
                }
            }
        }
    }

    fun initializeChat(ageGroup: String) {
        currentAgeGroup = ageGroup

        // Set system prompt, greeting, and AI name based on age group
        val (prompt, greeting, aiName) = when (ageGroup) {
            "Ages 4-7" -> {
                val systemMsg =
                    """You are a kind and gentle storyteller for young children (ages 4-7). 
                    |Use very simple words. Tell short, happy stories with clear morals. 
                    |Always be encouraging and positive. Make learning fun through imaginative tales.
                    |Keep responses brief (2-3 sentences max). End with a friendly question.""".trimMargin()
                val greetMsg =
                    "Hi there, little friend! 🌟 I love telling magical stories that teach us wonderful things. What would you like to hear a story about today?"
                val name = "Buddy the Storyteller"
                Triple(systemMsg, greetMsg, name)
            }

            "Ages 8-12" -> {
                val systemMsg =
                    """You are an exciting adventure guide and explorer for curious minds (ages 8-12).
                    |Turn every topic into a mystery, riddle, or adventure. Use engaging language and exciting metaphors.
                    |Encourage curiosity and problem-solving. Make them think like detectives and scientists.
                    |Be enthusiastic and use emojis occasionally. End with a challenging question.""".trimMargin()
                val greetMsg =
                    "Hey Explorer! 🔍✨ Ready for an adventure? I turn boring topics into exciting mysteries and cool discoveries. What do you want to investigate today?"
                val name = "Explorer Max"
                Triple(systemMsg, greetMsg, name)
            }

            "Ages 13-16+" -> {
                val systemMsg = """You are a smart and witty mentor for teenagers (ages 13-16+).
                    |Help with complex school topics (CBSE, ICSE, SAT prep). Explain clearly with real-world examples.
                    |Foster critical thinking and problem-solving skills. Be relatable and occasionally witty.
                    |Challenge them with thought-provoking questions. Prepare them for competitive exams.""".trimMargin()
                val greetMsg =
                    "Hey there! 🎓 I'm your study buddy and problem-solving partner. Whether it's math, science, or life skills - I've got your back. What's on your mind?"
                val name = "Nova the Mentor"
                Triple(systemMsg, greetMsg, name)
            }

            else -> {
                Triple("", "Hello! How can I help you today?", "AI Tutor")
            }
        }

        systemPrompt = prompt
        _aiName.value = aiName
        val greetingMessage = ChatMessage(text = greeting, isUser = false)
        _messages.value = listOf(greetingMessage)

        // Store greeting to speak once TTS is ready
        pendingGreeting = greeting

        Log.d("ViewModel", "initializeChat called for age group: $ageGroup")
        Log.d("ViewModel", "AI Name: $aiName")
        Log.d("ViewModel", "Available models count: ${_availableModels.value.size}")

        // Auto-load the first available downloaded model
        viewModelScope.launch(Dispatchers.IO) {
            // Wait a bit for models list to be populated if needed
            kotlinx.coroutines.delay(500)

            val downloadedModel = _availableModels.value.firstOrNull {
                it.isDownloaded && (it.category.name == "LLM" || it.category.name == "LANGUAGE")
            }

            Log.d("ViewModel", "Looking for downloaded models...")
            _availableModels.value.forEach { model ->
                Log.d(
                    "ViewModel",
                    "Model: ${model.name}, Category: ${model.category.name}, Downloaded: ${model.isDownloaded}"
                )
            }

            if (downloadedModel != null) {
                Log.d(
                    "ViewModel",
                    "Auto-loading model: ${downloadedModel.name} (ID: ${downloadedModel.id})"
                )
                loadModel(downloadedModel.id)
            } else {
                Log.w("ViewModel", "No downloaded LLM model available for auto-load")
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Please download a model from the Models menu"
                }
            }
        }
    }

    // Reply to message (for swipe right functionality)
    fun replyToMessage(message: ChatMessage) {
        _replyingToMessage.value = message
        Log.d("ViewModel", "Replying to: ${message.text}")
    }

    fun cancelReply() {
        _replyingToMessage.value = null
    }

    // TTS Control Functions
    fun toggleSpeechMode() {
        _isSpeechMode.value = !_isSpeechMode.value
        if (!_isSpeechMode.value) {
            // Stop any playing TTS when switching to text mode
            stopTTS()
        }
    }

    fun playTTS(messageId: String, text: String, startPosition: Int = 0) {
        // Stop any other currently playing TTS
        if (currentSpeakingMessageId != null && currentSpeakingMessageId != messageId) {
            stopTTS()
        }

        val textToSpeak = if (startPosition > 0 && startPosition < text.length) {
            text.substring(startPosition)
        } else {
            text
        }

        Log.d("TTS", "playTTS called for messageId: $messageId")
        Log.d("TTS", "Full text length: ${text.length}")
        Log.d("TTS", "Start position: $startPosition")
        Log.d("TTS", "Text to speak length: ${textToSpeak.length}")
        Log.d("TTS", "Text to speak preview: ${textToSpeak.take(50)}...")

        lastSpokenPosition = startPosition
        currentSpeakingMessageId = messageId
        _currentlyPlayingMessageId.value = messageId
        _ttsPlaybackState.value = _ttsPlaybackState.value.toMutableMap().apply {
            put(
                messageId,
                TTSPlaybackState(
                    isPlaying = true,
                    isPaused = false,
                    currentPosition = startPosition
                )
            )
        }

        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun pauseTTS(messageId: String) {
        tts?.stop()

        // Save the exact position where we stopped
        val pausedPosition = if (currentSpeakingMessageId == messageId) {
            lastSpokenPosition
        } else {
            _ttsPlaybackState.value[messageId]?.currentPosition ?: 0
        }

        _ttsPlaybackState.value = _ttsPlaybackState.value.toMutableMap().apply {
            put(
                messageId,
                TTSPlaybackState(
                    isPlaying = false,
                    isPaused = true,
                    currentPosition = pausedPosition
                )
            )
        }
        _currentlyPlayingMessageId.value = null
        currentSpeakingMessageId = null

        Log.d("TTS", "Paused at position: $pausedPosition for message: $messageId")
    }

    fun resumeTTS(messageId: String, text: String) {
        val currentState = _ttsPlaybackState.value[messageId] ?: TTSPlaybackState()
        val resumePosition = currentState.currentPosition

        Log.d("TTS", "Resuming from position: $resumePosition for message: $messageId")
        playTTS(messageId, text, resumePosition)
    }

    fun toggleTTSForMessage(messageId: String, text: String) {
        val currentState = _ttsPlaybackState.value[messageId]
        val isCurrentlyPlaying = currentState?.isPlaying == true
        val isPaused = currentState?.isPaused == true

        when {
            isCurrentlyPlaying -> {
                // Currently playing -> Pause it
                pauseTTS(messageId)
            }

            isPaused -> {
                // Paused -> Resume from where it stopped
                resumeTTS(messageId, text)
            }

            else -> {
                // Not playing or paused -> Start from beginning
                playTTS(messageId, text, 0)
            }
        }
    }

    fun stopTTS() {
        tts?.stop()
        _currentlyPlayingMessageId.value?.let { messageId ->
            _ttsPlaybackState.value = _ttsPlaybackState.value.toMutableMap().apply {
                put(
                    messageId,
                    TTSPlaybackState(isPlaying = false, isPaused = false, currentPosition = 0)
                )
            }
        }
        _currentlyPlayingMessageId.value = null
        currentSpeakingMessageId = null
        lastSpokenPosition = 0
    }

    fun playFromText(messageId: String, fullText: String, selectedText: String) {
        val startPosition = fullText.indexOf(selectedText)
        if (startPosition >= 0) {
            playTTS(messageId, fullText, startPosition)
        }
    }

    private fun updateTTSState(utteranceId: String, isPlaying: Boolean) {
        _ttsPlaybackState.value = _ttsPlaybackState.value.toMutableMap().apply {
            val currentState = get(utteranceId) ?: TTSPlaybackState()
            put(utteranceId, currentState.copy(isPlaying = isPlaying))
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
