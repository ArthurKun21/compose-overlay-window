# Animation Behavior Change

## Before (Slide from Top)
The window would appear by sliding down from the top of the screen, using the default Dialog animation style.

**Characteristics:**
- Animation style: `android.R.style.Animation_Dialog`
- Direction: Slide from top to bottom
- Less smooth user experience
- Default Android system animation

## After (Fade In/Out)
The window now appears and disappears with a smooth fade animation.

**Characteristics:**
- Animation type: Alpha transition (fade)
- Duration: 300ms (configurable)
- Direction: In-place fade
- Smooth and professional appearance
- Hardware-accelerated using ViewPropertyAnimator

## Visual Comparison

### Show Animation

**Before:**
```
[Empty screen]
        ↓ (Window slides down from top)
[Window appears at position]
```

**After:**
```
[Empty screen]
        ↓ (Window fades in at position)
[Window appears at position]
```

### Hide Animation

**Before:**
```
[Window visible at position]
        ↓ (Window slides up to top)
[Empty screen]
```

**After:**
```
[Window visible at position]
        ↓ (Window fades out in place)
[Empty screen]
```

## Timeline

### Show Animation Timeline (300ms)
```
0ms:   alpha = 0.0 (invisible)
150ms: alpha = 0.5 (semi-transparent)
300ms: alpha = 1.0 (fully visible)
```

### Hide Animation Timeline (300ms)
```
0ms:   alpha = 1.0 (fully visible)
150ms: alpha = 0.5 (semi-transparent)
300ms: alpha = 0.0 (invisible, then removed)
```

## User Experience Impact

✅ **Improvements:**
- More modern and professional appearance
- Less distracting to users
- Smoother visual transition
- Consistent with Material Design principles
- Better for accessibility (less motion)

✅ **Maintained:**
- Same method signatures (`show()`, `hide()`)
- Same lifecycle behavior
- Same error handling
- No performance degradation

## Code Example

The change is transparent to users of the library:

```kotlin
// Before
val window = ComposeFloatingWindow(context)
window.setContent { /* content */ }
window.show() // Slides from top

// After
val window = ComposeFloatingWindow(context)
window.setContent { /* content */ }
window.show() // Fades in smoothly
```

No code changes required for existing applications!
