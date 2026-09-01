# Implementation Plan — Fix overlay UI hanging until the window is moved

**Branch:** `fix/overlay-hangup`
**Status:** Implemented — commit `4276d85` ("fix: overlay UI stops updating until the window is moved")
**Date:** 2026-09-02

## Problem statement

The overlay UI frequently stops updating while the app is running normally. State changes never
reach the screen unless an external factor — most notably the user manually dragging/moving the
window — forces the UI to update.

## Investigation

- Swept every file under `library/src/main/kotlin/com/github/only52607/compose` (core window host,
  window/service wrappers, layout params, drag/keyboard helpers) and the samples.
- Compared coroutine handling with the original upstream repository
  [only52607/compose-floating-window](https://github.com/only52607/compose-floating-window)
  (cloned/fetched for reference), which does not exhibit the hang.
- Verified androidx behavior directly from the Compose ui 1.12.0 sources jar
  (`WindowRecomposer.android.kt`).
- Reviewed git history, including the sibling branch `fix/freezing-with-the-alert-dialog`
  (11 commits based on an older master) which contained a proven fix set for this exact symptom.

### Key upstream comparison (coroutines)

The original repo's `show()` creates a **fresh** `Recomposer(AndroidUiDispatcher.CurrentThread)`
on every show, runs it via `lifecycleScope.launch(...)`, and never ties coroutine machinery to
view attachment — so WindowManager detach/reattach cycles (every hide/show) cannot kill it. Its
one weakness is leaking idle old runners.

The fork instead used `View.createLifecycleAwareWindowRecomposer(...)`, whose documented androidx
contract (verified in 1.12.0 source) is:

> "Removing the view holding the ViewTreeRecomposer means we may never be reattached again. …
> shut it down whenever it becomes detached." — `recomposer.cancel()` on `onViewDetachedFromWindow`

That contract is safe for Activity content views (detach = permanent) but fatal for an overlay
window that detaches on every `hide()` and reattaches on `show()`.

## Root causes

**A. Coroutine flaw — recomposer cancelled on detach (the "hang up")**
`window/ComposeFloatingWindow.kt` and `service/ComposeServiceFloatingWindow.kt` created the
recomposer with `View.createLifecycleAwareWindowRecomposer(...)`, which cancels the recomposer the
moment its view detaches. `hide()` (`CoreFloatingWindow.kt` → `windowManager.removeViewImmediate`
after fade-out) detaches the ComposeView, so after the first hide→show cycle the composition ran on
a cancelled recomposer and recomposition never happened again.

**B. Rendering flaw — stale content until a drag (the "only updates when I move it")**
`defaultLayoutParams()` lacked `FLAG_HARDWARE_ACCELERATED`. Windows added directly through
`WindowManager.addView` are software-rendered by default (only Activity windows inherit hardware
acceleration from the theme). On recent Compose versions, RenderNode-backed layers (GraphicsContext)
in a software-rendered window can leave recomposed content unpainted until something forces a
window traversal — e.g. `updateViewLayout` during a drag. The upstream repo doesn't hit this
combination because it is on Compose BOM 2025.03.01 (ui 1.7.x) while this project is on ui 1.12.0.

**C. Supporting issues**
- Lifecycle never reached RESUMED, so `repeatOnLifecycle(RESUMED)` /
  `collectAsStateWithLifecycle(minActiveState = RESUMED)` never collected inside the overlay.
- Show-during-hide race: the fade-out `withEndAction` removed a view that `show()` had just reused.
- Screen-off: lifecycle stayed STARTED while the screen was off; composition work accumulated into
  an invisible surface and burst after screen-on (lag).
- Recomposer context lacked the system animator-duration scale handling.

## Decisions

- **Port strategy:** re-apply the final fixed state as a fresh single commit on
  `fix/overlay-hangup` (no old-branch history).
- **Recomposer failure behavior:** an uncaught exception in `runRecomposeAndApplyChanges` is
  allowed to propagate (crash) — no try/catch, no `CoroutineExceptionHandler` on the scope.
- The proven final state from `fix/freezing-with-the-alert-dialog` was used as the reference;
  the committed result is functionally identical to it (comments/formatting differ only).

## Implementation

1. **`core/LayoutParams.kt`** — added `FLAG_HARDWARE_ACCELERATED` to default flags, with a comment
   explaining it must be requested before `addView` and why software rendering + RenderNode layers
   leaves stale content.
2. **`core/CoreFloatingWindow.kt`**
   - Window-owned `createWindowRecomposer()`: `Recomposer(coroutineContext + SystemMotionDurationScale)`
     run `CoroutineStart.UNDISPATCHED` in `lifecycleCoroutineScope`; survives detach; cleaned up
     (frame-clock observer removed, scale closed) in a `finally` when the runner exits; cancelled
     only when content is replaced or the window is closed.
   - Composition frame clock paused on ON_STOP / resumed on ON_START via a `LifecycleEventObserver`
     (mirrors androidx's lifecycle-aware recomposer, minus the detach-cancel).
   - Private `SystemMotionDurationScale` (`MotionDurationScale`, `AutoCloseable`) observing
     `Settings.Global.ANIMATOR_DURATION_SCALE` via ContentObserver backed by `mutableFloatStateOf`.
   - Lifecycle: `show()` → `syncLifecycleWithScreenState()` dispatching ON_RESUME when showing and
     the screen is interactive; `ACTION_SCREEN_ON`/`ACTION_SCREEN_OFF` receiver registered in
     `init` via `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` on the application
     context and unregistered in `close()`.
   - `show()` reuses an attached window during hide fade-out (marks showing before cancelling the
     animator, then `updateImmediately()`); resets `coordinateUpdateScheduled` before re-add
     (frame callbacks posted before a detach may be discarded).
   - `hide()` end-action bails if the window was re-shown; resets `coordinateUpdateScheduled`.
   - `onDecorViewSizeChanged()` clamps x/y only for `Gravity.START or Gravity.TOP`.
   - `close()` unregisters the receiver, cancels the fade animation, handles `wasShowing` for
     ON_STOP, and resets `coordinateUpdateScheduled`.
3. **`window/ComposeFloatingWindow.kt`** + **`service/ComposeServiceFloatingWindow.kt`** — swapped
   in `createWindowRecomposer()` with a comment documenting the androidx detach-cancel trap; added
   `defaultViewModelCreationExtras` (window flavor) for `SavedStateHandle`/`AndroidViewModel`
   defaults; updated KDoc for the new behavior contract.
4. **`library/src/androidTest/.../FloatingWindowInstrumentedTest.kt`** — lifecycle test now asserts
   RESUMED (`showMovesLifecycleToResumedImmediately`); new regressions:
   `composeContentRecomposesAfterHideAndShow`, `showDuringHideKeepsWindowAttached`,
   `showDuringHideReappliesWindowParams`, `recomposerUsesSystemMotionDurationScale`,
   `recomposerObservesSystemMotionDurationScaleChanges` (+ `RecordingWindowManager` test helper).

## Verification

- `./gradlew :library:compileReleaseKotlin :library:compileDebugAndroidTestKotlin` — pass.
- `./gradlew spotlessCheck` — pass (CI lint.yml equivalent).
- `./gradlew :samples:{app-activity,service-hilt,fullscreen-dialog,keyboard-usage}:assembleRelease`
  — pass (CI build.yml equivalent).
- `./gradlew :library:connectedDebugAndroidTest` — **not run**: no device or emulator available on
  the machine at implementation time. Run on a device to exercise the regression tests, especially
  `composeContentRecomposesAfterHideAndShow`.

## Follow-ups

- The superseded `fix/freezing-with-the-alert-dialog` branch can be discarded; its content is
  functionally included in commit `4276d85`.
- Consider documenting the threading/lifecycle contract (RESUMED while shown + screen interactive,
  recomposer retained across hide/show) in `docs/`.
