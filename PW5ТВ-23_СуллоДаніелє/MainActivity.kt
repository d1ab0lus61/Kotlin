package com.example.transformermonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

data class TransformerState(
    val voltage: Double = 0.0,      // Напруга на трансформаторі в кіловольтах
    val current: Double = 0.0,      // Сила струму в амперах
    val temperature: Double = 0.0,  // Температура обмоток або масла трансформатора
    val load: Int = 0,              // Завантаження трансформатора у відсотках
    val timestamp: Long = 0L        // Час останнього оновлення параметрів
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)          // Виклик батьківського методу
        enableEdgeToEdge()                          // Включення повноекранного режиму UI

        setContent {                                // Встановлення Compose UI як контенту Activity
            TransformersScreen()                    // Запуск головного екрану програми
        }
    }
}

@Composable
fun TransformersScreen() {

    val coroutineScope = rememberCoroutineScope()
    // Створюємо scope, в якому будуть запускатися корутини оновлення даних

    val transformerIds = listOf("TR-101", "TR-202", "TR-303")
    // Список ідентифікаторів трансформаторів, що відображаються у UI

    val states = remember {
        // Зберігаємо словник (мапу) з ID → стан трансформатора
        mutableStateMapOf<String, TransformerState>().apply {
            transformerIds.forEach { this[it] = TransformerState() }
            // Для кожного трансформатора створюємо початковий пустий стан
        }
    }

    val jobs = remember { mutableStateMapOf<String, Job>() }
    // Мапа з активними корутинами для кожного трансформатора

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    // Формат часу для красивого відображення у UI

    Column(modifier = Modifier.padding(16.dp)) {
        // Колонка — основний вертикальний контейнер на екрані з відступами

        Text("Моніторинг трансформаторів", style = MaterialTheme.typography.headlineSmall)
        // Заголовок екрану

        Spacer(modifier = Modifier.height(12.dp))
        // Відступ між елементами

        Row {
            // Горизонтальне розміщення кнопок

            Button(onClick = {
                // Натискання кнопки "Запустити"

                transformerIds.forEach { id ->
                    // Проходимо по всіх трансформаторах

                    if (jobs[id]?.isActive != true) {
                        // Якщо для цього трансформатора ще не працює корутина — запускаємо нову

                        jobs[id] = coroutineScope.launch {
                            // Запуск нової корутини, що буде циклічно оновлювати дані

                            while (isActive) {
                                // Цикл оновлення, працює поки корутина не зупинена

                                delay(1500)
                                // Затримка 1.5 секунди між оновленнями

                                states[id] = TransformerState(
                                    // Генеруємо новий випадковий стан трансформатора
                                    voltage = (10000..35000).random() / 1000.0, // 10–35 кВ
                                    current = (100..600).random().toDouble(),   // 100–600 A
                                    temperature = (40..120).random().toDouble(),// 40–120 °C
                                    load = (10..100).random(),                 // 10–100 %
                                    timestamp = System.currentTimeMillis()     // Поточний час
                                )
                            }
                        }
                    }
                }
            }) {
                Text("Запустити") // Текст кнопки запуску
            }

            Spacer(modifier = Modifier.width(12.dp))
            // Горизонтальний відступ між кнопками

            Button(onClick = {
                // Натискання кнопки "Зупинити"

                jobs.values.forEach { it.cancel() }
                // Зупиняємо всі корутини, що оновлювали трансформатори

                jobs.clear()
                // Очищаємо мапу корутин
            }, enabled = jobs.isNotEmpty()) {
                // Кнопка активна тільки коли корутини працюють

                Text("Зупинити")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Відступ перед списком трансформаторів

        LazyColumn {
            // Лінива колонка — список трансформаторів

            transformerIds.forEach { id ->
                item {
                    // Елемент списку

                    val st = states[id]!!
                    // Отримуємо поточний стан трансформатора

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()            // Картка займає всю ширину
                            .padding(vertical = 6.dp), // Вертикальні відступи між картками
                        elevation = CardDefaults.cardElevation(4.dp) // Тінь картки
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Внутрішня колонка картки з відступами

                            Text("Трансформатор $id", style = MaterialTheme.typography.titleMedium)
                            // Заголовок картки з ID трансформатора

                            Spacer(modifier = Modifier.height(6.dp))
                            // Відступ після заголовка

                            Text("Час: ${if (st.timestamp == 0L) "--" else timeFormat.format(Date(st.timestamp))}")
                            // Якщо timestamp = 0 → ще не було оновлень → показуємо "--"

                            Text("Напруга: ${"%.2f".format(st.voltage)} кВ")
                            // Форматуємо напругу до 2 знаків після коми

                            Text("Струм: ${"%.1f".format(st.current)} A")
                            // Струм з 1 знаком після коми

                            Text("Температура: ${"%.1f".format(st.temperature)} °C")
                            // Температура

                            Text("Навантаження: ${st.load}%")
                            // Завантаження трансформатора у %
                        }
                    }
                }
            }
        }
    }
}
