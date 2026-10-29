package sv.edu.atlasbrisa.modelo.remoto

import com.google.gson.JsonObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sv.edu.atlasbrisa.modelo.*
import java.util.concurrent.TimeUnit

// Aquí consultamos las tres APIs y dejamos los datos listos para la app.
class RepositorioAtlasRemoto(
    private val servicioPaises: ServicioPaises,
    private val servicioCiudades: ServicioCiudades,
    private val servicioClima: ServicioClima,
    private val token: String,
    private val usuarioGeoNames: String,
    private val claveClima: String,
    private val horaActualMillis: () -> Long = System::currentTimeMillis,
) : RepositorioAtlas {
    private val climaGuardado = mutableMapOf<Long, Pair<Long, Clima>>()
    private val accesoAlClima = Mutex()
    private var regionesGuardadas: List<Region>? = null
    private val paisesGuardados = mutableMapOf<String, List<Pais>>()

    override suspend fun regiones(): List<Region> {
        regionesGuardadas?.let { return it }
        val todos = consultarPaises(null)
        val cantidades = todos.groupingBy { it.region }.eachCount()
        return regionesConocidas.map { it.copy(cantidadPaises = cantidades[it.codigo] ?: 0) }.also { regionesGuardadas = it }
    }

    override suspend fun paises(region: String): List<Pais> = paisesGuardados[region]
        ?: consultarPaises(region).also { paisesGuardados[region] = it }

    private suspend fun consultarPaises(region: String?): List<Pais> = consultarServicio("REST Countries") {
        revisarToken()
        val todos = mutableListOf<Pais>()
        var inicio = 0
        // La API manda los países por páginas; seguimos hasta traerlos todos.
        while (true) {
            val respuesta = servicioPaises.paises("Bearer $token", region, inicio = inicio)
            val datos = datosDePaises(respuesta)
            val pagina = datos.listaJson("objects").mapNotNull { it.comoObjetoONulo() }.map(::leerPais)
            todos += pagina.filter { it.iso2.isNotBlank() }
            val siguienteInicio = inicio + pagina.size
            val meta = datos.objeto("meta")
            val hayMas = meta?.get("more")?.takeIf { it.isJsonPrimitive }?.asBoolean
            val total = meta?.numero("total")?.toInt()
            val quedanMas = hayMas ?: (total?.let { siguienteInicio < it } ?: (pagina.size == 100))
            if (!quedanMas) break
            if (pagina.isEmpty() || siguienteInicio <= inicio || siguienteInicio > 1000) {
                throw ErrorAtlas("REST Countries devolvió una paginación inválida. Intenta más tarde.")
            }
            inicio = siguienteInicio
        }
        todos.distinctBy { it.iso2 }.sortedBy { it.nombreComun.paraBuscar() }
    }

    override suspend fun pais(iso2: String): Pais = consultarServicio("REST Countries") {
        revisarToken()
        val datos = datosDePaises(servicioPaises.pais("Bearer $token", iso2))
        datos.listaJson("objects").firstOrNull()?.comoObjetoONulo()?.let(::leerPais)
            ?.takeIf { it.iso2.equals(iso2, ignoreCase = true) }
            ?: throw ErrorAtlas("No se encontró el país seleccionado.")
    }

    private fun revisarToken() {
        if (token.isBlank()) throw ErrorAtlas("Agrega REST_COUNTRIES_TOKEN en secrets.properties para conectar los países. Puedes explorar la demostración mientras lo configuras.")
    }

    private fun datosDePaises(respuesta: JsonObject): JsonObject {
        val datos = respuesta.objeto("data") ?: throw ErrorAtlas("REST Countries no devolvió la lista de países.")
        if (datos.has("_demo")) throw ErrorAtlas("REST Countries está usando un token de demostración. Configura tu token personal para consultar todos los países.")
        return datos
    }

    override suspend fun ciudades(pais: Pais): List<Ciudad> = consultarServicio("GeoNames") {
        if (usuarioGeoNames.isBlank()) throw ErrorAtlas("Agrega GEONAMES_USERNAME en secrets.properties y habilita Free Web Services en tu cuenta.")
        val respuesta = servicioCiudades.ciudades(pais.iso2, usuarioGeoNames)
        // GeoNames también puede mandar un error dentro de una respuesta HTTP 200.
        respuesta.estadoServicio?.let { estadoServicio ->
            if (estadoServicio.codigo in listOf(11, 15, 17)) return@consultarServicio emptyList()
            throw errorGeoNames(estadoServicio)
        }
        var candidatas = respuesta.lugares.orEmpty()
        // La capital puede quedar fuera del top 10 por población (por ejemplo, Canberra).
        val incluyeCapital = candidatas.any { it.esCapital(pais) }
        if (!incluyeCapital && pais.capitales.isNotEmpty()) {
            val capitales = servicioCiudades.ciudades(pais.iso2, usuarioGeoNames, ciudades = null,
                maximoFilas = 5, tiposDeLugar = listOf("PPLC", "PPLG"))
            capitales.estadoServicio?.let { if (it.codigo !in listOf(11, 15, 17)) throw errorGeoNames(it) }
            candidatas = capitales.lugares.orEmpty() + candidatas
        }
        candidatas.mapNotNull { it.convertirEnCiudad(pais) }.distinctBy { it.id }
            .sortedWith(compareByDescending<Ciudad> { it.esCapital }.thenByDescending { it.poblacion }).take(10)
    }

    private fun CiudadGeoNames.esCapital(pais: Pais): Boolean =
        tipoLugar in listOf("PPLC", "PPLG") || pais.capitales.any { it.paraBuscar() == nombre.orEmpty().paraBuscar() }

    private fun CiudadGeoNames.convertirEnCiudad(pais: Pais): Ciudad? {
        val latitud = latitud?.toDoubleOrNull() ?: return null
        val longitud = longitud?.toDoubleOrNull() ?: return null
        val punto = Coordenadas(latitud, longitud).takeIf { it.esValida } ?: return null
        return Ciudad(idGeoNames, nombre?.takeIf { it.isNotBlank() } ?: "Ciudad sin nombre", poblacion ?: 0, punto, esCapital(pais))
    }

    override suspend fun clima(ciudad: Ciudad, actualizar: Boolean): Clima {
        if (claveClima.isBlank()) throw ErrorAtlas("Falta WEATHER_API_KEY. Agrégala en secrets.properties y vuelve a compilar.")
        // Si este clima tiene menos de diez minutos, usamos el que ya guardamos.
        if (!actualizar) accesoAlClima.withLock { climaGuardado[ciudad.id] }?.let { (momento, climaActual) ->
            if (horaActualMillis() - momento < TimeUnit.MINUTES.toMillis(10)) return climaActual
        }
        val climaActual = consultarServicio("WeatherAPI") {
            leerClima(servicioClima.climaActual(claveClima, "${ciudad.coordenadas.latitud},${ciudad.coordenadas.longitud}"))
        }
        accesoAlClima.withLock { climaGuardado[ciudad.id] = horaActualMillis() to climaActual }
        return climaActual
    }

    companion object {
        fun crear(token: String, usuarioGeoNames: String, clave: String): RepositorioAtlasRemoto {
            // Ponemos un límite de tiempo para que una consulta no se quede esperando siempre.
            val cliente = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
            fun <T> api(url: String, tipo: Class<T>): T = Retrofit.Builder().baseUrl(url).client(cliente)
                .addConverterFactory(GsonConverterFactory.create()).build().create(tipo)
            return RepositorioAtlasRemoto(
                api("https://api.restcountries.com/", ServicioPaises::class.java),
                api("https://secure.geonames.org/", ServicioCiudades::class.java),
                api("https://api.weatherapi.com/v1/", ServicioClima::class.java), token, usuarioGeoNames, clave,
            )
        }
    }
}
