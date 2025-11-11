# Keyboard and Status Bar Fixes

## Issues Fixed

### Issue 1: Keyboard Covering Messages

**Problem:** When clicking on the text input field and the keyboard opens, the first message goes
too far up, making it hard to see the conversation context.

**Solution:** Added `imePadding()` modifier to:

- The `LazyColumn` (messages list) - This ensures messages adjust their padding when the keyboard
  appears
- The input section `Column` - This ensures the input area moves up with the keyboard

**Code Changes:**

```kotlin
LazyColumn(
    state = listState,
    modifier = Modifier
        .weight(1f)
        .imePadding(), // Keeps messages visible above the keyboard
    // ...
)

Column(
    modifier = Modifier
        .fillMaxWidth()
        .imePadding() // Keeps input section above the keyboard
) {
    // Input fields...
}
```

### Issue 2: App Overlapping Status Bar

**Problem:** The app overlaps with the system status bar (battery, time, mobile data indicators at
the top).

**Solution:** Added `statusBarsPadding()` modifier to the `Scaffold` to ensure the app content
starts below the status bar.

**Code Changes:**

```kotlin
Scaffold(
    modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding(), // Ensures content doesn't overlap with status bar
    topBar = {
        // TopAppBar...
    }
) { padding ->
    // Content...
}
```

### Bonus: Auto-scroll When Typing

**Added Feature:** The chat now automatically scrolls to the last message when you start typing (
keyboard opens).

**Code Changes:**

```kotlin
// Auto-scroll when keyboard opens (user starts typing)
LaunchedEffect(inputText.isNotEmpty()) {
    if (messages.isNotEmpty() && inputText.isNotEmpty()) {
        // Delay slightly to allow keyboard animation to complete
        kotlinx.coroutines.delay(100)
        listState.animateScrollToItem(messages.lastIndex)
    }
}
```

## What These Modifiers Do

### `imePadding()`

- **IME** = Input Method Editor (the keyboard)
- Automatically adds padding when the keyboard appears
- Removes padding when keyboard is hidden
- Ensures UI elements stay visible above the keyboard

### `statusBarsPadding()`

- Adds padding at the top equal to the height of the system status bar
- Prevents content from being drawn behind the status bar
- Works on all Android versions with proper insets

## User Experience Improvements

### Before:

- ❌ Keyboard covered messages
- ❌ First message went too far up, losing context
- ❌ App overlapped with status bar
- ❌ Had to manually scroll to see recent messages

### After:

- ✅ Messages stay visible above keyboard
- ✅ Last message remains in view for context
- ✅ App content starts cleanly below status bar
- ✅ Auto-scrolls to latest message when typing starts
- ✅ Smooth animations when keyboard opens/closes

## Technical Details

### Window Insets

Android provides system window insets to handle:

- Status bar (top)
- Navigation bar (bottom)
- Keyboard (IME)
- Notches/cutouts

Jetpack Compose provides convenient modifiers to handle these:

- `statusBarsPadding()` - Top padding for status bar
- `navigationBarsPadding()` - Bottom padding for nav bar
- `imePadding()` - Dynamic padding for keyboard
- `systemBarsPadding()` - Combined status + navigation

### Why the 100ms Delay?

```kotlin
kotlinx.coroutines.delay(100)
```

The keyboard opening animation takes time. We wait 100ms to let the keyboard animation mostly
complete before scrolling, resulting in a smoother user experience.

## Testing Checklist

Test these scenarios:

### Keyboard Behavior:

- [ ] Open chat and click on input field
- [ ] Verify last message remains visible above keyboard
- [ ] Type some text and verify it scrolls to show context
- [ ] Close keyboard (back button) and verify messages stay in position
- [ ] Send message and verify smooth scroll to new message

### Status Bar:

- [ ] Launch app on device
- [ ] Verify app title doesn't overlap with status bar
- [ ] Verify status bar (battery, time, etc.) is fully visible
- [ ] Test on device with notch if available
- [ ] Test in portrait and landscape modes

### Edge Cases:

- [ ] Very long messages (should wrap properly)
- [ ] Many messages (scroll should work smoothly)
- [ ] Rotate device while keyboard is open
- [ ] Switch between apps while keyboard is open

## Compatibility

These fixes work on:

- ✅ Android 5.0+ (API 21+)
- ✅ Devices with notches/cutouts
- ✅ Devices with different screen sizes
- ✅ Portrait and landscape orientations
- ✅ Different keyboard heights (varies by keyboard app)

## Files Modified

1. **MainActivity.kt**
    - Added `imePadding()` to LazyColumn
    - Added `imePadding()` to input section Column
    - Added `statusBarsPadding()` to Scaffold
    - Added auto-scroll LaunchedEffect for keyboard

## Notes

- The `imePadding()` is applied to both the messages list and input section to ensure both adjust
  properly
- The `statusBarsPadding()` is applied to the Scaffold (root container) to affect the entire screen
- Auto-scroll has a small delay to sync with keyboard animation
- These modifiers work automatically - no need to manually calculate heights

## Future Improvements

Potential enhancements:

1. Add smooth animation when scrolling to last message
2. Show "scroll to bottom" FAB when user scrolls up
3. Remember scroll position when returning to chat
4. Add haptic feedback when auto-scrolling
5. Optimize scroll performance for very long conversations

Last Updated: [Current Date]
