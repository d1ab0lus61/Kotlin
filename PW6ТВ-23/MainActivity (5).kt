package com.example.transformermonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// ====================== МОДЕЛІ ТА ДОМЕН ======================

data class TransformerId(val id: String) // модель ідентифікатора трансформатора

data class Telemetry( // модель телеметричних даних
    val transformerId: TransformerId, // ідентифікатор трансформатора
    val timestampMs: Long, // час вимірювання у мілісекундах
    val voltageKv: Double, // напруга у кіловольтах
    val currentA: Double, // струм у амперах
    val temperatureC: Double, // температура у градусах Цельсія
    val loadFactor: Double, // коефіцієнт навантаження [0..1]
)

data class SeriesPoint(val t: Long, val value: Double) // точка для графіка (час + значення)

enum class AnomalyFlag { NONE, VOLTAGE_SPIKE, OVERCURRENT, OVERHEAT, UNDERLOAD, OVERLOAD } // перелік можливих аномалій

data class ProcessedTelemetry( // оброблені дані
    val raw: Telemetry, // сирі дані
    val smoothedVoltageKv: Double, // усереднена напруга
    val smoothedCurrentA: Double, // усереднений струм
    val smoothedTemperatureC: Double, // усереднена температура
    val anomaly: AnomalyFlag // виявлена аномалія
)

data class UiState( // стан інтерфейсу користувача
    val selectedTransformer: TransformerId? = null, // вибраний трансформатор
    val voltageSeries: List<SeriesPoint> = emptyList(), // серія напруги
    val currentSeries: List<SeriesPoint> = emptyList(), // серія струму
    val temperatureSeries: List<SeriesPoint> = emptyList(), // серія температури
    val loadSeries: List<SeriesPoint> = emptyList(), // серія навантаження
    val lastAnomaly: AnomalyFlag = AnomalyFlag.NONE, // остання аномалія
    val isRunning: Boolean = false, // чи працює генератор
    val availableIds: List<TransformerId> = emptyList() // список доступних трансформаторів
)

fun Double.clamp(min: Double, max: Double) = when { // функція обмеження значення
    this < min -> min // якщо менше мінімуму — повертаємо мінімум
    this > max -> max // якщо більше максимуму — повертаємо максимум
    else -> this // інакше повертаємо значення як є
}

object Rates { // об’єкт із налаштуваннями частоти
    val emission: Duration = 0.5.seconds // інтервал генерації даних — 0.5 секунди
}

// ====================== ГЕНЕРАТОР ДАНИХ ======================

class TelemetryGenerator( // клас генератора телеметричних даних
    private val scope: CoroutineScope, // область корутин, у якій запускається генерація
    private val output: Channel<Telemetry>, // канал для передачі згенерованих даних
    private val transformerIds: List<TransformerId>, // список трансформаторів, для яких генеруємо дані
    private val seed: Int = 42 // початкове значення для генератора випадкових чисел (для відтворюваності)
) {
    private val rnd = Random(seed) // створюємо генератор випадкових чисел із заданим seed

    fun start() { // функція запуску генерації даних
        transformerIds.forEach { id -> scope.launchGenerator(id) } // для кожного трансформатора запускаємо корутину генерації
    }

    private fun CoroutineScope.launchGenerator(id: TransformerId) =
        launch { // приватна функція, що запускає корутину для конкретного трансформатора
            var voltage = 10.0 + rnd.nextDouble(
                -0.5,
                0.5
            ) // початкове значення напруги (кВ) із невеликим шумом
            var current =
                150.0 + rnd.nextDouble(-10.0, 10.0) // початкове значення струму (А) із шумом
            var temp = 45.0 + rnd.nextDouble(-2.0, 2.0) // початкова температура (°C) із шумом
            var load = 0.6 + rnd.nextDouble(
                -0.05,
                0.05
            ) // початковий коефіцієнт навантаження [0..1] із шумом

            while (isActive) { // цикл, що працює поки корутина активна
                voltage += rnd.nextDouble(-0.1, 0.1) + spike(
                    0.02,
                    2.5
                ) // змінюємо напругу з шумом та можливим імпульсом
                current += rnd.nextDouble(-3.0, 3.0) + spike(
                    0.015,
                    80.0
                ) // змінюємо струм з шумом та можливим імпульсом
                temp += rnd.nextDouble(
                    -0.15,
                    0.15
                ) + heating(load) // змінюємо температуру з шумом та впливом навантаження
                load = (load + rnd.nextDouble(-0.02, 0.02)).clamp(
                    0.0,
                    1.0
                ) // змінюємо навантаження та обмежуємо його в межах [0..1]

                val now = System.currentTimeMillis() // отримуємо поточний час у мілісекундах
                output.trySend( // надсилаємо нові дані у канал
                    Telemetry(
                        transformerId = id, // ідентифікатор трансформатора
                        timestampMs = now, // час вимірювання
                        voltageKv = voltage.clamp(
                            6.0,
                            18.0
                        ), // напруга, обмежена в діапазоні [6..18] кВ
                        currentA = current.clamp(
                            10.0,
                            400.0
                        ), // струм, обмежений у діапазоні [10..400] А
                        temperatureC = temp.clamp(
                            20.0,
                            110.0
                        ), // температура, обмежена у діапазоні [20..110] °C
                        loadFactor = load // коефіцієнт навантаження
                    )
                )
                delay(Rates.emission) // затримка перед наступною генерацією (0.5 секунди)
            }
        }

    private fun spike(
        prob: Double,
        magnitude: Double
    ): Double { // функція для моделювання випадкових імпульсів
        return if (rnd.nextDouble() < prob) (if (rnd.nextBoolean()) magnitude else -magnitude) else 0.0 // з ймовірністю prob додаємо або віднімаємо імпульс
    }

    private fun heating(load: Double): Double { // функція для моделювання нагріву залежно від навантаження
        return (load - 0.5) * 0.4 // чим більше навантаження, тим більший приріст температури
    }
}

// ====================== РЕПОЗИТОРІЙ (КАНАЛ → FLOW) ======================

class MonitoringRepository(
    private val input: Channel<Telemetry>,
    private val windowSize: Int = 6 // ~3 секунди при 0.5s
) {
    val rawFlow: Flow<Telemetry> = input.receiveAsFlow()

    val processedFlow: Flow<ProcessedTelemetry> = rawFlow
        .buffer(64)
        .scan(Smoother(windowSize)) { sm, t -> sm.update(t) }
        .mapNotNull { sm ->
            val t = sm.last ?: return@mapNotNull null
            ProcessedTelemetry(
                raw = t,
                smoothedVoltageKv = sm.avgVoltage,
                smoothedCurrentA = sm.avgCurrent,
                smoothedTemperatureC = sm.avgTemp,
                anomaly = detectAnomaly(t)
            )
        }

    private fun detectAnomaly(t: Telemetry): AnomalyFlag = when {
        t.voltageKv > 16.5 || t.voltageKv < 7.0 -> AnomalyFlag.VOLTAGE_SPIKE
        t.currentA > 350.0 -> AnomalyFlag.OVERCURRENT
        t.temperatureC > 95.0 -> AnomalyFlag.OVERHEAT
        t.loadFactor < 0.2 -> AnomalyFlag.UNDERLOAD
        t.loadFactor > 0.9 -> AnomalyFlag.OVERLOAD
        else -> AnomalyFlag.NONE
    }

    private class Smoother(private val windowSize: Int) {
        private val buf = ArrayDeque<Telemetry>()
        var last: Telemetry? = null
            private set

        val avgVoltage: Double get() = if (buf.isEmpty()) 0.0 else buf.map { it.voltageKv }.average()
        val avgCurrent: Double get() = if (buf.isEmpty()) 0.0 else buf.map { it.currentA }.average()
        val avgTemp: Double get() = if (buf.isEmpty()) 0.0 else buf.map { it.temperatureC }.average()

        fun update(t: Telemetry): Smoother {
            last = t
            buf += t
            if (buf.size > windowSize) buf.removeFirst()
            return this
        }
    }
}

// ====================== VIEWMODEL ======================

class MonitoringViewModel : ViewModel() {
    private val channel = Channel<Telemetry>(Channel.UNLIMITED)
    private val repository = MonitoringRepository(channel)
    private val generator = TelemetryGenerator(
        scope = viewModelScope,
        output = channel,
        transformerIds = listOf(TransformerId("TX-001"), TransformerId("TX-014"), TransformerId("TX-099")),
        seed = 123
    )

    private val _state = MutableStateFlow(
        UiState(
            availableIds = listOf(TransformerId("TX-001"), TransformerId("TX-014"), TransformerId("TX-099"))
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        start()
    }

    fun start() {
        generator.start()
        _state.update { it.copy(isRunning = true, selectedTransformer = it.availableIds.firstOrNull()) }

        viewModelScope.launch {
            repository.processedFlow
                .filter { s -> s.raw.transformerId == _state.value.selectedTransformer }
                .collect { p ->
                    val t = p.raw.timestampMs
                    _state.update { st ->
                        st.copy(
                            voltageSeries = (st.voltageSeries + SeriesPoint(t, p.smoothedVoltageKv)).takeLast(256),
                            currentSeries = (st.currentSeries + SeriesPoint(t, p.smoothedCurrentA)).takeLast(256),
                            temperatureSeries = (st.temperatureSeries + SeriesPoint(t, p.smoothedTemperatureC)).takeLast(256),
                            loadSeries = (st.loadSeries + SeriesPoint(t, p.raw.loadFactor)).takeLast(256),
                            lastAnomaly = p.anomaly
                        )
                    }
                }
        }
    }

    fun switchTransformer(id: TransformerId) {
        _state.update { it.copy(selectedTransformer = id) }
    }
}

// ====================== UI (JETPACK COMPOSE) ======================

@Composable
fun MonitoringScreen(vm: MonitoringViewModel) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(state)
        Controls(
            ids = state.availableIds,
            selected = state.selectedTransformer,
            onSelect = { vm.switchTransformer(it) }
        )
        ChartCard("Напруга (кВ)", state.voltageSeries, Color(0xFF6A5ACD))
        ChartCard("Струм (А)", state.currentSeries, Color(0xFFFF8C00))
        ChartCard("Температура (°C)", state.temperatureSeries, Color(0xFFDC143C))
        ChartCard("Коеф. навантаження", state.loadSeries, Color(0xFF2E8B57), yMin = 0.0, yMax = 1.0)

        StatusCard(state.lastAnomaly)
    }
}

@Composable
private fun Header(state: UiState) {
    Text(
        text = "Моніторинг трансформаторів",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )
    Text(
        text = "Активний: ${state.isRunning} | Поточний: ${state.selectedTransformer?.id ?: "—"}",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun Controls(
    ids: List<TransformerId>,
    selected: TransformerId?,
    onSelect: (TransformerId) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ids.forEach { id ->
            val active = id == selected
            Button(
                onClick = { onSelect(id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Color(0xFF1E90FF) else Color(0xFF4682B4)
                )
            ) { Text(id.id) }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    series: List<SeriesPoint>,
    color: Color,
    yMin: Double? = null,
    yMax: Double? = null
) {
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LineChart(series = series, lineColor = color, yMin = yMin, yMax = yMax, gridColor = Color(0x33444444))
        }
    }
}

@Composable
private fun StatusCard(flag: AnomalyFlag) {
    val (label, col) = when (flag) {
        AnomalyFlag.NONE -> "Стан: стабільний" to Color(0xFF2E8B57)
        AnomalyFlag.VOLTAGE_SPIKE -> "Аномалія: імпульс напруги" to Color(0xFF6A5ACD)
        AnomalyFlag.OVERCURRENT -> "Аномалія: перевищення струму" to Color(0xFFFF8C00)
        AnomalyFlag.OVERHEAT -> "Аномалія: перегрів" to Color(0xFFDC143C)
        AnomalyFlag.UNDERLOAD -> "Аномалія: недовантаження" to Color(0xFF1E90FF)
        AnomalyFlag.OVERLOAD -> "Аномалія: перевантаження" to Color(0xFF8B0000)
    }
    Card {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = col, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun LineChart(
    series: List<SeriesPoint>,
    lineColor: Color,
    yMin: Double? = null,
    yMax: Double? = null,
    gridColor: Color = Color.LightGray
) {
    val values = series.map { it.value }
    val minYd = yMin ?: (values.minOrNull() ?: 0.0)
    val maxYd = yMax ?: (values.maxOrNull() ?: 1.0)
    val minX = series.minOfOrNull { it.t } ?: 0L
    val maxX = series.maxOfOrNull { it.t } ?: 1L
    val spanX = (maxX - minX).coerceAtLeast(1)
    val spanY = (maxYd - minYd).toFloat().coerceAtLeast(1e-6f)
    val minY = minYd.toFloat()
    val maxY = maxYd.toFloat()

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)) {

        // Сітка
        val gridSteps = 4
        repeat(gridSteps + 1) { i ->
            val y = size.height * (i.toFloat() / gridSteps.toFloat())
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val path = Path()
        series.forEachIndexed { idx, p ->
            val x = (((p.t - minX).toFloat() / spanX.toFloat()) * size.width)
            val y = size.height - (((p.value.toFloat() - minY) / spanY) * size.height)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ====================== ГОЛОВНА АКТИВНІСТЬ ======================

class MainActivity : ComponentActivity() {
    private val vm = MonitoringViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { MonitoringScreen(vm) }
            }
        }
    }
}
