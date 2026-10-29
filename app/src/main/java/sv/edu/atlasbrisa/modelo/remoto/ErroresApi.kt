package sv.edu.atlasbrisa.modelo.remoto

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import sv.edu.atlasbrisa.modelo.ErrorAtlas
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// Cambiamos los códigos del servicio por mensajes que se entiendan.
internal fun errorGeoNames(estadoServicio: EstadoGeoNames): ErrorAtlas = ErrorAtlas(when (estadoServicio.codigo) {
    10 -> "GeoNames: usuario inválido o servicios web sin habilitar. Activa Free Web Services en tu cuenta."
    11, 15, 17 -> "GeoNames no encontró ciudades para este país."
    18, 19, 20 -> "GeoNames alcanzó su límite de consultas. Intenta más tarde."
    13 -> "GeoNames tardó demasiado en responder. Vuelve a intentar."
    22 -> "GeoNames está ocupado. Intenta más tarde."
    else -> "GeoNames no pudo completar la consulta (código ${estadoServicio.codigo})."
})

internal fun errorHttp(servicio: String, estadoServicio: Int, codigoApi: Int?): ErrorAtlas = ErrorAtlas(when {
    servicio == "WeatherAPI" && codigoApi == 1006 -> "WeatherAPI no encontró esta ubicación. El resto de las ciudades sigue disponible."
    estadoServicio == 429 || (servicio == "WeatherAPI" && codigoApi == 2007) -> "$servicio alcanzó su límite de consultas. Intenta más tarde."
    estadoServicio == 401 || estadoServicio == 403 || (servicio == "WeatherAPI" && codigoApi in listOf(1002, 2006, 2008, 2009)) ->
        "$servicio: credencial inválida, deshabilitada o sin permiso. Revisa secrets.properties y vuelve a compilar."
    estadoServicio == 404 -> "$servicio no encontró la información solicitada."
    estadoServicio in 500..599 -> "$servicio está temporalmente fuera de servicio. Intenta más tarde."
    else -> "$servicio no pudo completar la consulta (HTTP $estadoServicio)."
})

internal suspend fun <T> consultarServicio(servicio: String, accion: suspend () -> T): T = try {
    accion()
} catch (error: CancellationException) {
    // Cancelar al cambiar de pantalla es normal; no lo mostramos como un fallo.
    throw error
} catch (error: ErrorAtlas) {
    throw error
} catch (error: HttpException) {
    val codigoApi = runCatching {
        JsonParser.parseString(error.response()?.errorBody()?.string()).asJsonObject.objeto("error")?.numero("code")?.toInt()
    }.getOrNull()
    throw errorHttp(servicio, error.code(), codigoApi)
} catch (error: SocketTimeoutException) {
    throw ErrorAtlas("$servicio tardó demasiado en responder. Revisa tu conexión y vuelve a intentar.")
} catch (error: UnknownHostException) {
    throw ErrorAtlas("No se pudo conectar a $servicio. Revisa tu conexión a Internet.")
} catch (error: IOException) {
    throw ErrorAtlas("Se perdió la conexión con $servicio. Vuelve a intentar.")
} catch (error: Exception) {
    throw ErrorAtlas("$servicio devolvió datos que no se pudieron interpretar. Vuelve a intentar.")
}
