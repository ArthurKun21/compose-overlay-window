# Fade In/Out Animation Feature

## Overview
The CoreFloatingWindow now uses smooth fade in/out animations when showing and hiding, providing a better user experience compared to the previous slide-from-top animation.

## Implementation Details

### Show Animation
When `show()` is called:
1. The decorView is set to alpha 0 (fully transparent)
2. The view is added to the WindowManager
3. A fade-in animation runs over 300ms, smoothly transitioning alpha from 0 to 1
4. The lifecycle moves to STARTED after the animation completes

### Hide Animation
When `hide()` is called:
1. A fade-out animation runs over 300ms, smoothly transitioning alpha from 1 to 0
2. The view is removed from the WindowManager after the animation completes
3. The lifecycle moves to STOPPED after removal

### Animation Duration
- **Duration**: 300 milliseconds (0.3 seconds)
- Defined as a constant: `ANIMATION_DURATION = 300L`
- Can be adjusted if needed by modifying the constant in `CoreFloatingWindow`

## Code Changes

### Modified Files
1. **CoreFloatingWindow.kt**
   - Updated `show()` method to implement fade-in animation
   - Updated `hide()` method to implement fade-out animation
   - Added `ANIMATION_DURATION` constant (300ms)

2. **LayoutParams.kt**
   - Removed `windowAnimations = android.R.style.Animation_Dialog`
   - Animations are now handled programmatically for better control

## Usage Example

```kotlin
val floatingWindow = ComposeFloatingWindow(context)

floatingWindow.setContent {
    Text("Hello, Floating Window!")
}

// Show with fade-in animation (300ms)
floatingWindow.show()

// Hide with fade-out animation (300ms)
floatingWindow.hide()
```

## Benefits

1. **Smoother User Experience**: Fade animations are less jarring than slide animations
2. **Professional Look**: Fade animations are commonly used in modern UIs
3. **Consistent Behavior**: Same animation for both show and hide operations
4. **Configurable**: Animation duration can be easily adjusted

## Testing

Added `CoreFloatingWindowAnimationTest.kt` with tests to verify:
- Animation initialization state
- Animation duration consistency

## Performance

- Animations use Android's ViewPropertyAnimator for hardware-accelerated performance
- 300ms duration provides smooth animation without feeling slow
- No performance impact on other window operations

## Future Enhancements

Possible improvements for the future:
- Make animation duration configurable via constructor parameter
- Add different animation types (scale, slide, etc.)
- Add custom interpolators for different animation curves
- Add option to disable animations for accessibility

## Migration Guide

No breaking changes. The animations are applied automatically:
- Existing code continues to work without modifications
- `show()` and `hide()` methods maintain the same signature
- Lifecycle events still fire at appropriate times

## Technical Notes

- Lifecycle events (ON_START, ON_STOP) are triggered after animations complete
- Error handling ensures lifecycle transitions happen even if animation fails
- Alpha is reset to 1.0 if show() is called multiple times
- Animations are automatically cancelled if the window is destroyed
