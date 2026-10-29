package sv.edu.atlasbrisa.vista

import android.view.MotionEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import sv.edu.atlasbrisa.controlador.ClimaDeCiudad
import sv.edu.atlasbrisa.R
import sv.edu.atlasbrisa.modelo.Pais
import java.io.File

@Composable
fun MapaCiudades(pais: Pais, filas: List<ClimaDeCiudad>, idCiudadElegida: Long?, versionEnfoque: Int,
    usarUnidadesInglesas: Boolean, alElegir: (Long) -> Unit, alMostrarTodas: () -> Unit) {
    val contexto = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current
    val abrirEnlace = LocalUriHandler.current
    val alElegirActual by rememberUpdatedState(alElegir)
    // Creamos el mapa una vez por país y después solo actualizamos sus marcadores.
    val mapa = remember(pais.iso2) {
        Configuration.getInstance().apply {
            userAgentValue = "AtlasBrisa/1.0 (Android; educational country explorer)"
            osmdroidBasePath = File(contexto.cacheDir, "maps")
            osmdroidTileCache = File(contexto.cacheDir, "maps/tiles")
            tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 40L * 1024 * 1024
        }
        MapView(contexto).apply {
            setTileSource(XYTileSource("OpenStreetMap", 0, 19, 256, ".png",
                arrayOf("https://tile.openstreetmap.org/"), "© OpenStreetMap contributors"))
            setMultiTouchControls(true)
            minZoomLevel = 2.0
            maxZoomLevel = 18.0
            controller.setZoom(5.0)
            pais.coordenadas?.let { controller.setCenter(GeoPoint(it.latitud, it.longitud)) }
            setOnTouchListener { vista, evento ->
                when (evento.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> vista.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP -> {
                        vista.parent?.requestDisallowInterceptTouchEvent(false)
                        vista.performClick()
                    }
                    MotionEvent.ACTION_CANCEL -> vista.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }
    // Pausamos y liberamos el mapa cuando Android cierra esta vista.
    DisposableEffect(mapa, cicloDeVida) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        cicloDeVida.lifecycle.addObserver(observador)
        if (cicloDeVida.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapa.onResume()
        onDispose {
            cicloDeVida.lifecycle.removeObserver(observador)
            mapa.onPause()
            mapa.onDetach()
        }
    }

    // Elegir una ciudad centra el mapa; "Ver todas" vuelve a encuadrar la lista.
    LaunchedEffect(mapa, filas.map { it.ciudad.id }, idCiudadElegida, versionEnfoque) {
        val elegido = filas.firstOrNull { it.ciudad.id == idCiudadElegida }
        if (elegido != null) {
            mapa.controller.animateTo(GeoPoint(elegido.ciudad.coordenadas.latitud, elegido.ciudad.coordenadas.longitud), 11.0, 650L)
        } else if (filas.isNotEmpty()) {
            val puntos = filas.map { GeoPoint(it.ciudad.coordenadas.latitud, it.ciudad.coordenadas.longitud) }
            mapa.post {
                if (puntos.size == 1) {
                    mapa.controller.setCenter(puntos.first())
                    mapa.controller.setZoom(9.0)
                } else mapa.zoomToBoundingBox(BoundingBox.fromGeoPoints(puntos), false, 64)
            }
        }
    }

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${filas.size} ciudades en el mapa", fontSize = 12.sp, color = TextoSecundario, modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = alMostrarTodas) { Text("Ver todas", fontSize = 12.sp) }
        }
        Box(Modifier.fillMaxWidth().height(285.dp).clip(RoundedCornerShape(20.dp))) {
            AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize(), update = { vista ->
                InfoWindow.closeAllInfoWindowsOn(vista)
                vista.overlays.clear()
                // Cada marcador muestra el nombre y el clima de su ciudad.
                filas.forEach { fila ->
                    vista.overlays.add(Marker(vista).apply {
                        icon = ContextCompat.getDrawable(contexto,
                            if (fila.ciudad.esCapital) R.drawable.ic_map_capital else R.drawable.ic_map_pin)
                        position = GeoPoint(fila.ciudad.coordenadas.latitud, fila.ciudad.coordenadas.longitud)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = fila.ciudad.nombre + if (fila.ciudad.esCapital) " · Capital" else ""
                        snippet = fila.clima?.let { clima ->
                            "${decimal(if (usarUnidadesInglesas) clima.temperaturaF else clima.temperaturaC)} ${if (usarUnidadesInglesas) "°F" else "°C"} · ${clima.condicion}"
                        } ?: if (fila.cargando) "Consultando clima…" else "Clima no disponible"
                        setOnMarkerClickListener { marcador, _ ->
                            alElegirActual(fila.ciudad.id)
                            marcador.showInfoWindow()
                            true
                        }
                        if (fila.ciudad.id == idCiudadElegida) showInfoWindow()
                    })
                }
                vista.invalidate()
            })
            Surface(modifier = Modifier.align(Alignment.BottomStart).clickable {
                abrirEnlace.openUri("https://www.openstreetmap.org/copyright")
            }) {
                Text("© OpenStreetMap contributors", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
            }
        }
        TextButton(onClick = { abrirEnlace.openUri("https://www.openstreetmap.org/copyright") }, contentPadding = PaddingValues(0.dp)) {
            Text("© OpenStreetMap contributors · © GeoNames · WeatherAPI.com", fontSize = 9.sp, color = TextoSecundario)
        }
    }
}
