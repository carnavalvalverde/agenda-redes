package es.carnavalverde.agenda.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.carnavalverde.agenda.R

object C {
    val fondo = Color(0xFF1C1C22)
    val tarjeta = Color(0xFF26262E)
    val tarjeta2 = Color(0xFF30303A)
    val verde = Color(0xFF9CC45A)
    val texto = Color(0xFFF6F6F1)
    val texto2 = Color(0xFFA5A5A0)
    val granate = Color(0xFF8A2140)
    val naranja = Color(0xFFE7B04A)
    val amarillo = Color(0xFFE9D46B)
    val rojo = Color(0xFFE0605A)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun sans(w: Int) = Font(
    R.font.source_sans3, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun playfair(w: Int) = Font(
    R.font.playfair_italic, FontWeight(w), FontStyle.Italic,
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val SourceSans = FontFamily(sans(400), sans(600), sans(700))
val Playfair = FontFamily(playfair(400), playfair(600), playfair(700))

private val base = TextStyle(fontFamily = SourceSans)
private val titulo = TextStyle(fontFamily = Playfair, fontStyle = FontStyle.Italic)

private val tipografia = Typography(
    displayLarge = titulo.copy(fontSize = 56.sp, fontWeight = FontWeight.W600),
    headlineLarge = titulo.copy(fontSize = 30.sp, fontWeight = FontWeight.W600),
    headlineMedium = titulo.copy(fontSize = 26.sp, fontWeight = FontWeight.W600),
    headlineSmall = titulo.copy(fontSize = 22.sp, fontWeight = FontWeight.W600),
    titleLarge = titulo.copy(fontSize = 22.sp, fontWeight = FontWeight.W600),
    titleMedium = base.copy(fontSize = 17.sp, fontWeight = FontWeight.W600),
    titleSmall = base.copy(fontSize = 15.sp, fontWeight = FontWeight.W600),
    bodyLarge = base.copy(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = base.copy(fontSize = 13.sp, color = C.texto2),
    labelLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight.W600),
    labelMedium = base.copy(fontSize = 13.sp, fontWeight = FontWeight.W600),
    labelSmall = base.copy(fontSize = 12.sp, fontWeight = FontWeight.W600),
)

private val colores = darkColorScheme(
    primary = C.verde,
    onPrimary = C.fondo,
    secondary = C.verde,
    onSecondary = C.fondo,
    background = C.fondo,
    onBackground = C.texto,
    surface = C.fondo,
    onSurface = C.texto,
    surfaceVariant = C.tarjeta,
    onSurfaceVariant = C.texto2,
    surfaceContainer = C.tarjeta,
    surfaceContainerHigh = C.tarjeta,
    surfaceContainerHighest = C.tarjeta2,
    surfaceContainerLow = C.tarjeta,
    outline = C.texto2,
    outlineVariant = Color(0xFF3A3A44),
    error = C.rojo,
)

@Composable
fun AgendaTema(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, typography = tipografia) {
        Surface(color = C.fondo, contentColor = C.texto, content = content)
    }
}

@Composable
fun Chip(texto: String, fondo: Color = C.tarjeta2, color: Color = C.texto, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
