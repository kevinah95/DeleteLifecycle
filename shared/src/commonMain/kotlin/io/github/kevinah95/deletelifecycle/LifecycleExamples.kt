package io.github.kevinah95.deletelifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.currentStateAsState

// ---------------------------------------------------------------------------
// Main screen – lets students navigate between the four lifecycle demos
// ---------------------------------------------------------------------------

@Composable
fun LifecycleDemoScreen() {
    val demos = listOf(
        "1. Observe State",
        "2. EventEffect",
        "3. StartEffect",
        "4. ResumeEffect",
    )
    var selectedIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding(),
    ) {
        Text(
            text = "KMP Lifecycle Examples",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Tab row to pick a demo
        PrimaryScrollableTabRow(selectedTabIndex = selectedIndex) {
            demos.forEachIndexed { index, title ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    text = { Text(title, fontSize = 12.sp) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        when (selectedIndex) {
            0 -> Demo1_ObserveState()
            1 -> Demo2_LifecycleEventEffect()
            2 -> Demo3_LifecycleStartEffect()
            3 -> Demo4_LifecycleResumeEffect()
        }
    }
}

// ---------------------------------------------------------------------------
// Demo 1 – Observing the current Lifecycle State
//
// Concept: Every composable has access to the ambient LifecycleOwner via
// LocalLifecycleOwner.  You can convert its state into Compose State<T> with
// currentStateAsState() and react to it like any other state.
//
// States (in order): INITIALIZED → CREATED → STARTED → RESUMED
//                                  ← STARTED ← PAUSED
//                    DESTROYED ←  CREATED
// ---------------------------------------------------------------------------

@Composable
fun Demo1_ObserveState() {
    // Obtain the nearest LifecycleOwner (provided by the Activity/Fragment)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Convert the lifecycle state into Compose State so the UI recomposes
    // automatically whenever the state changes
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()

    // Map each state to a descriptive colour for easy visual feedback
    val stateColor = when (lifecycleState) {
        Lifecycle.State.RESUMED   -> Color(0xFF4CAF50) // green
        Lifecycle.State.STARTED   -> Color(0xFFFF9800) // orange
        Lifecycle.State.CREATED   -> Color(0xFF2196F3) // blue
        Lifecycle.State.DESTROYED -> Color(0xFFF44336) // red
        else                      -> Color(0xFF9E9E9E) // grey
    }

    DemoCard(
        title = "1. Observing Lifecycle State",
        description = "LocalLifecycleOwner gives you the ambient LifecycleOwner. " +
                "currentStateAsState() turns its state into Compose State<T> so the " +
                "UI recomposes on every transition.",
    ) {
        Text("Hint: minimize / restore the app to see state changes.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(stateColor, RoundedCornerShape(12.dp)),
        ) {
            Text(
                text = lifecycleState.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Diagram showing all states
        LifecycleStateDiagram(current = lifecycleState)
    }
}

// ---------------------------------------------------------------------------
// Demo 2 – LifecycleEventEffect
//
// Concept: LifecycleEventEffect is a side-effect API that runs a lambda once
// every time a specific Lifecycle.Event fires.  Unlike DisposableEffect you
// choose which event triggers the lambda.
// ---------------------------------------------------------------------------

@Composable
fun Demo2_LifecycleEventEffect() {
    val log = remember { mutableStateListOf<String>() }

    // Each LifecycleEventEffect registers a one-shot callback for that event.
    // The effects are automatically cancelled when the composable leaves
    // the composition (no manual cleanup needed).

    LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
        log.add("🟣 ON_CREATE  – composable entered composition")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        log.add("🔵 ON_START   – lifecycle moved to STARTED")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        log.add("🟢 ON_RESUME  – lifecycle moved to RESUMED (fully visible)")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        log.add("🟡 ON_PAUSE   – lifecycle moved back to STARTED")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        log.add("🔴 ON_STOP    – lifecycle moved back to CREATED (hidden)")
    }
    // ON_DESTROY is intentionally omitted: by the time the event fires, the
    // composable is leaving composition and the effect would not run reliably.

    DemoCard(
        title = "2. LifecycleEventEffect",
        description = "Registers a callback that fires once for the chosen event. " +
                "Each event is independent – no cleanup lambda required.",
    ) {
        Text("Hint: minimize / restore the app to trigger ON_STOP / ON_START.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        if (log.isEmpty()) {
            Text("No events yet…", color = Color.Gray, modifier = Modifier.padding(8.dp))
        } else {
            EventLogView(log)
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { log.clear() }) { Text("Clear log") }
    }
}

// ---------------------------------------------------------------------------
// Demo 3 – LifecycleStartEffect
//
// Concept: LifecycleStartEffect is like LaunchedEffect but tied to ON_START /
// ON_STOP.  The setup block runs when the lifecycle reaches STARTED; the
// onStopOrDispose cleanup block runs when it drops back to CREATED or the
// composable leaves composition.
// ---------------------------------------------------------------------------

@Composable
fun Demo3_LifecycleStartEffect() {
    val log = remember { mutableStateListOf<String>() }
    var isStarted by remember { mutableStateOf(false) }

    // LifecycleStartEffect receives the key (like LaunchedEffect) and a block.
    // Inside the block you call onStopOrDispose { … } at the end to provide
    // the cleanup action.
    LifecycleStartEffect(Unit) {
        isStarted = true
        log.add("▶ Started – setup block ran (ON_START)")

        onStopOrDispose {
            isStarted = false
            log.add("⏹ Stopped – cleanup block ran (ON_STOP or dispose)")
        }
    }

    DemoCard(
        title = "3. LifecycleStartEffect",
        description = "Setup runs on ON_START. The onStopOrDispose block runs on " +
                "ON_STOP or when the composable leaves composition. " +
                "Ideal for resources that should only be active while the screen is visible.",
    ) {
        Text("Hint: minimize / restore the app.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        StatusBadge(active = isStarted, activeLabel = "STARTED", inactiveLabel = "STOPPED")
        Spacer(Modifier.height(8.dp))

        EventLogView(log)

        Spacer(Modifier.height(8.dp))
        Button(onClick = { log.clear() }) { Text("Clear log") }
    }
}

// ---------------------------------------------------------------------------
// Demo 4 – LifecycleResumeEffect
//
// Concept: LifecycleResumeEffect is like LifecycleStartEffect but tied to
// ON_RESUME / ON_PAUSE.  Use it for resources that must only run when the
// screen is in the foreground and fully interactive (e.g. camera, sensors).
// ---------------------------------------------------------------------------

@Composable
fun Demo4_LifecycleResumeEffect() {
    val log = remember { mutableStateListOf<String>() }
    var isResumed by remember { mutableStateOf(false) }

    // Setup block runs on ON_RESUME.
    // onPauseOrDispose cleanup runs on ON_PAUSE or when leaving composition.
    LifecycleResumeEffect(Unit) {
        isResumed = true
        log.add("▶ Resumed – setup block ran (ON_RESUME)")

        onPauseOrDispose {
            isResumed = false
            log.add("⏸ Paused – cleanup block ran (ON_PAUSE or dispose)")
        }
    }

    DemoCard(
        title = "4. LifecycleResumeEffect",
        description = "Setup runs on ON_RESUME. The onPauseOrDispose block runs on " +
                "ON_PAUSE or when the composable leaves composition. " +
                "Use for interactive resources like camera or location.",
    ) {
        Text("Hint: minimize / restore the app (ON_STOP also triggers ON_PAUSE first).", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        StatusBadge(active = isResumed, activeLabel = "RESUMED", inactiveLabel = "PAUSED")
        Spacer(Modifier.height(8.dp))

        EventLogView(log)

        Spacer(Modifier.height(8.dp))
        Button(onClick = { log.clear() }) { Text("Clear log") }
    }
}

// ---------------------------------------------------------------------------
// Shared UI helpers
// ---------------------------------------------------------------------------

@Composable
private fun DemoCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Shows each Lifecycle.State as a coloured chip, highlighting the current one. */
@Composable
private fun LifecycleStateDiagram(current: Lifecycle.State) {
    val states = listOf(
        Lifecycle.State.INITIALIZED,
        Lifecycle.State.CREATED,
        Lifecycle.State.STARTED,
        Lifecycle.State.RESUMED,
        Lifecycle.State.DESTROYED,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("All States", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        states.forEachIndexed { index, state ->
            val isActive = state == current
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(vertical = 3.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = if (isActive) 2.dp else 0.dp,
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(vertical = 6.dp, horizontal = 12.dp),
            ) {
                Text(
                    text = state.name,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                )
            }
            if (index < states.lastIndex) {
                Text("↕", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun StatusBadge(active: Boolean, activeLabel: String, inactiveLabel: String) {
    val color = if (active) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
    val label = if (active) "● $activeLabel" else "○ $inactiveLabel"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EventLogView(log: List<String>) {
    val listState = rememberLazyListState()

    // Auto-scroll to the latest entry
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) listState.animateScrollToItem(log.lastIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 220.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(8.dp),
    ) {
        LazyColumn(state = listState) {
            items(log) { entry ->
                Text(
                    text = entry,
                    color = Color(0xFFD4D4D4),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }
    }
}
