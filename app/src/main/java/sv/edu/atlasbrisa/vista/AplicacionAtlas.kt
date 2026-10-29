package sv.edu.atlasbrisa.vista

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sv.edu.atlasbrisa.controlador.*
import sv.edu.atlasbrisa.modelo.*

// Esta vista solo muestra el estado y manda al controlador lo que tocas.
@Composable
fun AplicacionAtlas(controlador: ControladorAtlas) {
    // Compose vuelve a dibujar lo necesario cuando cambia este estado.
    val estado by controlador.estado.collectAsStateWithLifecycle()
    var mostrarConfiguracion by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = estado.pantalla != Pantalla.REGIONES, onBack = controlador::volver)
    Scaffold(containerColor = FondoClaro, topBar = {
        Column(Modifier.background(FondoClaro).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (estado.pantalla == Pantalla.REGIONES) Globo(Modifier.size(28.dp))
                else TextButton(onClick = controlador::volver, contentPadding = PaddingValues(0.dp)) { Text("← Atrás") }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("ATLAS BRISA", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 13.sp)
                    Text("Países, ciudades y clima", color = TextoSecundario, fontSize = 11.sp)
                }
                TextButton(onClick = { mostrarConfiguracion = true }) { Text("Conectar") }
            }
            if (estado.demo) Surface(color = AmarilloSuave) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("DEMOSTRACIÓN · Datos de ejemplo", fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { controlador.iniciar(false) }) { Text("Salir", fontSize = 12.sp) }
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (estado.pantalla) {
                Pantalla.REGIONES -> PantallaRegiones(estado, controlador::abrirRegion, { controlador.iniciar(true) }, controlador::reintentar)
                Pantalla.PAISES -> PantallaPaises(estado, controlador::abrirPais, controlador::reintentar)
                Pantalla.DETALLE -> PantallaDetallePais(estado, controlador)
            }
        }
    }
    if (mostrarConfiguracion) DialogoConfiguracion(alCerrar = { mostrarConfiguracion = false }, alVerDemo = { mostrarConfiguracion = false; controlador.iniciar(true) })
}

@Composable
private fun PantallaRegiones(estado: EstadoAtlas, alElegirRegion: (Region) -> Unit, alVerDemo: () -> Unit, alReintentar: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Surface(shape = RoundedCornerShape(26.dp), color = AzulOscuro) {
                Box(Modifier.fillMaxWidth().heightIn(min = 225.dp).background(Brush.linearGradient(listOf(AzulBarca, GranaBarca))).padding(24.dp)) {
                    Globo(Modifier.size(130.dp).align(Alignment.TopEnd).offset(x = 35.dp, y = 70.dp), Color.White.copy(alpha = 0.14f))
                    Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TU PRÓXIMO DESCUBRIMIENTO", color = AmarilloBarca, fontSize = 10.sp, letterSpacing = 1.4.sp)
                        Text("El mundo,\na tu ritmo.", fontSize = 40.sp, lineHeight = 43.sp,
                            fontFamily = FontFamily.Serif, color = Color.White)
                        Text("Conoce un país. Encuentra sus ciudades.\nDescubre cómo se siente hoy.",
                            color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
        if (estado.cargando) item { Cargando("Conectando con el mundo…") }
        estado.error?.let { mensaje -> item {
            TarjetaError(mensaje, alReintentar)
            Spacer(Modifier.height(8.dp))
            Button(onClick = alVerDemo, modifier = Modifier.fillMaxWidth()) { Text("Explorar demostración") }
        } }
        item { TituloDeSeccion("Elige un horizonte", "Empieza por una región") }
        items(estado.regiones.chunked(2)) { pareja ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pareja.forEach { region ->
                    val colorTarjeta = when (region.codigo) {
                        "Americas" -> AzulSuave
                        "Europe" -> GranaSuave
                        "Asia" -> AzulSuave
                        "Africa" -> AmarilloSuave
                        "Oceania" -> GranaSuave
                        else -> AzulSuave
                    }
                    Card(onClick = { alElegirRegion(region) }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = colorTarjeta)) {
                        Spacer(Modifier.fillMaxWidth().height(4.dp).background(if (estado.regiones.indexOf(region) % 2 == 0) AzulBarca else GranaBarca))
                        Column(Modifier.padding(16.dp).heightIn(min = 136.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("0${estado.regiones.indexOf(region) + 1}", fontSize = 11.sp, color = TextoSecundario)
                                Text("↗", fontSize = 23.sp, color = AzulBarca)
                            }
                            Column {
                                Text(region.titulo, fontFamily = FontFamily.Serif, fontSize = 24.sp, color = TextoPrincipal)
                                Text(if (region.cantidadPaises > 0) "${region.cantidadPaises} ${if (region.cantidadPaises == 1) "país" else "países"}${if (estado.demo) " de ejemplo" else ""}" else region.subtitulo,
                                    fontSize = 11.sp, color = TextoSecundario, lineHeight = 16.sp)
                            }
                        }
                    }
                }
                if (pareja.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item { Text("Un lugar nuevo empieza con curiosidad.", color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)) }
    }
}

@Composable
private fun PantallaPaises(estado: EstadoAtlas, alElegirPais: (String) -> Unit, alReintentar: () -> Unit) {
    var busqueda by rememberSaveable(estado.regionElegida?.codigo) { mutableStateOf("") }
    // Buscamos por nombre, capital o ISO, sin preocuparnos por las tildes.
    val filtrados = remember(busqueda, estado.paises) {
        estado.paises.filter { pais -> busqueda.paraBuscar().let { it in pais.nombreComun.paraBuscar() ||
            pais.capitales.any { capital -> it in capital.paraBuscar() } || it in pais.iso2.paraBuscar() } }
    }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("EXPLORA LA REGIÓN", fontSize = 10.sp, letterSpacing = 1.5.sp, color = TextoSecundario)
            Text(estado.regionElegida?.titulo.orEmpty(), fontFamily = FontFamily.Serif, fontSize = 39.sp)
            Text("Cada país tiene algo que contarte.", color = TextoSecundario)
        }
        item {
            OutlinedTextField(value = busqueda, onValueChange = { busqueda = it }, label = { Text("Buscar país o capital") },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
        }
        if (estado.cargando) item { Cargando("Buscando países…") }
        estado.error?.let { item { TarjetaError(it, alReintentar) } }
        if (!estado.cargando && estado.error == null && filtrados.isEmpty()) item {
            TarjetaVacia("Sin resultados", if (busqueda.isBlank()) "No hay países disponibles en esta región." else "Prueba con otro país, capital o código ISO.")
        }
        items(filtrados, key = { it.iso2 }) { pais ->
            Card(onClick = { alElegirPais(pais.iso2) }, shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Bandera(pais, Modifier.size(62.dp, 44.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pais.nombreComun, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(pais.capitales.joinToString().ifBlank { "Sin capital oficial" }, fontSize = 13.sp, color = TextoSecundario)
                    }
                    Text("→", color = AzulBarca, fontSize = 23.sp)
                }
            }
        }
    }
}

@Composable
private fun DialogoConfiguracion(alCerrar: () -> Unit, alVerDemo: () -> Unit) {
    val abrirEnlace = LocalUriHandler.current
    AlertDialog(onDismissRequest = alCerrar, title = { Text("Conecta tu atlas") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Crea las cuentas, completa secrets.properties en Android Studio y vuelve a ejecutar la app.")
            TextButton(onClick = { abrirEnlace.openUri("https://restcountries.com/sign-up") }) { Text("1. REST Countries · token") }
            TextButton(onClick = { abrirEnlace.openUri("https://www.geonames.org/manageaccount") }) { Text("2. GeoNames · usuario + habilitar servicios") }
            TextButton(onClick = { abrirEnlace.openUri("https://www.weatherapi.com/signup") }) { Text("3. WeatherAPI · API key") }
            Text("La guía paso a paso está en docs/CREDENCIALES.md. La demostración permite explorar con datos locales de ejemplo.",
                style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }, confirmButton = { TextButton(onClick = alCerrar) { Text("Entendido") } },
        dismissButton = { TextButton(onClick = alVerDemo) { Text("Ver demostración") } })
}
