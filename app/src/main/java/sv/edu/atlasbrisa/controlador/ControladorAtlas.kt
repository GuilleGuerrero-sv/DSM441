package sv.edu.atlasbrisa.controlador

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import sv.edu.atlasbrisa.modelo.*

enum class Pantalla { REGIONES, PAISES, DETALLE }
// Cada ciudad lleva su propia carga y su propio error.
data class ClimaDeCiudad(val ciudad: Ciudad, val clima: Clima? = null, val cargando: Boolean = true, val error: String? = null)
// La pantalla lee esta información y se actualiza cuando cambia.
data class EstadoAtlas(
    val pantalla: Pantalla = Pantalla.REGIONES,
    val demo: Boolean = false,
    val regiones: List<Region> = regionesConocidas,
    val regionElegida: Region? = null,
    val paises: List<Pais> = emptyList(),
    val pais: Pais? = null,
    val codigoPaisElegido: String? = null,
    val ciudades: List<ClimaDeCiudad> = emptyList(),
    val idCiudadElegida: Long? = null,
    val cargando: Boolean = false,
    val cargandoCiudades: Boolean = false,
    val error: String? = null,
    val errorCiudades: String? = null,
    val usarUnidadesInglesas: Boolean = false,
    val cambioDeEnfoque: Int = 0,
)

// Aquí llegan los toques de la pantalla y decidimos qué datos hay que consultar.
class ControladorAtlas(
    private val repositorioReal: RepositorioAtlas,
    private val repositorioDemo: RepositorioAtlas,
    private val alcance: CoroutineScope,
) {
    private val estadoMutable = MutableStateFlow(EstadoAtlas())
    val estado = estadoMutable.asStateFlow()
    private var tareaNavegacion: Job? = null
    private var tareaCiudades: Job? = null
    private val reintentosClima = mutableMapOf<Long, Job>()
    private var numeroDeConsulta = 0
    // Pedimos tres climas a la vez para no llenar la API de consultas.
    private val turnosClima = Semaphore(3)
    private val repositorio: RepositorioAtlas get() = if (estado.value.demo) repositorioDemo else repositorioReal

    fun iniciar(demo: Boolean = false, region: String? = null, codigoPais: String? = null, usarUnidadesInglesas: Boolean = false) {
        cancelarPendientes()
        estadoMutable.value = EstadoAtlas(demo = demo, usarUnidadesInglesas = usarUnidadesInglesas)
        tareaNavegacion = alcance.launch {
            estadoMutable.update { it.copy(cargando = true) }
            try {
                val regiones = repositorio.regiones()
                estadoMutable.update { it.copy(regiones = regiones, cargando = false) }
                val elegido = regiones.firstOrNull { it.codigo == region }
                if (elegido != null) {
                    estadoMutable.update { it.copy(regionElegida = elegido, pantalla = Pantalla.PAISES, cargando = true) }
                    val paises = repositorio.paises(elegido.codigo)
                    estadoMutable.update { it.copy(paises = paises, cargando = false) }
                    if (codigoPais != null) abrirPais(codigoPais)
                }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) { estadoMutable.update { it.copy(cargando = false, error = error.mensajeAmigable()) } }
        }
    }

    // Al elegir una región, buscamos sus países y avisamos que estamos cargando.
    fun abrirRegion(region: Region) {
        cancelarPendientes()
        estadoMutable.update { it.copy(pantalla = Pantalla.PAISES, regionElegida = region,
            paises = emptyList(), pais = null, codigoPaisElegido = null, ciudades = emptyList(), cargando = true, error = null, errorCiudades = null) }
        tareaNavegacion = alcance.launch {
            try {
                val resultado = repositorio.paises(region.codigo)
                estadoMutable.update { it.copy(paises = resultado, cargando = false) }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) { estadoMutable.update { it.copy(cargando = false, error = error.mensajeAmigable()) } }
        }
    }

    fun abrirPais(iso2: String) {
        cancelarPendientes()
        estadoMutable.update { it.copy(pantalla = Pantalla.DETALLE, pais = null, codigoPaisElegido = iso2, ciudades = emptyList(),
            idCiudadElegida = null, cargando = true, cargandoCiudades = false, error = null, errorCiudades = null) }
        tareaNavegacion = alcance.launch {
            try {
                val pais = repositorio.pais(iso2)
                estadoMutable.update { it.copy(pais = pais, cargando = false) }
                cargarCiudades()
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) { estadoMutable.update { it.copy(cargando = false, error = error.mensajeAmigable()) } }
        }
    }

    // Primero van las ciudades. Después buscamos el clima de cada una.
    fun cargarCiudades() {
        val pais = estado.value.pais ?: return
        tareaCiudades?.cancel()
        reintentosClima.values.forEach { it.cancel() }
        reintentosClima.clear()
        // Este número evita mezclar resultados si cambias rápido de país.
        val numeroConsultaActual = ++numeroDeConsulta
        estadoMutable.update { it.copy(cargandoCiudades = true, errorCiudades = null, ciudades = emptyList(), idCiudadElegida = null) }
        tareaCiudades = alcance.launch {
            try {
                val ciudades = repositorio.ciudades(pais)
                if (numeroDeConsulta != numeroConsultaActual) return@launch
                estadoMutable.update { it.copy(cargandoCiudades = false, ciudades = ciudades.map(::ClimaDeCiudad)) }
                supervisorScope { ciudades.forEach { ciudad -> launch { consultarClima(ciudad, numeroConsultaActual) } } }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) {
                if (numeroDeConsulta == numeroConsultaActual) estadoMutable.update { it.copy(cargandoCiudades = false, errorCiudades = error.mensajeAmigable()) }
            }
        }
    }

    private suspend fun consultarClima(ciudad: Ciudad, numeroConsultaActual: Int, actualizar: Boolean = false) {
        try {
            val clima = turnosClima.withPermit { repositorio.clima(ciudad, actualizar) }
            actualizarCiudad(ciudad.id, numeroConsultaActual) { it.copy(clima = clima, cargando = false, error = null) }
        } catch (error: CancellationException) { throw error
        } catch (error: Exception) {
            actualizarCiudad(ciudad.id, numeroConsultaActual) { it.copy(cargando = false, error = error.mensajeAmigable()) }
        }
    }

    // Repetimos solo el clima que falló, sin recargar todo el país.
    fun reintentarClima(idCiudad: Long) {
        val fila = estado.value.ciudades.firstOrNull { it.ciudad.id == idCiudad } ?: return
        if (fila.cargando) return
        val numeroConsultaActual = numeroDeConsulta
        actualizarCiudad(idCiudad, numeroConsultaActual) { it.copy(cargando = true, error = null) }
        reintentosClima[idCiudad] = alcance.launch {
            if (numeroConsultaActual != numeroDeConsulta) return@launch
            consultarClima(fila.ciudad, numeroConsultaActual, actualizar = true)
        }
    }

    private fun actualizarCiudad(id: Long, numeroConsultaActual: Int, cambiarFila: (ClimaDeCiudad) -> ClimaDeCiudad) {
        if (numeroConsultaActual == numeroDeConsulta) estadoMutable.update { estado ->
            estado.copy(ciudades = estado.ciudades.map { if (it.ciudad.id == id) cambiarFila(it) else it })
        }
    }

    fun elegirCiudad(id: Long) { estadoMutable.update { it.copy(idCiudadElegida = id, cambioDeEnfoque = it.cambioDeEnfoque + 1) } }
    fun mostrarTodasLasCiudades() { estadoMutable.update { it.copy(idCiudadElegida = null, cambioDeEnfoque = it.cambioDeEnfoque + 1) } }
    fun cambiarUnidades(usarInglesas: Boolean) { estadoMutable.update { it.copy(usarUnidadesInglesas = usarInglesas) } }

    fun reintentar() = when (estado.value.pantalla) {
        Pantalla.REGIONES -> iniciar(estado.value.demo, usarUnidadesInglesas = estado.value.usarUnidadesInglesas)
        Pantalla.PAISES -> estado.value.regionElegida?.let(::abrirRegion) ?: Unit
        Pantalla.DETALLE -> estado.value.codigoPaisElegido?.let(::abrirPais) ?: Unit
    }

    fun volver() {
        cancelarPendientes()
        estadoMutable.update {
            when (it.pantalla) {
                Pantalla.DETALLE -> it.copy(pantalla = Pantalla.PAISES, pais = null, codigoPaisElegido = null, ciudades = emptyList(), cargando = false,
                    cargandoCiudades = false, idCiudadElegida = null, error = null, errorCiudades = null)
                else -> it.copy(pantalla = Pantalla.REGIONES, regionElegida = null, cargando = false, error = null, errorCiudades = null)
            }
        }
    }

    // Si ya saliste de esa pantalla, sus consultas dejan de hacer falta.
    private fun cancelarPendientes() {
        numeroDeConsulta++
        tareaNavegacion?.cancel()
        tareaCiudades?.cancel()
        reintentosClima.values.forEach { it.cancel() }
        reintentosClima.clear()
    }
    private fun Exception.mensajeAmigable() = if (this is ErrorAtlas) message else "No fue posible completar la consulta. Vuelve a intentar."
}
