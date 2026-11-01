package app.colde.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.colde.ui.theme.CustomThemeColors

private enum class ColorRole(val label: String) {
    PRIMARY("Primária"),
    SECONDARY("Secundária"),
    TERTIARY("Destaque"),
    BACKGROUND("Fundo"),
    TEXT("Texto")
}

private fun CustomThemeColors.get(role: ColorRole): Int = when (role) {
    ColorRole.PRIMARY -> primary
    ColorRole.SECONDARY -> secondary
    ColorRole.TERTIARY -> tertiary
    ColorRole.BACKGROUND -> background
    ColorRole.TEXT -> text
}

private fun CustomThemeColors.set(role: ColorRole, argb: Int): CustomThemeColors = when (role) {
    ColorRole.PRIMARY -> copy(primary = argb)
    ColorRole.SECONDARY -> copy(secondary = argb)
    ColorRole.TERTIARY -> copy(tertiary = argb)
    ColorRole.BACKGROUND -> copy(background = argb)
    ColorRole.TEXT -> copy(text = argb)
}

@Composable
fun CustomThemeDialog(
    initial: CustomThemeColors,
    onSave: (CustomThemeColors) -> Unit,
    onDismiss: () -> Unit
) {
    var colors by remember { mutableStateOf(initial) }
    var role by remember { mutableStateOf(ColorRole.PRIMARY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema personalizado") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color(colors.background))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf(colors.primary, colors.secondary, colors.tertiary).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Texto de exemplo", color = Color(colors.text))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ColorRole.entries.take(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ColorRole.entries.drop(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ChannelSlider("R", colors.get(role) shr 16 and 0xFF) { v ->
                    val c = colors.get(role)
                    colors = colors.set(role, (c and 0xFF00FFFF.toInt()) or (v shl 16))
                }
                ChannelSlider("G", colors.get(role) shr 8 and 0xFF) { v ->
                    val c = colors.get(role)
                    colors = colors.set(role, (c and 0xFFFF00FF.toInt()) or (v shl 8))
                }
                ChannelSlider("B", colors.get(role) and 0xFF) { v ->
                    val c = colors.get(role)
                    colors = colors.set(role, (c and 0xFFFFFF00.toInt()) or v)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(colors) }, modifier = Modifier.testTag("CustomThemeSaveButton")) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ChannelSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    var v by remember(value) { mutableIntStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(16.dp))
        Slider(
            value = v.toFloat(),
            onValueChange = {
                v = it.toInt()
                onChange(v)
            },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text("$v", modifier = Modifier.width(32.dp))
    }
}
