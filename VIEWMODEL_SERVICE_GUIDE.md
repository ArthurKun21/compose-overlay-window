# ViewModel Support for Service-Based Floating Windows

## Overview
This guide explains how to use ViewModels in floating windows attached to Services. Since Services don't have the same lifecycle as Activities, ViewModels must be managed manually or through dependency injection frameworks like Hilt.

## Important Note

**`ComposeServiceFloatingWindow` does NOT implement `HasDefaultViewModelProviderFactory`** to avoid conflicts with dependency injection frameworks like Hilt. This is intentional and by design.

## Why Manual ViewModel Management?

Unlike Activities, Services require explicit ViewModel management because:
- Services don't have built-in ViewModel support
- Dependency injection frameworks (Hilt, Koin) handle ViewModel creation differently in Services
- Automatic ViewModel factories can conflict with Hilt's injection mechanism
- Manual management gives you more control over the ViewModel lifecycle

## Basic Usage

### Method 1: Manual ViewModel Creation (Recommended for Simple Cases)

```kotlin
// Your ViewModel
class FloatingViewModel(private val repository: Repository) : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()
    
    fun increment() {
        _count.value++
    }
}

// Your Service
class MyOverlayService : Service() {
    private var viewModel: FloatingViewModel? = null
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    override fun onCreate() {
        super.onCreate()
        
        // Create ViewModel manually
        viewModel = FloatingViewModel(MyRepository())
        
        floatingWindow = ComposeServiceFloatingWindow(applicationContext).apply {
            setContent {
                MaterialTheme {
                    // Pass ViewModel as parameter
                    viewModel?.let { MyFloatingContent(it) }
                }
            }
        }
        
        floatingWindow?.show()
    }
    
    override fun onDestroy() {
        floatingWindow?.close()
        viewModel = null  // Clean up ViewModel
        super.onDestroy()
    }
}

@Composable
fun MyFloatingContent(viewModel: FloatingViewModel) {
    val count by viewModel.count.collectAsStateWithLifecycle()
    
    FloatingActionButton(
        modifier = Modifier.dragServiceFloatingWindow(),
        onClick = { viewModel.increment() }
    ) {
        Text("Count: $count")
    }
}
```

## Advanced Examples

### Example 1: ViewModel with SavedStateHandle

ViewModels can now access `SavedStateHandle` for state preservation:

```kotlin
class SavedStateViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    var text: String
        get() = savedStateHandle.get<String>("text") ?: ""
        set(value) { savedStateHandle.set("text", value) }
    
    private val _items = MutableStateFlow<List<String>>(emptyList())
    val items: StateFlow<List<String>> = _items.asStateFlow()
    
    init {
        // Restore state on init
        _items.value = savedStateHandle.get<List<String>>("items") ?: emptyList()
    }
    
    fun addItem(item: String) {
        val newItems = _items.value + item
        _items.value = newItems
        savedStateHandle.set("items", newItems)
    }
}

@Composable
fun FloatingListScreen(
    viewModel: SavedStateViewModel = viewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .dragServiceFloatingWindow()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        items.forEach { item ->
            Text(item)
        }
    }
}
```

### Example 2: Multiple ViewModels

You can use multiple ViewModels in the same floating window:

```kotlin
class DataViewModel : ViewModel() {
    val data = MutableStateFlow("Data")
}

class UIViewModel : ViewModel() {
    val expanded = MutableStateFlow(false)
}

@Composable
fun MultiViewModelContent(
    dataViewModel: DataViewModel = viewModel(),
    uiViewModel: UIViewModel = viewModel()
) {
    val data by dataViewModel.data.collectAsStateWithLifecycle()
    val expanded by uiViewModel.expanded.collectAsStateWithLifecycle()
    
    // Use both ViewModels
}
```

### Example 3: Custom ViewModelProvider.Factory

For ViewModels with constructor parameters:

```kotlin
class CustomViewModel(
    private val repository: Repository
) : ViewModel() {
    // Implementation
}

class CustomViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// In your Service
class MyService : Service() {
    private val repository = MyRepository()
    
    override fun onCreate() {
        super.onCreate()
        
        val floatingWindow = ComposeServiceFloatingWindow(applicationContext).apply {
            setContent {
                MaterialTheme {
                    val factory = remember { CustomViewModelFactory(repository) }
                    MyContent(viewModel = viewModel(factory = factory))
                }
            }
        }
        
        floatingWindow.show()
    }
}
```

### Method 2: With Dependency Injection (Hilt) - Recommended for Production

Using Hilt for automatic dependency injection (this is the recommended approach for production apps):

```kotlin
// Your HiltViewModel
@HiltViewModel
class FloatingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    val userData = userRepository.userData
    val preferences = preferencesRepository.preferences
    
    fun updateUser(name: String) = viewModelScope.launch {
        userRepository.updateUser(name)
    }
}

// Service Overlay wrapper (injected by Hilt)
@ServiceScoped
class ServiceOverlay @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
) {
    private var viewModel: FloatingViewModel? = null
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    init {
        floatingWindow = createFloatingWindow()
    }
    
    private fun createFloatingWindow(): ComposeServiceFloatingWindow {
        // Create ViewModel with Hilt-injected dependencies
        viewModel = FloatingViewModel(userPreferencesRepository)
        
        return ComposeServiceFloatingWindow(context).apply {
            setContent {
                MaterialTheme {
                    // Pass ViewModel as parameter
                    viewModel?.let { FloatingScreen(it) }
                }
            }
        }
    }
    
    fun show() {
        floatingWindow?.show()
    }
    
    fun close() {
        floatingWindow?.close()
        viewModel = null
    }
}

// Your Service with Hilt
@AndroidEntryPoint
class OverlayService : Service() {
    
    @Inject
    lateinit var serviceOverlay: ServiceOverlay
    
    override fun onCreate() {
        super.onCreate()
        serviceOverlay.show()
    }
    
    override fun onDestroy() {
        serviceOverlay.close()
        super.onDestroy()
    }
}

@Composable
fun FloatingScreen(viewModel: FloatingViewModel) {
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    // Use the data
}
```

**Why this pattern works:**
- ✅ No conflicts with Hilt's dependency injection
- ✅ ViewModel is created with Hilt-injected dependencies
- ✅ Clean separation of concerns with ServiceOverlay wrapper
- ✅ Proper lifecycle management
- ✅ Avoids the `HasDefaultViewModelProviderFactory` issue

## Migration Guide

### Current Best Practice (Manual ViewModel Management)

The recommended approach is to manually create and manage ViewModels in Services:

```kotlin
class ServiceOverlay @Inject constructor(
    private val repository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
) {
    private var viewModel: FloatingViewModel? = null
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    init {
        // Manually create ViewModel
        viewModel = FloatingViewModel(repository)
        
        floatingWindow = ComposeServiceFloatingWindow(context).apply {
            setContent {
                // Pass ViewModel manually
                viewModel?.let { vm ->
                    FloatingScreen(vm)
                }
            }
        }
    }
    
    fun close() {
        floatingWindow?.close()
        viewModel = null  // Manual cleanup
    }
}
```

**Example (This is the correct approach):**

```kotlin
@ServiceScoped
class ServiceOverlay @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
) {
    private var viewModel: FloatingViewModel? = null
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    init {
        // Create ViewModel manually (with or without Hilt dependencies)
        viewModel = FloatingViewModel(userPreferencesRepository)
        
        floatingWindow = ComposeServiceFloatingWindow(context).apply {
            setContent {
                MaterialTheme {
                    // Pass ViewModel as parameter
                    viewModel?.let { FloatingScreen(it) }
                }
            }
        }
    }
    
    fun close() {
        floatingWindow?.close()
        viewModel = null  // Manual cleanup
    }
}
```

### Why Not Use `HasDefaultViewModelProviderFactory`?

`ComposeServiceFloatingWindow` intentionally **does NOT** implement `HasDefaultViewModelProviderFactory` because:

1. **Hilt Conflicts**: Implementing this interface causes crashes when used with Hilt in Services
2. **Service Lifecycle**: Services don't have the same lifecycle as Activities, making automatic ViewModel factories problematic
3. **Dependency Injection**: DI frameworks like Hilt and Koin need to control ViewModel creation
4. **Explicit Management**: Manual management is clearer and more predictable in Service contexts

## Best Practices

### ✅ DO:

1. **Create ViewModels manually in your Service** - Maintain explicit control
   ```kotlin
   class MyService : Service() {
       private var viewModel: MyViewModel? = null
       
       override fun onCreate() {
           viewModel = MyViewModel(repository)
       }
   }
   ```

2. **Pass ViewModels as parameters to Composables**
   ```kotlin
   @Composable
   fun MyContent(viewModel: MyViewModel)  // No default parameter
   ```

3. **Collect StateFlow with lifecycle** - Use `collectAsStateWithLifecycle()`
   ```kotlin
   val state by viewModel.state.collectAsStateWithLifecycle()
   ```

4. **Clean up ViewModels in Service** - Set to null in onDestroy
   ```kotlin
   override fun onDestroy() {
       floatingWindow?.close()
       viewModel = null  // Important!
       super.onDestroy()
   }
   ```

5. **Use viewModelScope** - For coroutines in ViewModels
   ```kotlin
   fun loadData() = viewModelScope.launch {
       // Coroutine code
   }
   ```

### ❌ DON'T:

1. **Don't use `viewModel()` function in Service context** - It won't work properly
   ```kotlin
   // ❌ Don't do this in Services
   floatingWindow.setContent {
       MyContent(viewModel = viewModel())  // Will crash!
   }
   
   // ✅ Do this instead
   val vm = MyViewModel(repository)
   floatingWindow.setContent {
       MyContent(vm)  // Pass as parameter
   }
   ```

2. **Don't try to use `HasDefaultViewModelProviderFactory`** - Causes Hilt conflicts
   ```kotlin
   // ❌ ComposeServiceFloatingWindow does NOT implement this
   // This is intentional to avoid Hilt conflicts
   ```

3. **Don't forget to close the window** - This cleans up ViewModels
   ```kotlin
   override fun onDestroy() {
       floatingWindow?.close()  // ✅ This cleans up ViewModels
       super.onDestroy()
   }
   ```

## ViewModel Lifecycle

The ViewModel lifecycle in a service-based floating window:

```
Service onCreate
    ↓
Window created
    ↓
setContent called
    ↓
ViewModel created (first composition)
    ↓
Window shown
    ↓
... Window is active ...
    ↓
Window.close() called
    ↓
ViewModel.onCleared() called
    ↓
ViewModelStore cleared
    ↓
Service onDestroy
```

## Debugging

### Check ViewModel is Created
```kotlin
class MyViewModel : ViewModel() {
    init {
        Log.d("MyViewModel", "ViewModel created")
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d("MyViewModel", "ViewModel cleared")
    }
}
```

### Verify Lifecycle State
```kotlin
@Composable
fun DebugContent(viewModel: MyViewModel = viewModel()) {
    val window = LocalServiceFloatingWindow.current
    val lifecycleState = window.lifecycle.currentState
    
    Text("Lifecycle: $lifecycleState")
}
```

## Common Issues and Solutions

### Issue 1: ViewModel Not Persisting State

**Problem**: State is lost when the window is hidden and shown again.

**Solution**: Use `SavedStateHandle` in your ViewModel:
```kotlin
class MyViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var data by savedStateHandle.saveable { mutableStateOf("") }
}
```

### Issue 2: Multiple ViewModel Instances

**Problem**: Getting different ViewModel instances each composition.

**Solution**: Ensure you're using `viewModel()` correctly and not creating new instances:
```kotlin
// ✅ Correct - Same instance
@Composable
fun MyContent(viewModel: MyViewModel = viewModel())

// ❌ Wrong - Creates new instance each time
@Composable
fun MyContent(viewModel: MyViewModel = remember { MyViewModel() })
```

### Issue 3: ViewModel Not Cleaned Up

**Problem**: ViewModel's `onCleared()` is not called.

**Solution**: Always call `close()` on the window:
```kotlin
override fun onDestroy() {
    floatingWindow?.close()  // This triggers ViewModel cleanup
    super.onDestroy()
}
```

### Issue 4: Context/Application Not Available

**Problem**: ViewModel needs Application context.

**Solution**: Use `AndroidViewModel`:
```kotlin
class MyViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
}
```

## Testing ViewModels in Floating Windows

```kotlin
@Test
fun testViewModel() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val window = ComposeServiceFloatingWindow(context)
    
    window.setContent {
        val viewModel: TestViewModel = viewModel()
        // Test ViewModel behavior
    }
    
    // Cleanup
    window.close()
}
```

## Summary

- ✅ Full ViewModel support in service-based floating windows
- ✅ Automatic lifecycle management
- ✅ State preservation with SavedStateHandle
- ✅ Compatible with dependency injection (Hilt, Koin, etc.)
- ✅ No breaking changes to existing code
- ✅ Same API as regular Compose Activities

For more information:
- [ViewModel Overview](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Compose ViewModel](https://developer.android.com/jetpack/compose/libraries#viewmodel)
- [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate)
