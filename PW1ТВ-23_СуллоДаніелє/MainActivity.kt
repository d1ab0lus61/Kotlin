package com.example.transformermonitoringsystem

// Імпортуємо необхідні компоненти Android та Jetpack Compose
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.transformermonitoringsystem.ui.theme.TransformerMonitoringSystemTheme

// Головна активність програми
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Встановлюємо контент за допомогою Jetpack Compose
        setContent {
            TransformerMonitoringSystemTheme {
                // Поверхня з фоновим кольором
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Викликаємо головний екран
                    MainScreen()
                }
            }
        }
    }
}

// Головна функція інтерфейсу користувача
@Composable
fun MainScreen() {
    // Змінна для введення імені
    var name by remember { mutableStateOf("") }
    // Змінна для збереження привітання
    var greeting by remember { mutableStateOf("") }

    // Змінна для введення числа
    var numberInput by remember { mutableStateOf("") }
    // Змінна для повідомлення після перевірки числа
    var numberMessage by remember { mutableStateOf("") }

    // Змінна для контролю кольору тексту
    var isRed by remember { mutableStateOf(false) }

    // Вертикальне розташування елементів з відступом
    Column(modifier = Modifier.padding(16.dp)) {

        // Блок: Введення імені та привітання
        Text(text = "Введіть ваше ім’я:")
        TextField(
            value = name,
            onValueChange = { name = it }, // Оновлення значення при введенні
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { greeting = "Привіт, $name!" }, // Формування привітання
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Показати привітання")
        }
        // Виведення привітання, якщо воно не порожнє
        if (greeting.isNotEmpty()) {
            Text(text = greeting, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp)) // Відступ між блоками

        // Блок: Введення числа та умовна логіка
        Text(text = "Введіть число:")
        TextField(
            value = numberInput,
            onValueChange = { numberInput = it }, // Оновлення введеного числа
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val number = numberInput.toIntOrNull() // Перетворення введеного тексту в число
                // Умовна логіка для формування повідомлення
                numberMessage = when {
                    number == null -> "Будь ласка, введіть коректне число."
                    number < 10 -> "Число менше 10."
                    number in 10..20 -> "Число між 10 і 20."
                    else -> "Число більше 20."
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Перевірити число")
        }
        // Виведення повідомлення після перевірки
        if (numberMessage.isNotEmpty()) {
            Text(text = numberMessage, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp)) // Відступ між блоками

        // Блок: Зміна стану елементів (колір тексту)
        Text(text = "Натисніть кнопку для зміни кольору тексту:")
        Button(
            onClick = { isRed = !isRed }, // Зміна стану: червоний ↔ синій
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Змінити колір")
        }
        // Текст, який змінює колір залежно від стану
        Text(
            text = "Цей текст змінює колір",
            color = if (isRed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Прев’ю для перегляду інтерфейсу в Android Studio
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TransformerMonitoringSystemTheme {
        MainScreen()
    }
}
