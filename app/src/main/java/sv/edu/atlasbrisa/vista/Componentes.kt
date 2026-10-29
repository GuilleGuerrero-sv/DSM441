package sv.edu.atlasbrisa.vista

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import sv.edu.atlasbrisa.modelo.Pais
import java.text.NumberFormat
import java.util.Locale

// Damos formato a los números para que se lean más fácil.
fun poblacion(valor: Long?): String = valor?.let { NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-SV")).format(it) } ?: "No disponible"
fun decimal(valor: Double): String = String.format(Locale.forLanguageTag("es-SV"), "%.1f", valor)

// Este globo se dibuja aquí mismo; no hace falta descargar una imagen.
@Composable
fun Globo(modifier: Modifier = Modifier, color: Color = AzulBarca) {
    Canvas(modifier) {
        val lado = size.minDimension
        val izquierda = (size.width - lado) / 2
        val arriba = (size.height - lado) / 2
        drawCircle(color, lado / 2 - 2, Offset(size.width / 2, size.height / 2), style = Stroke(2.dp.toPx()))
        drawOval(color, Offset(izquierda + lado * .25f, arriba + 2), Size(lado * .5f, lado - 4), style = Stroke(1.5.dp.toPx()))
        drawOval(color, Offset(izquierda + 2, arriba + lado * .29f), Size(lado - 4, lado * .42f), style = Stroke(1.5.dp.toPx()))
        drawLine(color, Offset(izquierda + 2, size.height / 2), Offset(izquierda + lado - 2, size.height / 2), 1.5.dp.toPx())
    }
}

@Composable
fun Bandera(pais: Pais, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(AzulSuave), contentAlignment = Alignment.Center) {
        Text(pais.banderaEmoji.ifBlank { pais.iso2 }, fontSize = 23.sp, color = AzulBarca)
        AsyncImage(model = pais.urlBandera, contentDescription = "Bandera de ${pais.nombreComun}", modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun TituloDeSeccion(titulo: String, subtitulo: String? = null) {
    Column {
        Text(titulo, fontSize = 25.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
        subtitulo?.let { Text(it, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun Cargando(mensaje: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(mensaje, color = TextoSecundario)
    }
}

@Composable
fun TarjetaError(mensaje: String, alReintentar: (() -> Unit)? = null) {
    Surface(color = GranaSuave, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No pudimos completar la consulta", fontWeight = FontWeight.SemiBold, color = GranaBarca)
            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = GranaBarca)
            if (alReintentar != null) OutlinedButton(onClick = alReintentar) { Text("Volver a intentar") }
        }
    }
}

@Composable
fun TarjetaVacia(titulo: String, mensaje: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            Text(mensaje, color = TextoSecundario, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
