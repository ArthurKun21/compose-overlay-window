# Theming Guide for Floating Windows

## Overview
This guide explains how to apply Material3 themes to your floating window content to ensure proper styling, colors, and appearance.

## The Problem

When creating a floating window overlay, the content is rendered in a separate window context that doesn't automatically inherit the app's theme. This means:

- Default Material colors won't be applied
- Typography styles may not match your app
- Dark/Light theme switching won't work automatically
- Dynamic colors (Android 12+) won't be applied

## The Solution

Wrap your floating window content in a `MaterialTheme` composable to explicitly apply theming.

## Basic Usage

### Step 1: Create Your Theme

First, create a theme composable for your floating window (or reuse your existing app theme):

```kotlin
@Composable
fun FloatingWindowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

### Step 2: Apply Theme in setContent

When setting your floating window content, wrap it in your theme:

```kotlin
val floatingWindow = ComposeFloatingWindow(context)

floatingWindow.setContent {
    FloatingWindowTheme {
        // Your floating window content here
        FloatingActionButton(
            modifier = Modifier.dragFloatingWindow(),
            onClick = { /* action */ }
        ) {
            Icon(Icons.Default.Add, "Add")
        }
    }
}

floatingWindow.show()
```

## Complete Examples

### Example 1: Simple Floating Button with Theme

```kotlin
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.github.only52607.compose.window.ComposeFloatingWindow
import com.github.only52607.compose.window.dragFloatingWindow

fun createThemedFloatingWindow(context: Context): ComposeFloatingWindow {
    return ComposeFloatingWindow(context).apply {
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6200EE),
                    secondary = Color(0xFF03DAC6)
                )
            ) {
                FloatingActionButton(
                    modifier = Modifier.dragFloatingWindow(),
                    onClick = { /* action */ }
                ) {
                    Icon(Icons.Default.Star, "Star")
                }
            }
        }
    }
}
```

### Example 2: Using App's Existing Theme

```kotlin
import io.github.yourapp.ui.theme.YourAppTheme

fun createFloatingWindowWithAppTheme(context: Context): ComposeFloatingWindow {
    return ComposeFloatingWindow(context).apply {
        setContent {
            YourAppTheme {
                // Your content inherits all theme properties
                Surface(
                    modifier = Modifier.dragFloatingWindow(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Themed Window",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Button(onClick = { /* action */ }) {
                            Text("Action")
                        }
                    }
                }
            }
        }
    }
}
```

### Example 3: Dynamic Theme with Dark Mode Support

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*

fun createDynamicThemedWindow(context: Context): ComposeFloatingWindow {
    return ComposeFloatingWindow(context).apply {
        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            
            MaterialTheme(
                colorScheme = if (isDarkTheme) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                }
            ) {
                Surface(
                    modifier = Modifier.dragFloatingWindow(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "New notification",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
```

### Example 4: Service-Based Floating Window with Theme

```kotlin
import com.github.only52607.compose.service.ComposeServiceFloatingWindow

class MyOverlayService : Service() {
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    override fun onCreate() {
        super.onCreate()
        
        floatingWindow = ComposeServiceFloatingWindow(applicationContext).apply {
            setContent {
                MaterialTheme(
                    colorScheme = lightColorScheme()
                ) {
                    FloatingActionButton(
                        modifier = Modifier.dragServiceFloatingWindow(),
                        onClick = { /* action */ }
                    ) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                }
            }
        }
        
        floatingWindow?.show()
    }
    
    override fun onDestroy() {
        floatingWindow?.close()
        super.onDestroy()
    }
}
```

## Theme Properties You Can Customize

### Color Scheme
```kotlin
MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Color(0xFF6200EE),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBB86FC),
        onPrimaryContainer = Color.Black,
        secondary = Color(0xFF03DAC6),
        onSecondary = Color.Black,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        // ... and more
    )
)
```

### Typography
```kotlin
val CustomTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp
    )
)

MaterialTheme(
    colorScheme = yourColorScheme,
    typography = CustomTypography
)
```

### Shapes
```kotlin
val CustomShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

MaterialTheme(
    colorScheme = yourColorScheme,
    shapes = CustomShapes
)
```

## Best Practices

### ✅ DO:
- **Always wrap** your floating window content in a MaterialTheme
- **Reuse** your app's theme composable for consistency
- **Consider dark mode** - check `isSystemInDarkTheme()`
- **Use dynamic colors** on Android 12+ for system integration
- **Test both** light and dark themes

### ❌ DON'T:
- Don't forget the MaterialTheme wrapper - colors won't work properly
- Don't hardcode colors - use theme colors for consistency
- Don't ignore accessibility - ensure proper contrast ratios
- Don't apply theme to decorView only - it must be in Compose

## Common Issues and Solutions

### Issue 1: Colors Not Appearing
**Problem**: Your Material3 components use default colors instead of your theme.

**Solution**: Make sure you wrapped the content in MaterialTheme:
```kotlin
setContent {
    MaterialTheme(colorScheme = yourColorScheme) {
        YourContent() // ✅ Now has access to theme
    }
}
```

### Issue 2: Dark Mode Not Working
**Problem**: Window doesn't respect system dark mode.

**Solution**: Check dark mode in your theme:
```kotlin
setContent {
    val isDark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    ) {
        YourContent()
    }
}
```

### Issue 3: Typography Not Applied
**Problem**: Text styles don't match your app.

**Solution**: Include typography in your MaterialTheme:
```kotlin
MaterialTheme(
    colorScheme = yourColorScheme,
    typography = YourTypography // ✅ Include this
) {
    YourContent()
}
```

### Issue 4: Theme Updates Not Reflecting
**Problem**: Changing theme properties doesn't update the window.

**Solution**: Use State to trigger recomposition:
```kotlin
var isDarkMode by remember { mutableStateOf(false) }

MaterialTheme(
    colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
) {
    // Content will recompose when isDarkMode changes
}
```

## Testing Your Theme

### Visual Inspection Checklist:
- [ ] Primary colors applied correctly
- [ ] Text is readable with proper contrast
- [ ] Buttons use theme colors
- [ ] Icons have appropriate tint
- [ ] Dark mode works correctly
- [ ] Dynamic colors work on Android 12+
- [ ] Typography styles are consistent with app
- [ ] Shapes match your design system

### Code Example for Testing:
```kotlin
@Composable
fun ThemeTestContent() {
    Column(modifier = Modifier.padding(16.dp)) {
        // Test primary color
        Button(onClick = {}) {
            Text("Primary Button")
        }
        
        // Test surface color
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Text("Surface Text", modifier = Modifier.padding(8.dp))
        }
        
        // Test typography
        Text(
            "Headline",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "Body text",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
```

## Advanced: Creating a Reusable Theme Wrapper

For complex apps, create a dedicated theme wrapper:

```kotlin
@Composable
fun FloatingWindowThemeWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        isDarkTheme -> YourDarkColorScheme
        else -> YourLightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = YourTypography,
        shapes = YourShapes
    ) {
        content()
    }
}

// Usage:
floatingWindow.setContent {
    FloatingWindowThemeWrapper {
        YourContent()
    }
}
```

## Migration Guide

If you have existing floating windows without themes:

### Before (No Theme):
```kotlin
floatingWindow.setContent {
    FloatingActionButton(onClick = {}) {
        Icon(Icons.Default.Add, "Add")
    }
}
```

### After (With Theme):
```kotlin
floatingWindow.setContent {
    MaterialTheme(
        colorScheme = lightColorScheme()
    ) {
        FloatingActionButton(onClick = {}) {
            Icon(Icons.Default.Add, "Add")
        }
    }
}
```

## Summary

- ✅ Always wrap floating window content in MaterialTheme
- ✅ Reuse your app's theme for consistency
- ✅ Support dark mode with isSystemInDarkTheme()
- ✅ Use dynamic colors on Android 12+
- ✅ Test in both light and dark modes

For more information on Material3 theming, see:
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose Material3](https://developer.android.com/jetpack/compose/designsystems/material3)
