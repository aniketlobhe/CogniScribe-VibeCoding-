# CogniScribe - AI-Powered Learning Companion

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![On-Device AI](https://img.shields.io/badge/AI-On--Device-FF6F00)]()

**Your Personal Learning Companion that Adapts, Engages, and Protects**

CogniScribe is an innovative educational Android app featuring 100% on-device AI, adaptive age-based
personas, and advanced multimodal interaction - built for the hackathon showcasing cutting-edge
mobile AI technology.

---

## Key Features

### Adaptive AI Personas

Three distinct AI personalities that match developmental stages:

- **Buddy the Storyteller** (Ages 4-7): Gentle, playful storytelling with clear morals
- **Explorer Max** (Ages 8-12): Turns topics into mysteries and adventures
- **Nova the Mentor** (Ages 13-16+): Smart mentor for CBSE/ICSE/SAT prep

### 100% Private & Secure

- **On-device AI** using Qwen 2.5 0.5B model
- **Zero data collection** - No internet required after setup
- **Completely safe** for children
- **No analytics or tracking**

### Advanced Multimodal Interaction

- **Voice Input**: Android Speech Recognition with real-time transcription
- **Voice Output**: Text-to-Speech with smart playback controls
- **Text Input**: Traditional typing interface
- **Smart TTS Controls**: Play, pause, resume from exact position
- **"Speak from Here"**: Long-press any word to start TTS from that point

### WhatsApp-Style Features

- **Swipe to Reply**: Swipe right to reply to specific messages
- **Swipe to Control TTS**: Swipe left to pause/resume speech
- **Reply Preview**: See what you're replying to
- **Smooth Animations**: Eye-comfortable, professional polish

### Beautiful UI/UX

- **Material Design 3** with gradient backgrounds
- **Smooth animations** - Optimized for eye comfort
- **Age-specific branding** - Different icons and names per age group
- **Dark mode support** - Automatic theme adaptation

---

## Screenshots

> Add screenshots here showing:
> - Start screen with 3 age groups
> - Chat interface with messages
> - Speech recognition in action
> - TTS controls and swipe gestures

---

## Technical Stack

### Core Technologies

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with StateFlow
- **Navigation**: Jetpack Navigation Compose

### AI & ML

- **SDK**: RunAnywhere SDK v0.1.3-alpha
- **Model**: Qwen 2.5 0.5B (GGUF format)
- **Backend**: llama.cpp (7 ARM64 CPU variants)
- **Execution**: 100% on-device inference

### Speech Technologies

- **STT**: Android SpeechRecognizer with partial results
- **TTS**: Android TextToSpeech with UtteranceProgressListener
- **Position Tracking**: Real-time word-level playback control

### Key Libraries

- Kotlin Coroutines & Flow
- AndroidX Lifecycle & ViewModel
- Jetpack Compose Material3
- Ktor Client (for model downloads)
- OkHttp, Retrofit, Gson

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator with:
    - **Min SDK**: 24 (Android 7.0)
    - **Target SDK**: 36
    - **RAM**: 2GB+ recommended
    - **Storage**: 1GB+ free space

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/aniketlobhe/CogniScribe.git
   cd CogniScribe
   ```

2. **Open in Android Studio**
    - File → Open → Select the project folder
    - Wait for Gradle sync to complete

3. **Build and Run**
    - Connect your Android device or start an emulator
    - Click the Run button or press `Shift + F10`

4. **First Launch Setup**
    - On first launch, tap the ⋮ menu → "Manage Models"
    - Download "Qwen 2.5 0.5B Instruct"
    - Model will auto-load when download completes (~500MB)

5. **Grant Permissions**
    - Allow microphone access for voice input
    - Permissions requested at runtime

---

## How to Use

### Getting Started

1. **Select Age Group**: Choose from 3 age-appropriate personas
2. **Download Model**: First-time setup (one-time ~500MB download)
3. **Start Learning**: Type or speak your questions!

### Interaction Modes

#### Text Mode (Default)

- Type messages in the input field
- AI responds with text
- Manual TTS controls (▶️ Pause 🔄 Replay)

#### Speech Mode

- Enable from ⋮ menu
- AI automatically speaks all responses
- Hands-free learning experience

### Advanced Features

#### Voice Input

- Tap the 🎤 microphone button
- Speak your question
- See real-time transcription
- Tap send or edit before sending

#### Reply to Messages

- Swipe right on any AI message
- Reply preview appears above input
- AI understands conversation context

#### TTS Control

- **Swipe left**: Pause/Resume TTS
- **Long-press text**: Menu to "Speak from here"
- **Tap buttons**: Play, Pause, Replay from start

---

## Project Structure

```
CogniScribe/
├── app/
│   ├── src/main/
│   │   ├── java/com/runanywhere/startup_hackathon20/
│   │   │   ├── MainActivity.kt           # Main UI and Composables
│   │   │   ├── ChatViewModel.kt          # Business logic and AI
│   │   │   ├── StartScreen.kt            # Age group selection
│   │   │   └── MyApplication.kt          # SDK initialization
│   │   ├── res/                           # Resources
│   │   └── AndroidManifest.xml
│   ├── libs/                              # RunAnywhere SDK AARs
│   └── build.gradle.kts                   # Dependencies
├── gradle/                                # Gradle wrapper
├── README.md                              # This file
└── .gitignore                             # Git ignore rules
```

---

## Key Implementation Highlights

### 1. Age-Specific System Prompts

```kotlin
val systemPrompt = when (ageGroup) {
    "Ages 4-7" -> "You are a kind storyteller for young children..."
    "Ages 8-12" -> "Turn every topic into a mystery or adventure..."
    "Ages 13-16+" -> "Help with complex topics using real-world examples..."
}
```

### 2. Streaming AI Responses

```kotlin
RunAnywhere.generateStream(messageWithPrompt).collect { token ->
    assistantResponse += token
    // Update UI in real-time
}
```

### 3. Position-Based TTS Resume

```kotlin
tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
        lastSpokenPosition = start  // Track exact word position
    }
})
```

### 4. Smooth Swipe Animations

```kotlin
val animatedOffsetX by animateDpAsState(
    targetValue = offsetX.dp,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

---

## Educational Approach

### Ages 4-7: Foundation Building

- Simple vocabulary and short sentences
- Story-based learning with clear morals
- Positive reinforcement
- Emotional engagement

### Ages 8-12: Curiosity Expansion

- Mystery and adventure framing
- Encourages questions and exploration
- Detective-style problem solving
- Builds scientific thinking

### Ages 13-16+: Critical Thinking

- Real-world applications
- Exam preparation (CBSE/ICSE/SAT)
- Complex concepts with examples
- Analytical skills development

---

## Privacy & Safety

- **No internet required** for AI interactions
- **No user data collected** or transmitted
- **No third-party analytics**
- **Age-appropriate content** filtering
- **Parental-friendly** design
- **COPPA compliant** approach

---

## Hackathon Highlights

### Innovation

- First on-device AI tutor with age-adaptive personas
- Advanced TTS with word-level position tracking
- Novel "speak from here" feature

### Technical Excellence

- Smooth 60fps animations
- Memory-efficient streaming
- Professional architecture (MVVM + Flow)

### User Experience

- Intuitive WhatsApp-style interface
- Delightful micro-interactions
- Accessibility-first design

### Social Impact
- Makes quality education accessible offline
- Protects children's privacy
- Adapts to developmental stages

---

## Future Roadmap

- [ ] Add more age groups (0-4, 17-18)
- [ ] Progress tracking and analytics
- [ ] Parent dashboard
- [ ] Curriculum integration
- [ ] Subject-specific tutors
- [ ] Larger models (1B, 3B)
- [ ] Multi-language support
- [ ] Collaborative learning features

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project was created for a hackathon. License details to be determined.

---

## Author

**Aniket Lobhe**

- GitHub: [@aniketlobhe](https://github.com/aniketlobhe)

---

## Acknowledgments

- **RunAnywhere SDK** for making on-device AI accessible
- **Qwen Team** for the excellent small language model
- **Android Team** for Speech APIs and Jetpack Compose
- **Hackathon Organizers** for the opportunity

---

## Support

For questions or issues:

- Open an issue on GitHub
- Contact: [Your email]

---

<div align="center">
  <b>Built with ❤️ for children's education and privacy</b>
  <br><br>
  <sub>Making AI-powered learning accessible, safe, and fun</sub>
</div>
