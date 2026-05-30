package io.github.kevinah95.deletelifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

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
// Pantalla principal – permite a los estudiantes navegar entre los cuatro demos
// ---------------------------------------------------------------------------

@Composable
fun LifecycleDemoScreen() {
    val demos = listOf(
        "1. Observe State",
        "2. EventEffect",
        "3. StartEffect",
        "4. ResumeEffect",
    )
    // rememberSaveable conserva el índice seleccionado al rotar la pantalla
    var selectedIndex by rememberSaveable { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .safeContentPadding(),
    ) {
        Text(
            text = "Ejemplos del Lifecycle en KMP",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Fila de pestañas para seleccionar un demo
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
// Demo 1 – Observar el Lifecycle State actual
//
// Concepto: Todo composable tiene acceso al LifecycleOwner del entorno a través
// de LocalLifecycleOwner. Con currentStateAsState() conviertes su estado en un
// Compose State<T> y puedes reaccionar a él como cualquier otro estado.
//
// Estados (en orden): INITIALIZED → CREATED → STARTED → RESUMED
//                                   ← STARTED ← PAUSED
//                     DESTROYED ←  CREATED
// ---------------------------------------------------------------------------

@Composable
fun Demo1_ObserveState() {
    // Obtenemos el LifecycleOwner más cercano (proporcionado por la Activity/Fragment)
    val lifecycleOwner = LocalLifecycleOwner.current

    // Convertimos el lifecycle state en un Compose State para que la UI
    // se recomponga automáticamente en cada transición
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()

    // Asignamos un color a cada estado para retroalimentación visual
    val stateColor = when (lifecycleState) {
        Lifecycle.State.RESUMED   -> Color(0xFF4CAF50) // verde
        Lifecycle.State.STARTED   -> Color(0xFFFF9800) // naranja
        Lifecycle.State.CREATED   -> Color(0xFF2196F3) // azul
        Lifecycle.State.DESTROYED -> Color(0xFFF44336) // rojo
        else                      -> Color(0xFF9E9E9E) // gris
    }

    DemoCard(
        title = "1. Observando el Lifecycle State",
        description = "LocalLifecycleOwner te da acceso al LifecycleOwner del entorno. " +
                "currentStateAsState() convierte su estado en un Compose State<T> para que " +
                "la UI se recomponga en cada transición.",
    ) {
        Text("Pista: minimiza y restaura la app para ver los cambios de estado.", fontSize = 12.sp, color = Color.Gray)
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

        // Diagrama con todos los estados posibles
        LifecycleStateDiagram(current = lifecycleState)
    }
}

// ---------------------------------------------------------------------------
// Demo 2 – LifecycleEventEffect
//
// Concepto: LifecycleEventEffect es una API de side-effect que ejecuta un
// lambda una vez cada vez que un Lifecycle.Event específico ocurre. A diferencia
// de DisposableEffect, tú eliges qué evento dispara el lambda.
// ---------------------------------------------------------------------------

@Composable
fun Demo2_LifecycleEventEffect() {
    // listSaver serializa la lista como una List<String> para sobrevivir la rotación
    val log: MutableList<String> = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { mutableStateListOf(*it.toTypedArray()) })
    ) { mutableStateListOf() }

    // Cada LifecycleEventEffect registra un callback puntual para ese evento.
    // Los effects se cancelan automáticamente cuando el composable sale
    // de la composición (no se necesita cleanup manual).

    LifecycleEventEffect(Lifecycle.Event.ON_CREATE) {
        log.add("🟣 ON_CREATE  – el composable entró en la composición")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        log.add("🔵 ON_START   – lifecycle pasó al estado STARTED")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        log.add("🟢 ON_RESUME  – lifecycle pasó al estado RESUMED (visible e interactivo)")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        log.add("🟡 ON_PAUSE   – lifecycle regresó al estado STARTED")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        log.add("🔴 ON_STOP    – lifecycle regresó al estado CREATED (oculto)")
    }
    // ON_DESTROY se omite intencionalmente: cuando el evento ocurre, el composable
    // ya está saliendo de la composición y el effect no se ejecutaría de forma confiable.

    DemoCard(
        title = "2. LifecycleEventEffect",
        description = "Registra un callback que se dispara una vez por cada evento elegido. " +
                "Cada evento es independiente – no se requiere lambda de cleanup.",
    ) {
        Text("Pista: minimiza y restaura la app para disparar ON_STOP / ON_START.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        if (log.isEmpty()) {
            Text("Sin eventos todavía…", color = Color.Gray, modifier = Modifier.padding(8.dp))
        } else {
            EventLogView(log)
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { log.clear() }) { Text("Limpiar log") }
    }
}

// ---------------------------------------------------------------------------
// Demo 3 – LifecycleStartEffect
//
// Concepto: LifecycleStartEffect es como LaunchedEffect pero vinculado a
// ON_START / ON_STOP. El bloque de setup corre cuando el lifecycle llega a
// STARTED; el bloque de cleanup onStopOrDispose corre cuando regresa a CREATED
// o cuando el composable sale de la composición.
// ---------------------------------------------------------------------------

@Composable
fun Demo3_LifecycleStartEffect() {
    val log: MutableList<String> = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { mutableStateListOf(*it.toTypedArray()) })
    ) { mutableStateListOf() }
    var isStarted by rememberSaveable { mutableStateOf(false) }

    // LifecycleStartEffect recibe una key (igual que LaunchedEffect) y un bloque.
    // Dentro del bloque debes llamar onStopOrDispose { … } al final para
    // definir la acción de cleanup.
    LifecycleStartEffect(Unit) {
        isStarted = true
        log.add("▶ Iniciado – bloque de setup ejecutado (ON_START)")

        onStopOrDispose {
            isStarted = false
            log.add("⏹ Detenido – bloque de cleanup ejecutado (ON_STOP o dispose)")
        }
    }

    DemoCard(
        title = "3. LifecycleStartEffect",
        description = "El setup corre en ON_START. El bloque onStopOrDispose corre en " +
                "ON_STOP o cuando el composable sale de la composición. " +
                "Ideal para recursos que solo deben estar activos mientras la pantalla es visible.",
    ) {
        Text("Pista: minimiza y restaura la app.", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        StatusBadge(active = isStarted, activeLabel = "STARTED", inactiveLabel = "STOPPED")
        Spacer(Modifier.height(8.dp))

        EventLogView(log)

        Spacer(Modifier.height(8.dp))
        Button(onClick = { log.clear() }) { Text("Limpiar log") }
    }
}

// ---------------------------------------------------------------------------
// Demo 4 – LifecycleResumeEffect
//
// Concepto: LifecycleResumeEffect es como LifecycleStartEffect pero vinculado a
// ON_RESUME / ON_PAUSE. Úsalo para recursos que solo deben estar activos cuando
// la pantalla está en primer plano y es completamente interactiva (ej. cámara,
// sensores).
// ---------------------------------------------------------------------------

@Composable
fun Demo4_LifecycleResumeEffect() {
    val log: MutableList<String> = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(save = { it.toList() }, restore = { mutableStateListOf(*it.toTypedArray()) })
    ) { mutableStateListOf() }
    var isResumed by rememberSaveable { mutableStateOf(false) }

    // El bloque de setup corre en ON_RESUME.
    // El cleanup onPauseOrDispose corre en ON_PAUSE o al salir de la composición.
    LifecycleResumeEffect(Unit) {
        isResumed = true
        log.add("▶ Reanudado – bloque de setup ejecutado (ON_RESUME)")

        onPauseOrDispose {
            isResumed = false
            log.add("⏸ Pausado – bloque de cleanup ejecutado (ON_PAUSE o dispose)")
        }
    }

    DemoCard(
        title = "4. LifecycleResumeEffect",
        description = "El setup corre en ON_RESUME. El bloque onPauseOrDispose corre en " +
                "ON_PAUSE o cuando el composable sale de la composición. " +
                "Úsalo para recursos interactivos como la cámara o la ubicación.",
    ) {
        Text("Pista: minimiza y restaura la app (ON_STOP también dispara ON_PAUSE primero).", fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        StatusBadge(active = isResumed, activeLabel = "RESUMED", inactiveLabel = "PAUSED")
        Spacer(Modifier.height(8.dp))

        EventLogView(log)

        Spacer(Modifier.height(8.dp))
        Button(onClick = { log.clear() }) { Text("Limpiar log") }
    }
}

// ---------------------------------------------------------------------------
// Helpers de UI compartidos
// ---------------------------------------------------------------------------

@Composable
private fun DemoCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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

/** Muestra cada Lifecycle.State como un chip con color, resaltando el estado actual. */
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
        Text("Todos los estados", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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

    // Auto-scroll hacia la entrada más reciente
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
