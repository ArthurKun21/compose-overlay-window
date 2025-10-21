# ViewModel Support for Service-Based Floating Windows

## Overview
`ComposeServiceFloatingWindow` now provides full ViewModel support, enabling you to use ViewModels in floating windows attached to Services, just like you would in regular Activities.

## What Changed

The `ComposeServiceFloatingWindow` class now implements `HasDefaultViewModelProviderFactory`, which provides:
- Automatic ViewModel creation using `viewModel()` function
- ViewModel lifecycle tied to the floating window
- State preservation across configuration changes
- Proper cleanup when the window is destroyed

## Basic Usage

### Simple ViewModel in Service

```kotlin
// Your ViewModel
class FloatingViewModel : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()
    
    fun increment() {
        _count.value++
    }
}

// Your Service
class MyOverlayService : Service() {
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    override fun onCreate() {
        super.onCreate()
        
        floatingWindow = ComposeServiceFloatingWindow(applicationContext).apply {
            setContent {
                MaterialTheme {
                    // ViewModel is automatically created and managed!
                    MyFloatingContent(viewModel = viewModel())
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

@Composable
fun MyFloatingContent(
    viewModel: FloatingViewModel = viewModel()
) {
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

### Example 4: With Dependency Injection (Hilt)

Using Hilt for dependency injection:

```kotlin
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

@AndroidEntryPoint
class OverlayService : Service() {
    
    @Inject
    lateinit var userRepository: UserRepository
    
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    override fun onCreate() {
        super.onCreate()
        
        floatingWindow = ComposeServiceFloatingWindow(applicationContext).apply {
            setContent {
                MaterialTheme {
                    // Hilt automatically provides the ViewModel
                    FloatingScreen(viewModel = hiltViewModel())
                }
            }
        }
        
        floatingWindow?.show()
    }
}

@Composable
fun FloatingScreen(
    viewModel: FloatingViewModel = hiltViewModel()
) {
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    
    // Use the data
}
```

## Migration Guide

### Before (Manual ViewModel Management)

Previously, you had to manually create and manage ViewModels:

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

### After (Automatic ViewModel Support)

Now you can use ViewModels directly with `viewModel()` function:

```kotlin
class ServiceOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var floatingWindow: ComposeServiceFloatingWindow? = null
    
    init {
        floatingWindow = ComposeServiceFloatingWindow(context).apply {
            setContent {
                MaterialTheme {
                    // ViewModel is automatically created and managed!
                    FloatingScreen(viewModel = viewModel())
                }
            }
        }
    }
    
    fun close() {
        floatingWindow?.close()
        // ViewModel is automatically cleaned up!
    }
}
```

## Best Practices

### ✅ DO:

1. **Use `viewModel()` function** - Let the framework manage lifecycle
   ```kotlin
   @Composable
   fun MyContent(viewModel: MyViewModel = viewModel())
   ```

2. **Collect StateFlow with lifecycle** - Use `collectAsStateWithLifecycle()`
   ```kotlin
   val state by viewModel.state.collectAsStateWithLifecycle()
   ```

3. **Clean up in ViewModel** - Override `onCleared()` for cleanup
   ```kotlin
   override fun onCleared() {
       super.onCleared()
       // Cleanup resources
   }
   ```

4. **Use viewModelScope** - For coroutines in ViewModels
   ```kotlin
   fun loadData() = viewModelScope.launch {
       // Coroutine code
   }
   ```

### ❌ DON'T:

1. **Don't manually create ViewModels** - Use `viewModel()` instead
   ```kotlin
   // ❌ Don't do this
   val vm = remember { MyViewModel() }
   
   // ✅ Do this
   val vm: MyViewModel = viewModel()
   ```

2. **Don't store ViewModels in Service** - Let the window manage them
   ```kotlin
   // ❌ Don't do this
   class MyService : Service() {
       private var viewModel: MyViewModel? = null
   }
   
   // ✅ Do this - use viewModel() in Composable
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
