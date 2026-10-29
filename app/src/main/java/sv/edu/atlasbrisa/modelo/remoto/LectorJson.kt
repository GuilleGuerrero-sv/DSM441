package sv.edu.atlasbrisa.modelo.remoto

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import sv.edu.atlasbrisa.modelo.*
import java.util.Locale

internal fun JsonObject.objeto(clave: String): JsonObject? = get(clave)?.takeIf { it.isJsonObject }?.asJsonObject
internal fun JsonObject.listaJson(clave: String): JsonArray = get(clave)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
internal fun JsonObject.texto(clave: String): String? = get(clave)?.takeIf { it.isJsonPrimitive }?.asString
internal fun JsonObject.numero(clave: String): Double? = texto(clave)?.toDoubleOrNull()
internal fun JsonElement.comoObjetoONulo(): JsonObject? = takeIf { isJsonObject }?.asJsonObject

private val espanol = Locale.forLanguageTag("es")
// Esta alternativa ayuda cuando el servicio trae el nombre pero no su código.
private val idiomasPorNombreIngles by lazy {
    Locale.getISOLanguages().associateBy { codigo ->
        Locale.forLanguageTag(codigo).getDisplayLanguage(Locale.ENGLISH).lowercase(Locale.ROOT)
    }
}

internal fun leerIdioma(idioma: JsonObject): String? {
    val nombre = idioma.texto("name")?.trim()?.takeIf { it.isNotBlank() }
    val etiquetas = listOf("bcp47", "iso639_1", "iso639_3", "iso639_2t", "iso639_2b")
        .mapNotNull { idioma.texto(it)?.trim()?.takeIf(String::isNotBlank) }
        .plus(listOfNotNull(nombre?.let { idiomasPorNombreIngles[it.lowercase(Locale.ROOT)] }))
    // Usamos el nombre en español, no el nombre nativo (como Avañe'ẽ).
    val traducido = etiquetas.asSequence().mapNotNull { etiqueta ->
        val local = Locale.forLanguageTag(etiqueta)
        local.getDisplayLanguage(espanol).takeIf {
            it.isNotBlank() && !it.equals(local.language, ignoreCase = true)
        }
    }.firstOrNull()
    val resultado = traducido ?: nombre
        ?: idioma.texto("native_name")?.trim()?.takeIf { it.isNotBlank() }
    return resultado?.replaceFirstChar { it.titlecase(espanol) }
}

// Leemos los campos de la API y los pasamos a nuestros nombres en español.
internal fun leerPais(json: JsonObject): Pais {
    val nombres = json.objeto("names")
    // Si el servicio trae traducción al español, la preferimos.
    val nombresEnEspanol = nombres?.objeto("translations")?.objeto("spa")
    val codigos = json.objeto("codes")
    val coordenadas = json.objeto("coordinates")?.let { punto ->
        val latitud = punto.numero("lat")
        val longitud = punto.numero("lng")
        if (latitud != null && longitud != null) Coordenadas(latitud, longitud).takeIf { it.esValida } else null
    }
    return Pais(
        iso2 = codigos?.texto("alpha_2").orEmpty(),
        iso3 = codigos?.texto("alpha_3").orEmpty(),
        nombreComun = nombresEnEspanol?.texto("common") ?: nombres?.texto("common") ?: "País sin nombre",
        nombreOficial = nombresEnEspanol?.texto("official") ?: nombres?.texto("official") ?: "No disponible",
        capitales = json.listaJson("capitals").mapNotNull { it.comoObjetoONulo()?.texto("name") },
        region = json.texto("region").orEmpty(),
        subregion = json.texto("subregion") ?: "No disponible",
        poblacion = json.texto("population")?.toLongOrNull(),
        urlBandera = json.objeto("flag")?.texto("url_png"),
        banderaEmoji = json.objeto("flag")?.texto("emoji").orEmpty(),
        monedas = json.listaJson("currencies").mapNotNull { elemento -> elemento.comoObjetoONulo()?.let {
            listOfNotNull(it.texto("name"), it.texto("code"), it.texto("symbol")).joinToString(" · ")
        } },
        idiomas = json.listaJson("languages").mapNotNull { elemento ->
            elemento.comoObjetoONulo()?.let(::leerIdioma)
        }.distinct(),
        coordenadas = coordenadas,
        codigoNumerico = codigos?.texto("ccn3")?.trim()
            ?.takeIf { it.matches(Regex("[0-9]{1,3}")) }?.padStart(3, '0'),
        codigoCoi = codigos?.texto("cioc")?.trim()?.takeIf { it.isNotBlank() },
    )
}

internal fun leerClima(json: JsonObject): Clima {
    val actual = json.objeto("current") ?: throw ErrorAtlas("WeatherAPI no devolvió el clima de esta ciudad.")
    val condicion = actual.objeto("condition")
    val iconoRecibido = condicion?.texto("icon")
    // Algunos íconos llegan sin https; lo agregamos para poder mostrarlos.
    val icono = when {
        iconoRecibido?.startsWith("//") == true -> "https:$iconoRecibido"
        iconoRecibido?.startsWith("http://") == true -> iconoRecibido.replaceFirst("http://", "https://")
        iconoRecibido?.startsWith("https://") == true -> iconoRecibido
        else -> null
    }
    return Clima(
        temperaturaC = actual.numero("temp_c") ?: throw ErrorAtlas("La temperatura no está disponible."),
        temperaturaF = actual.numero("temp_f") ?: throw ErrorAtlas("La temperatura no está disponible."),
        condicion = condicion?.texto("text") ?: "Sin descripción",
        urlIcono = icono,
        vientoKmh = actual.numero("wind_kph") ?: 0.0,
        vientoMph = actual.numero("wind_mph") ?: 0.0,
        humedad = actual.numero("humidity")?.toInt() ?: 0,
        actualizadoEn = actual.texto("last_updated") ?: "No disponible",
    )
}
