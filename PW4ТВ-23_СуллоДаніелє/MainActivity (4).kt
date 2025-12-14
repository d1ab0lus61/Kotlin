package com.example.transformermonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transformermonitoring.ui.theme.TransformerMonitoringTheme
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


// 1. DATA CLASS

data class Transformer(
    val id: Int, // Ідентифікатор трансформатора
    val name: String,  // Назва трансформатора
    val maxTemperature: Double, // Максимально допустима температура
    val maxVoltage: Double, // Максимально допустима напруга
    val isActive: Boolean // Стан трансформатора
)

// 2. Делеговані функції

class MonitoringStatusDelegate : ReadWriteProperty<Any?, Boolean> {
    private var value: Boolean = false // Приватне поле для зберігання стану
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean { // Отримання стану моніторингу
        return value
    }
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) { // Зміна стану моніторингу
        this.value = value
    }
}

// 3. EXTENSION-функції


fun Transformer.isTemperatureCritical(currentTemp: Double): Boolean { // Перевірка перевищення температури
    return currentTemp > maxTemperature
}

fun Transformer.isVoltageCritical(currentVoltage: Double): Boolean { // Перевірка перевищення напруги
    return currentVoltage > maxVoltage
}

fun Double.formatTemperature(): String { // Форматування температури
    return String.format("%.1f °C", this)
}

fun Double.formatVoltage(): String { // Форматування напруги
    return String.format("%.1f В", this)
}

fun Double.isNegative(): Boolean { // Перевірка від’ємного значення
    return this < 0
}

fun Boolean.toMonitoringStatus(): String { // Перетворення логічного стану у текст
    return if (this) "Моніторинг активний" else "Моніторинг зупинений"
}

fun Transformer.isWorkingNormally(temp: Double, voltage: Double): Boolean { // Перевірка, чи трансформатор у нормальному стані
    return temp <= maxTemperature && voltage <= maxVoltage
}

// 4. TRY / CATCH

class TransformerMonitoringManager {

    // Делегована змінна стану моніторингу
    var monitoringStatus: Boolean by MonitoringStatusDelegate()

    // Запуск системи моніторингу
    fun startMonitoring(): String {
        return try {
            monitoringStatus = true
            "Система моніторингу трансформатора запущена"
        } catch (e: Exception) {
            "Помилка запуску системи"
        }
    }

    // Зупинка системи моніторингу
    fun stopMonitoring(): String {
        return try {
            monitoringStatus = false
            "Система моніторингу трансформатора зупинена"
        } catch (e: Exception) {
            "Помилка зупинки системи"
        }
    }

    // Перевірка стану трансформатора
    fun checkTransformerState(
        transformer: Transformer,
        temperature: Double,
        voltage: Double
    ): String {
        return try {

            // Перевірка на від’ємні значення
            if (temperature.isNegative() || voltage.isNegative()) {
                "Помилка: введені від’ємні значення"
            }
            // Перевірка критичних параметрів
            else if (
                transformer.isTemperatureCritical(temperature) ||
                transformer.isVoltageCritical(voltage)
            ) {
                "Аварійний стан. Температура: ${temperature.formatTemperature()}, Напруга: ${voltage.formatVoltage()}"
            }
            // Нормальний режим роботи
            else {
                "Нормальний режим. Температура: ${temperature.formatTemperature()}, Напруга: ${voltage.formatVoltage()}"
            }

        } catch (e: Exception) {
            "Помилка аналізу стану трансформатора"
        }
    }
}

// 5. Main файл
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Створення трансформатора
        val transformer = Transformer(
            id = 1,
            name = "Силовий трансформатор ТМ-1000",
            maxTemperature = 90.0,
            maxVoltage = 10000.0,
            isActive = true
        )

        // Створення менеджера моніторингу
        val manager = TransformerMonitoringManager()

        setContent {

            TransformerMonitoringTheme {

                // Текст результату
                var resultText by remember {
                    mutableStateOf("Моніторинг не запущений")
                }

                // Поле введення температури
                var temperatureInput by remember {
                    mutableStateOf("")
                }

                // Поле введення напруги
                var voltageInput by remember {
                    mutableStateOf("")
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),

                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Заголовок
                    Text(
                        text = "Система моніторингу трансформаторів",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // Поле введення температури
                    OutlinedTextField(
                        value = temperatureInput,
                        onValueChange = { temperatureInput = it },
                        label = { Text("Введіть температуру (°C)") }
                    )

                    // Поле введення напруги
                    OutlinedTextField(
                        value = voltageInput,
                        onValueChange = { voltageInput = it },
                        label = { Text("Введіть напругу (В)") }
                    )

                    // Кнопка запуску моніторингу
                    Button(onClick = {
                        resultText = manager.startMonitoring()
                    }) {
                        Text("Запустити моніторинг")
                    }

                    // Кнопка зупинки моніторингу
                    Button(onClick = {
                        resultText = manager.stopMonitoring()
                    }) {
                        Text("Зупинити моніторинг")
                    }

                    // Кнопка перевірки стану
                    Button(onClick = {

                        val temp = temperatureInput.toDoubleOrNull()
                        val voltage = voltageInput.toDoubleOrNull()

                        resultText = if (temp != null && voltage != null) {
                            manager.checkTransformerState(transformer, temp, voltage)
                        } else {
                            "Помилка: введіть коректні числові значення"
                        }

                    }) {
                        Text("Перевірити стан")
                    }

                    Divider(thickness = 2.dp)

                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
