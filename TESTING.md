# Testing Guide

## Overview
This document provides guidance on running and writing tests for the Compose Overlay Window library.

## Test Structure

The library includes three types of tests:

### 1. Unit Tests (`library/src/test`)
- Run on the JVM (no Android device needed)
- Test business logic and algorithms
- Use Mockito for mocking Android dependencies
- Use Robolectric for Android framework testing

### 2. Instrumented Tests (`library/src/androidTest`)
- Run on an Android device or emulator
- Test Android-specific functionality
- Test actual window operations
- Test Compose UI behavior

### 3. Benchmark Tests (`benchmark/src/androidTest`)
- Measure performance of critical operations
- Run on release builds for accurate measurements
- Use AndroidX Benchmark library

## Running Tests

### Unit Tests

Run all unit tests:
```bash
./gradlew :library:test
```

Run specific test class:
```bash
./gradlew :library:test --tests CoreFloatingWindowStateTest
```

Run with coverage:
```bash
./gradlew :library:testDebugUnitTest --info
```

### Instrumented Tests

**Prerequisites**: Connected Android device or running emulator

Run all instrumented tests:
```bash
./gradlew :library:connectedAndroidTest
```

Run specific test class:
```bash
./gradlew :library:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.github.only52607.compose.core.CoreFloatingWindowInstrumentedTest
```

### Benchmark Tests

**Prerequisites**: 
- Physical Android device (recommended)
- Release build configuration
- Disable animations: Settings → Developer Options → Animation Scales (set to 0.0)

Run benchmarks:
```bash
./gradlew :benchmark:connectedAndroidTest
```

View results:
- Results are saved to: `benchmark/build/outputs/connected_android_test_additional_output/`
- Look for JSON files with detailed metrics

## Test Coverage

### Core Module Tests

#### CoreFloatingWindow
- ✅ State management (showing/destroyed states)
- ✅ Lifecycle transitions
- ✅ Resource cleanup
- ✅ Coordinate updates
- ✅ Display helper integration
- ✅ ViewModelStore access
- ✅ SavedStateRegistry access

#### DisplayHelper
- ✅ Display metrics retrieval
- ✅ API version compatibility
- ✅ Metric consistency
- ✅ Device characteristics

#### Permissions
- ✅ Overlay permission checking
- ✅ Permission request functionality

### Window Module Tests

#### ComposeFloatingWindow
- ✅ Initialization
- ✅ ViewModelProviderFactory
- ✅ Lifecycle management
- ✅ State flows
- ✅ Resource cleanup

#### Compose UI
- ✅ Simple content rendering
- ✅ Multiple elements
- ✅ Layout behavior

### Service Module Tests

#### ComposeServiceFloatingWindow
- ✅ Initialization
- ✅ Lifecycle management
- ✅ State management
- ✅ Resource cleanup

### Benchmark Tests

#### Window Creation
- ✅ Single window creation
- ✅ Multiple window creation
- ✅ Creation and destruction
- ✅ Parameter updates

#### Content Rendering
- ✅ Simple content
- ✅ Complex content
- ✅ Content updates
- ✅ Multiple content changes

## Writing New Tests

### Unit Test Example

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyFeatureTest {
    
    @Test
    fun `feature behaves correctly`() = runTest {
        // Arrange
        val expected = true
        
        // Act
        val result = myFeature()
        
        // Assert
        assertEquals(expected, result)
    }
}
```

### Instrumented Test Example

```kotlin
@RunWith(AndroidJUnit4::class)
class MyFeatureInstrumentedTest {
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun feature_works_on_device() {
        // Test implementation
    }
}
```

### Benchmark Test Example

```kotlin
@RunWith(AndroidJUnit4::class)
class MyFeatureBenchmark {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    @Test
    fun benchmark_myFeature() {
        benchmarkRule.measureRepeated {
            // Operation to benchmark
            myFeature()
        }
    }
}
```

## Test Dependencies

### Unit Testing
- JUnit 4
- Mockito (mocking framework)
- Mockito-Kotlin (Kotlin extensions)
- Robolectric (Android framework simulation)
- Turbine (Flow testing)
- Kotlinx Coroutines Test

### Instrumented Testing
- AndroidX Test Core
- AndroidX Test Runner
- AndroidX Test Rules
- AndroidX Test JUnit
- Espresso (UI testing)
- Compose UI Test
- Turbine (Flow testing)

### Benchmarking
- AndroidX Benchmark JUnit4

## Best Practices

### Unit Tests
1. Use descriptive test names with backticks
2. Follow Arrange-Act-Assert pattern
3. Test one thing per test
4. Use `runTest` for coroutine testing
5. Mock Android dependencies appropriately

### Instrumented Tests
1. Always clean up resources in `@After`
2. Use `ApplicationProvider.getApplicationContext()`
3. Test actual Android behavior
4. Handle permission requirements
5. Consider device state (animations, battery, etc.)

### Benchmark Tests
1. Run on physical devices when possible
2. Disable animations before benchmarking
3. Test release builds only
4. Measure meaningful operations
5. Use `runWithTimingDisabled` for setup/teardown
6. Run multiple iterations for statistical significance

## Continuous Integration

### GitHub Actions Example

```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          
      - name: Run Unit Tests
        run: ./gradlew :library:test
        
      - name: Run Instrumented Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          target: google_apis
          arch: x86_64
          script: ./gradlew :library:connectedAndroidTest
```

## Troubleshooting

### Common Issues

#### Unit Tests Fail with Android Framework Errors
**Solution**: Add Robolectric or mock the Android dependencies

#### Instrumented Tests Timeout
**Solution**: 
- Increase test timeout
- Ensure device is unlocked
- Disable animations
- Check for permission dialogs

#### Benchmark Tests Show High Variance
**Solution**:
- Use physical device
- Close background apps
- Disable battery optimization
- Ensure stable performance mode

#### Permission Tests Fail
**Solution**: Grant SYSTEM_ALERT_WINDOW permission manually for test app

## Test Metrics

### Current Coverage
- Unit Tests: Comprehensive core logic coverage
- Instrumented Tests: Full Android integration coverage
- Benchmarks: Key performance paths covered

### Goals
- Maintain >80% code coverage
- All critical paths tested
- Performance benchmarks for regressions
- Regular security testing

## Additional Resources

- [Android Testing Documentation](https://developer.android.com/training/testing)
- [Jetpack Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [AndroidX Benchmark](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview)
- [Kotlin Coroutines Testing](https://kotlinlang.org/docs/coroutines-testing.html)

## Contributing Tests

When contributing new features:

1. Write unit tests for business logic
2. Write instrumented tests for Android integration
3. Add benchmarks for performance-critical operations
4. Ensure all tests pass before submitting PR
5. Update this guide if adding new test patterns

## Reporting Issues

If you find issues with tests:

1. Check this guide first
2. Verify your environment setup
3. Run tests on a clean checkout
4. Report with full error logs and device info
5. Include steps to reproduce
