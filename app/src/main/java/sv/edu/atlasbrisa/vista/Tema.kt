package sv.edu.atlasbrisa.vista

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cambiando esta paleta se actualizan los colores de toda la app.
val AzulBarca = Color(0xFF004D98)
val AzulOscuro = Color(0xFF102451)
val GranaBarca = Color(0xFFA50044)
val AmarilloBarca = Color(0xFFFFD43B)
val TextoPrincipal = Color(0xFF18254A)
val TextoSecundario = Color(0xFF59627A)
val FondoClaro = Color(0xFFF5F6FB)
val AzulSuave = Color(0xFFE5EDFC)
val GranaSuave = Color(0xFFF8E4EC)
val AmarilloSuave = Color(0xFFFFF3C2)

@Composable
fun TemaAtlas(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AzulBarca, onPrimary = Color.White,
            primaryContainer = AzulSuave, onPrimaryContainer = AzulOscuro,
            secondary = GranaBarca, onSecondary = Color.White, secondaryContainer = GranaSuave,
            onSecondaryContainer = GranaBarca, tertiary = AmarilloBarca, onTertiary = AzulOscuro,
            background = FondoClaro, onBackground = TextoPrincipal,
            surface = FondoClaro, onSurface = TextoPrincipal,
            surfaceVariant = AzulSuave, onSurfaceVariant = TextoSecundario,
            outline = Color(0xFF8C97AF), error = Color(0xFFAC2340),
        ),
        typography = Typography(), content = content,
    )
}
