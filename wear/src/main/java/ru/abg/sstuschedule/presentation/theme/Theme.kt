package ru.abg.sstuschedule.presentation.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

@Composable
fun SGTUScheduleTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme

    // Пытаемся получить системную палитру (на основе циферблата).
    // Если не поддерживается — используем стандартную тему Wear M3.
    val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        runCatching { dynamicColorScheme(context) }.getOrNull() ?: fallback
    } else {
        fallback
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}