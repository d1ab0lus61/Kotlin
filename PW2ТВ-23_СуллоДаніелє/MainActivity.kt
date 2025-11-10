@file:OptIn(ExperimentalMaterial3Api::class) // Дозвіл на використання експериментального API Material 3

package com.example.transformermonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.random.Random

// =====================================================================
// ---------------------- МОДЕЛЬ ТРАНСФОРМАТОРА -----------------------
// =====================================================================
/*
 * Клас, який описує один трансформатор і його характеристики.
 * Використовується як базова одиниця для зберігання даних у системі.
 */
data class Transformer(
    val id: Int,               // Унікальний ідентифікатор трансформатора
    var name: String,          // Назва або маркування трансформатора
    var voltage: Double,       // Напруга у вольтах
    var current: Double,       // Струм у амперах
    var efficiency: Double,    // ККД у відсотках
    var temperature: Double,   // Температура у °C
    var distance: Double = 1.0 // Відстань лінії електропередачі (в км)
) {

    // ---- Обчислення потужності трансформатора ----
    fun calculatePower(): Double = voltage * current * (efficiency / 100)
    // Формула: P = U * I * η
    // Повертає потужність у Ватах

    // ---- Корекція ККД залежно від температури ----
    fun adjustEfficiency() {
        if (temperature > 60) efficiency -= 0.5   // При перегріві ККД зменшується
        else if (temperature < 30) efficiency += 0.3 // При низьких температурах трохи зростає
        efficiency = efficiency.coerceIn(80.0, 99.0) // Обмеження в межах [80%, 99%]
    }

    // ---- Імітація зміни навантаження ----
    fun simulateLoadFluctuation() {
        val change = Random.nextDouble(-5.0, 5.0)  // Випадкова зміна струму
        current = (current + change).coerceAtLeast(1.0)
    }

    // ---- Втрати енергії на лінії передачі ----
    fun calculateTransmissionLoss(): Double {
        val lossCoeff = 0.05 * distance             // 5% втрат на 1 км
        return calculatePower() * lossCoeff
    }

    // ---- Теплові втрати ----
    fun calculateHeatLoss(): Double {
        // Спрощена формула теплових втрат (залежить від відхилення від норми 25°C)
        return 0.01 * (temperature - 25).pow(2)
    }

    // ---- Коефіцієнт потужності ----
    fun calculatePowerFactor(angle: Double): Double = cos(angle)
    // Використовується для імітації фазового кута між струмом і напругою
}

// =====================================================================
// ---------------------- ОБЧИСЛЮВАЛЬНІ ФУНКЦІЇ -----------------------
// =====================================================================
/*
 * Об’єкт для глобальних розрахунків енергетичних показників
 * на основі набору трансформаторів.
 */
object EnergyCalculator {

    // ---- Загальна потужність усіх трансформаторів ----
    fun calculateTotalEnergy(vararg transformers: Transformer): Double {
        return transformers.sumOf { it.calculatePower() }
    }

    // ---- Середня температура системи ----
    fun calculateAverageTemperature(vararg transformers: Transformer): Double {
        if (transformers.isEmpty()) return 0.0
        return transformers.sumOf { it.temperature } / transformers.size
    }
}

// =====================================================================
// ---------------------- ОСНОВНА АКТИВНІСТЬ ---------------------------
// =====================================================================
/*
 * Точка входу у програму. Тут ініціалізується графічний інтерфейс
 * і викликається головна функція TransformerMonitoringApp().
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TransformerMonitoringApp() // Запуск головного UI
        }
    }
}

// =====================================================================
// ---------------------- ГОЛОВНИЙ ІНТЕРФЕЙС ---------------------------
// =====================================================================
/*
 * Це головна функція, яка створює весь інтерфейс користувача.
 * Реалізовано за допомогою Jetpack Compose.
 */
@Composable
fun TransformerMonitoringApp() {

    // Список трансформаторів, що відображається на екрані
    var transformers = remember { mutableStateListOf<Transformer>() }

    // Параметри, що вводяться користувачем
    var voltage by remember { mutableStateOf(TextFieldValue("220")) }
    var current by remember { mutableStateOf(TextFieldValue("50")) }
    var efficiency by remember { mutableStateOf(TextFieldValue("96")) }
    var temperature by remember { mutableStateOf(TextFieldValue("40")) }
    var distance by remember { mutableStateOf(TextFieldValue("1")) }

    // Змінні для збереження результатів розрахунків
    var totalEnergy by remember { mutableStateOf(0.0) }
    var avgTemp by remember { mutableStateOf(0.0) }

    // ----------- Основна структура інтерфейсу (Scaffold) -----------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Система моніторингу трансформаторів") }
            )
        }
    ) { padding ->

        // ----------- Прокручуваний список елементів -----------
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ===================================================
            // ---------- ФОРМА ВВЕДЕННЯ ПАРАМЕТРІВ -------------
            // ===================================================
            item {
                Text("Введіть параметри нового трансформатора:")
                Spacer(Modifier.height(8.dp))

                // Поля для вводу напруги та струму
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedTextField(
                        value = voltage,
                        onValueChange = { voltage = it },
                        label = { Text("Напруга (В)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it },
                        label = { Text("Струм (А)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Поля для вводу ККД та температури
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedTextField(
                        value = efficiency,
                        onValueChange = { efficiency = it },
                        label = { Text("ККД (%)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Темп. (°C)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Поле для відстані лінії
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it },
                    label = { Text("Довжина лінії (км)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // ---------- Кнопки додавання або автозаповнення ----------
                Row {
                    // Додавання власного трансформатора
                    Button(onClick = {
                        val t = Transformer(
                            id = transformers.size + 1,
                            name = "T-${transformers.size + 1}",
                            voltage = voltage.text.toDoubleOrNull() ?: 220.0,
                            current = current.text.toDoubleOrNull() ?: 50.0,
                            efficiency = efficiency.text.toDoubleOrNull() ?: 96.0,
                            temperature = temperature.text.toDoubleOrNull() ?: 40.0,
                            distance = distance.text.toDoubleOrNull() ?: 1.0
                        )
                        transformers.add(t)
                    }) {
                        Text("Додати трансформатор")
                    }

                    Spacer(Modifier.width(8.dp))

                    // Автоматичне заповнення трьох тестових трансформаторів
                    Button(onClick = {
                        transformers.clear()
                        transformers.addAll(
                            listOf(
                                Transformer(1, "T-100", 220.0, 60.0, 98.0, 45.0, 1.5),
                                Transformer(2, "T-200", 110.0, 80.0, 95.0, 55.0, 2.0),
                                Transformer(3, "T-300", 330.0, 40.0, 97.0, 48.0, 0.8)
                            )
                        )
                    }) {
                        Text("Автозаповнення")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ===================================================
            // ---------- ВІДОБРАЖЕННЯ СПИСКУ ТРАНСФОРМАТОРІВ ----
            // ===================================================
            items(transformers) { t ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${t.name} — Потужність: %.2f кВт".format(t.calculatePower() / 1000))
                        Text("Температура: ${t.temperature} °C")
                        Text("Втрати при передачі: %.2f Вт".format(t.calculateTransmissionLoss()))
                        Text("Теплові втрати: %.2f Вт".format(t.calculateHeatLoss()))
                        Text("ККД: %.2f %%".format(t.efficiency))
                    }
                }
            }

            // ===================================================
            // ---------- КНОПКА СИМУЛЯЦІЇ ------------------------
            // ===================================================
            item {
                Spacer(Modifier.height(16.dp))

                // Кнопка запуску моделювання роботи трансформаторів
                Button(onClick = {
                    transformers.forEach {
                        it.simulateLoadFluctuation()  // Імітація зміни струму
                        it.adjustEfficiency()         // Корекція ККД
                    }
                    // Підрахунок загальних показників
                    totalEnergy = EnergyCalculator.calculateTotalEnergy(*transformers.toTypedArray())
                    avgTemp = EnergyCalculator.calculateAverageTemperature(*transformers.toTypedArray())
                }) {
                    Text("Запустити симуляцію")
                }

                Spacer(Modifier.height(8.dp))

                // Відображення підсумкових результатів симуляції
                Text("Загальна потужність: %.2f кВт".format(totalEnergy / 1000))
                Text("Середня температура: %.1f °C".format(avgTemp))
            }
        }
    }
}
