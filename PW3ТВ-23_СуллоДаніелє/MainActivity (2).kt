@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.transformermonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.random.Random

// ============================================================
// -------------- ІНТЕРФЕЙСИ ТА КЛАСИ ПРИСТРОЇВ ---------------
// ============================================================

/**
 * Базовий інтерфейс для всіх пристроїв, які можуть бути змодельовані та моніторитись.
 * Визначає:
 *  - унікальний ідентифікатор (id)
 *  - назву пристрою (name)
 *  - метод simulate() — для імітації змін стану пристрою
 *  - метод getStatus() — для відображення поточного стану у вигляді тексту
 */
interface IMonitoredDevice {
    val id: Int
    val name: String
    fun simulate()
    fun getStatus(): String
}

/**
 * Абстрактний клас для електричних пристроїв.
 * Визначає базові параметри: напруга, струм, назву, id.
 * Містить абстрактну функцію calculatePower(), яку кожен нащадок має реалізувати.
 */
abstract class ElectricalDevice(
    override val id: Int,
    override val name: String,
    open var voltage: Double,
    open var current: Double
) : IMonitoredDevice {
    abstract fun calculatePower(): Double
}

/**
 * Клас Transformer — моделює реальний електричний трансформатор.
 * Містить такі параметри:
 *  - напруга (voltage)
 *  - струм (current)
 *  - ККД (efficiency)
 *  - температура (temperature)
 *  - відстань передачі енергії (distance)
 *
 * Реалізує кілька допоміжних функцій для фізичних розрахунків.
 */
class Transformer(
    id: Int,
    name: String,
    voltage: Double,
    current: Double,
    var efficiency: Double,
    var temperature: Double,
    var distance: Double = 1.0
) : ElectricalDevice(id, name, voltage, current) {

    /** Розрахунок потужності з урахуванням ККД */
    override fun calculatePower(): Double = voltage * current * (efficiency / 100)

    /** Коригування ККД залежно від температури */
    fun adjustEfficiency() {
        if (temperature > 60) efficiency -= 0.5
        else if (temperature < 30) efficiency += 0.3
        efficiency = efficiency.coerceIn(80.0, 99.0) // обмежуємо значення в допустимих межах
    }

    /** Імітація зміни навантаження (струму) трансформатора */
    fun simulateLoadFluctuation() {
        val change = Random.nextDouble(-5.0, 5.0)
        current = (current + change).coerceAtLeast(1.0)
    }

    /** Розрахунок втрат при передачі енергії */
    fun calculateTransmissionLoss(): Double {
        val lossCoeff = 0.05 * distance
        return calculatePower() * lossCoeff
    }

    /** Розрахунок теплових втрат */
    fun calculateHeatLoss(): Double = 0.01 * (temperature - 25).pow(2)

    /** Розрахунок коефіцієнта потужності */
    fun calculatePowerFactor(angle: Double): Double = cos(angle)

    /** Повна симуляція зміни параметрів трансформатора */
    override fun simulate() {
        simulateLoadFluctuation()
        adjustEfficiency()
        temperature += Random.nextDouble(-1.0, 1.5)
    }

    /** Формує короткий рядок зі статусом трансформатора */
    override fun getStatus(): String {
        return "$name | Потужність: %.2f кВт | ККД: %.1f%% | T=%.1f°C".format(
            calculatePower() / 1000, efficiency, temperature
        )
    }
}

/**
 * Клас TemperatureSensor — датчик температури.
 * Змінює температуру в невеликому діапазоні при кожній симуляції.
 */
class TemperatureSensor(
    override val id: Int,
    override val name: String,
    var temperature: Double
) : IMonitoredDevice {
    override fun simulate() {
        temperature += Random.nextDouble(-0.3, 0.4)
    }

    override fun getStatus(): String = "$name: Температура = %.1f°C".format(temperature)
}

/**
 * Клас VoltageSensor — датчик напруги.
 * Імітує коливання напруги у визначеному діапазоні.
 */
class VoltageSensor(
    override val id: Int,
    override val name: String,
    var voltage: Double
) : IMonitoredDevice {
    override fun simulate() {
        voltage += Random.nextDouble(-2.0, 2.0)
    }

    override fun getStatus(): String = "$name: Напруга = %.1f В".format(voltage)
}

// ============================================================
// ------------------ СИСТЕМА МОНІТОРИНГУ ---------------------
// ============================================================

/**
 * Об'єкт MonitoringSystem — центральна частина програми.
 * Зберігає список усіх пристроїв та журнал подій.
 * Містить функції для додавання, автозаповнення, симуляції і розрахунків.
 */
object MonitoringSystem {
    val devices = mutableStateListOf<IMonitoredDevice>() // поточний список пристроїв
    val logs = mutableStateListOf<String>() // журнал дій користувача

    /** Додавання нового пристрою в систему */
    fun addDevice(device: IMonitoredDevice) {
        devices.add(device)
        logs.add("Додано новий пристрій: ${device.name}")
    }

    /** Автоматичне заповнення прикладами пристроїв */
    fun autoFill() {
        devices.clear()
        addDevice(Transformer(1, "T-1", 220.0, 60.0, 97.0, 45.0))
        addDevice(Transformer(2, "T-2", 110.0, 80.0, 95.0, 55.0))
        addDevice(TemperatureSensor(3, "TempSensor-1", 47.0))
        addDevice(VoltageSensor(4, "VoltSensor-1", 220.0))
        logs.add("Автозаповнення пристроїв виконано.")
    }

    /** Імітація всіх пристроїв */
    fun simulateAll() {
        devices.forEach { it.simulate() }
        logs.add("Виконано симуляцію усіх пристроїв.")
    }

    /** Обчислення сумарної потужності всіх трансформаторів */
    fun getOverallPower(): Double =
        devices.filterIsInstance<Transformer>().sumOf { it.calculatePower() }

    /** Середня температура трансформаторів */
    fun getAverageTemperature(): Double {
        val temps = devices.filterIsInstance<Transformer>().map { it.temperature }
        return if (temps.isEmpty()) 0.0 else temps.average()
    }
}

// ============================================================
// ------------------ ОСНОВНИЙ ІНТЕРФЕЙС ----------------------
// ============================================================

/**
 * Головна функція інтерфейсу користувача.
 * Містить три вкладки (екрани):
 *  1) Додавання трансформаторів
 *  2) Моніторинг стану
 *  3) Журнал подій
 */
@Composable
fun TransformerMonitoringApp() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            // Нижня панель навігації між вкладками
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Додавання") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Моніторинг") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Журнал") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { padding ->
        // Відображення вибраного екрану
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> AddTransformerScreen()
                1 -> MonitoringScreen()
                2 -> LogScreen()
            }
        }
    }
}

// ============================================================
// ------------------ ЕКРАН 1: ДОДАВАННЯ ----------------------
// ============================================================

/**
 * Екран для додавання нового трансформатора.
 * Містить поля введення параметрів і кнопки:
 *  - "Додати трансформатор"
 *  - "Автозаповнення"
 */
@Composable
fun AddTransformerScreen() {
    var voltage by remember { mutableStateOf(TextFieldValue("220")) }
    var current by remember { mutableStateOf(TextFieldValue("50")) }
    var efficiency by remember { mutableStateOf(TextFieldValue("96")) }
    var temperature by remember { mutableStateOf(TextFieldValue("40")) }

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Додавання нового трансформатора", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Поля для введення параметрів
            OutlinedTextField(value = voltage, onValueChange = { voltage = it },
                label = { Text("Напруга (В)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = current, onValueChange = { current = it },
                label = { Text("Струм (А)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = efficiency, onValueChange = { efficiency = it },
                label = { Text("ККД (%)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = temperature, onValueChange = { temperature = it },
                label = { Text("Температура (°C)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))

            // Кнопки
            Row {
                Button(onClick = {
                    val t = Transformer(
                        id = MonitoringSystem.devices.size + 1,
                        name = "T-${MonitoringSystem.devices.size + 1}",
                        voltage = voltage.text.toDoubleOrNull() ?: 220.0,
                        current = current.text.toDoubleOrNull() ?: 50.0,
                        efficiency = efficiency.text.toDoubleOrNull() ?: 96.0,
                        temperature = temperature.text.toDoubleOrNull() ?: 40.0
                    )
                    MonitoringSystem.addDevice(t)
                }) { Text("Додати трансформатор") }

                Spacer(Modifier.width(8.dp))

                Button(onClick = { MonitoringSystem.autoFill() }) {
                    Text("Автозаповнення")
                }
            }
        }
    }
}

// ============================================================
// ------------------ ЕКРАН 2: МОНІТОРИНГ ---------------------
// ============================================================

/**
 * Екран для перегляду усіх пристроїв і запуску симуляції.
 * Відображає список пристроїв та підсумкові розрахунки.
 */
@Composable
fun MonitoringScreen() {
    var output by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Відображення усіх пристроїв
        items(MonitoringSystem.devices) { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(device.getStatus(), modifier = Modifier.padding(16.dp))
            }
        }

        // Кнопка запуску симуляції
        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                MonitoringSystem.simulateAll()
                output =
                    "Загальна потужність: %.2f кВт\nСередня температура: %.1f °C".format(
                        MonitoringSystem.getOverallPower() / 1000,
                        MonitoringSystem.getAverageTemperature()
                    )
            }) { Text("Запустити симуляцію") }

            Spacer(Modifier.height(8.dp))
            Text(output)
        }
    }
}

// ============================================================
// ------------------ ЕКРАН 3: ЖУРНАЛ ПОДІЙ -------------------
// ============================================================

/**
 * Екран журналу, де зберігаються усі дії користувача:
 *  - додавання пристроїв
 *  - автозаповнення
 *  - запуск симуляції
 */
@Composable
fun LogScreen() {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        items(MonitoringSystem.logs.reversed()) { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Text(log, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

// ============================================================
// ------------------ MAIN ACTIVITY ----------------------------
// ============================================================

/**
 * Точка входу програми.
 * Встановлює основний UI-контент через Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TransformerMonitoringApp() }
    }
}
