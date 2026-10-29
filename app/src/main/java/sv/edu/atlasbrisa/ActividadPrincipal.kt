package sv.edu.atlasbrisa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import sv.edu.atlasbrisa.controlador.ControladorAtlas
import sv.edu.atlasbrisa.controlador.Pantalla
import sv.edu.atlasbrisa.modelo.RepositorioAtlasDemo
import sv.edu.atlasbrisa.modelo.remoto.RepositorioAtlasRemoto
import sv.edu.atlasbrisa.vista.AplicacionAtlas
import sv.edu.atlasbrisa.vista.TemaAtlas

// Aquí arranca la app y juntamos el modelo, la vista y el controlador.
class ActividadPrincipal : ComponentActivity() {
    private lateinit var controlador: ControladorAtlas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val estadoGuardado = savedInstanceState
        controlador = ControladorAtlas(
            RepositorioAtlasRemoto.crear(BuildConfig.REST_COUNTRIES_TOKEN, BuildConfig.GEONAMES_USERNAME, BuildConfig.WEATHER_API_KEY),
            RepositorioAtlasDemo(), lifecycleScope,
        )
        // Si giraste el teléfono, volvemos al país que estabas viendo.
        controlador.iniciar(
            demo = estadoGuardado?.getBoolean("demo") ?: false,
            region = estadoGuardado?.getString("region"),
            codigoPais = estadoGuardado?.getString("country"),
            usarUnidadesInglesas = estadoGuardado?.getBoolean("imperial") ?: false,
        )
        setContent { TemaAtlas { AplicacionAtlas(controlador) } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val estadoPorGuardar = outState
        // Guardamos lo justo para no perder la pantalla al girar el teléfono.
        val estado = controlador.estado.value
        estadoPorGuardar.putBoolean("demo", estado.demo)
        estadoPorGuardar.putBoolean("imperial", estado.usarUnidadesInglesas)
        if (estado.pantalla != Pantalla.REGIONES) estadoPorGuardar.putString("region", estado.regionElegida?.codigo)
        if (estado.pantalla == Pantalla.DETALLE) estadoPorGuardar.putString("country", estado.codigoPaisElegido)
        super.onSaveInstanceState(estadoPorGuardar)
    }
}
