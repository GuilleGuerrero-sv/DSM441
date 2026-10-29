package sv.edu.atlasbrisa.vista

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import sv.edu.atlasbrisa.controlador.*
import sv.edu.atlasbrisa.modelo.*

// Juntamos los datos del país, el mapa y las tarjetas del clima.
@Composable
fun PantallaDetallePais(estado: EstadoAtlas, controlador: ControladorAtlas) {
    val estadoLista = rememberLazyListState()
    val alcance = rememberCoroutineScope()
    val pais = estado.pais
    LazyColumn(state = estadoLista, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (estado.cargando) item { Cargando("Conociendo el país…") }
        estado.error?.let { item { TarjetaError(it, controlador::reintentar) } }
        if (pais != null) {
            item(key = "country") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Bandera(pais, Modifier.size(80.dp, 57.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pais.nombreComun, fontFamily = FontFamily.Serif, fontSize = 33.sp, lineHeight = 37.sp)
                        Text(pais.nombreOficial, color = TextoSecundario, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item(key = "facts") { DatosDelPais(pais) }
            item(key = "cities_heading") {
                TituloDeSeccion("Ciudades y clima", "Toca una ciudad para encontrarla en el mapa.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Unidades", color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    FilterChip(selected = !estado.usarUnidadesInglesas, onClick = { controlador.cambiarUnidades(false) }, label = { Text("°C · km/h") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = estado.usarUnidadesInglesas, onClick = { controlador.cambiarUnidades(true) }, label = { Text("°F · mph") })
                }
            }
            item(key = "map") {
                MapaCiudades(pais = pais, filas = estado.ciudades, idCiudadElegida = estado.idCiudadElegida,
                    versionEnfoque = estado.cambioDeEnfoque, usarUnidadesInglesas = estado.usarUnidadesInglesas,
                    alElegir = controlador::elegirCiudad, alMostrarTodas = controlador::mostrarTodasLasCiudades)
            }
            if (estado.cargandoCiudades) item { Cargando("Ubicando ciudades…") }
            estado.errorCiudades?.let { item { TarjetaError(it, controlador::cargarCiudades) } }
            if (!estado.cargandoCiudades && estado.errorCiudades == null && estado.ciudades.isEmpty()) item {
                TarjetaVacia("Sin ciudades disponibles", "GeoNames no devolvió ciudades con coordenadas válidas para este país. Puedes seguir consultando su información.")
            }
            items(estado.ciudades, key = { it.ciudad.id }) { fila ->
                TarjetaCiudad(fila, estado.usarUnidadesInglesas, estado.idCiudadElegida == fila.ciudad.id,
                    alElegir = {
                        controlador.elegirCiudad(fila.ciudad.id)
                        // Subimos al mapa para que veas la ciudad que acabas de elegir.
                        alcance.launch { estadoLista.animateScrollToItem(3) }
                    }, alReintentar = { controlador.reintentarClima(fila.ciudad.id) })
            }
            item {
                Text(if (estado.demo) "Datos ilustrativos para practicar. El mapa necesita Internet para cargar su fondo."
                    else "Países: REST Countries · Ciudades: GeoNames\nClima: WeatherAPI.com · Mapas: OpenStreetMap",
                    color = TextoSecundario, fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun DatosDelPais(pais: Pais) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Dato("CAPITAL", pais.capitales.joinToString().ifBlank { "Sin capital oficial" }, Modifier.weight(1f))
                Dato("POBLACIÓN", poblacion(pais.poblacion), Modifier.weight(1f))
            }
            HorizontalDivider(color = FondoClaro)
            Dato("REGIÓN / SUBREGIÓN", "${regionesConocidas.firstOrNull { it.codigo == pais.region }?.titulo ?: pais.region} · ${pais.subregion}")
            Dato("CÓDIGOS ISO Y COI",
                "Alfa-2: ${pais.iso2.ifBlank { "No disponible" }} - Alfa-3: ${pais.iso3.ifBlank { "No disponible" }}\n" +
                    "Numérico: ${pais.codigoNumerico ?: "No disponible"} - COI: ${pais.codigoCoi ?: "No disponible"}")
            Dato("MONEDAS", pais.monedas.joinToString("\n").ifBlank { "No disponibles" })
            Dato("IDIOMAS", pais.idiomas.joinToString().ifBlank { "No disponibles" })
            Dato("COORDENADAS DEL PAÍS", pais.coordenadas?.let { "${it.latitud}, ${it.longitud}" } ?: "No disponibles")
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(etiqueta, fontSize = 9.sp, letterSpacing = 1.sp, color = TextoSecundario)
        Text(valor, fontSize = 14.sp, color = TextoPrincipal)
    }
}

@Composable
private fun TarjetaCiudad(fila: ClimaDeCiudad, usarUnidadesInglesas: Boolean, elegido: Boolean, alElegir: () -> Unit, alReintentar: () -> Unit) {
    val esCapital = fila.ciudad.esCapital
    // La capital conserva su fondo grana aunque selecciones otra ciudad.
    val fondo = when {
        esCapital -> GranaSuave
        elegido -> AzulSuave
        else -> Color.White
    }
    Card(onClick = alElegir, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = when {
            elegido -> BorderStroke(3.dp, AzulBarca)
            esCapital -> BorderStroke(2.dp, GranaBarca)
            else -> null
        }) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (esCapital) {
                        Surface(color = GranaBarca, contentColor = Color.White, shape = RoundedCornerShape(50.dp)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("★", color = AmarilloBarca, fontSize = 14.sp)
                                Text("CAPITAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(fila.ciudad.nombre, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                        color = if (esCapital) GranaBarca else TextoPrincipal)
                    Text("${poblacion(fila.ciudad.poblacion)} habitantes", color = TextoSecundario, fontSize = 12.sp)
                }
                Text("Ver en mapa ↗", color = AzulBarca, fontSize = 11.sp)
            }
            if (fila.cargando) Cargando("Consultando clima…", Modifier.padding(0.dp))
            fila.clima?.let { clima ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(model = clima.urlIcono, contentDescription = clima.condicion, modifier = Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${decimal(if (usarUnidadesInglesas) clima.temperaturaF else clima.temperaturaC)} ${if (usarUnidadesInglesas) "°F" else "°C"}",
                            fontSize = 30.sp, fontFamily = FontFamily.Serif)
                        Text(clima.condicion, fontSize = 13.sp, color = TextoSecundario)
                    }
                }
                Text("Viento: ${decimal(if (usarUnidadesInglesas) clima.vientoMph else clima.vientoKmh)} ${if (usarUnidadesInglesas) "mph" else "km/h"}  ·  Humedad: ${clima.humedad}%",
                    fontSize = 12.sp, color = TextoSecundario)
                Text("Actualización: ${clima.actualizadoEn}", fontSize = 10.sp, color = TextoSecundario)
            }
            fila.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                OutlinedButton(onClick = alReintentar) { Text("Reintentar clima") }
            }
        }
    }
}
