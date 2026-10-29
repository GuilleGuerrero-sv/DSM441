package sv.edu.atlasbrisa.modelo

import java.text.Normalizer

// Estas clases solo guardan datos; aquí no dibujamos pantallas.
data class Region(val codigo: String, val titulo: String, val subtitulo: String, val cantidadPaises: Int = 0)

val regionesConocidas = listOf(
    Region("Americas", "América", "De norte a sur"),
    Region("Europe", "Europa", "Historias en cada ciudad"),
    Region("Asia", "Asia", "Un mundo de contrastes"),
    Region("Africa", "África", "Horizontes por descubrir"),
    Region("Oceania", "Oceanía", "Entre islas y océanos"),
    Region("Antarctic", "Antártida", "El extremo del mundo"),
)

data class Coordenadas(val latitud: Double, val longitud: Double) {
    val esValida: Boolean get() = latitud.isFinite() && longitud.isFinite() &&
        latitud in -90.0..90.0 && longitud in -180.0..180.0
}

// Lo que necesitamos mostrar de cada país.
data class Pais(
    val iso2: String,
    val iso3: String,
    val nombreComun: String,
    val nombreOficial: String,
    val capitales: List<String>,
    val region: String,
    val subregion: String,
    val poblacion: Long?,
    val urlBandera: String?,
    val banderaEmoji: String,
    val monedas: List<String>,
    val idiomas: List<String>,
    val coordenadas: Coordenadas?,
    // Son texto para conservar ceros como el 032 de Argentina.
    val codigoNumerico: String? = null,
    val codigoCoi: String? = null,
)

data class Ciudad(
    val id: Long,
    val nombre: String,
    val poblacion: Long,
    val coordenadas: Coordenadas,
    val esCapital: Boolean,
)

data class Clima(
    val temperaturaC: Double,
    val temperaturaF: Double,
    val condicion: String,
    val urlIcono: String?,
    val vientoKmh: Double,
    val vientoMph: Double,
    val humedad: Int,
    val actualizadoEn: String,
)

// Así podemos buscar "Mexico" aunque el nombre lleve tilde.
fun String.paraBuscar(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "").lowercase().trim()

// Tanto la API real como la demo tienen que ofrecer estas mismas consultas.
interface RepositorioAtlas {
    suspend fun regiones(): List<Region>
    suspend fun paises(region: String): List<Pais>
    suspend fun pais(iso2: String): Pais
    suspend fun ciudades(pais: Pais): List<Ciudad>
    suspend fun clima(ciudad: Ciudad, actualizar: Boolean = false): Clima
}

class ErrorAtlas(override val message: String) : Exception(message)
