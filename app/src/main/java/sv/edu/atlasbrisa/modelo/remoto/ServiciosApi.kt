package sv.edu.atlasbrisa.modelo.remoto

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// Aqui escribimos las rutas. Retrofit se encarga de hacer la peticion.
interface ServicioPaises {
    @GET("countries/v5")
    suspend fun paises(
        @Header("Authorization") autorizacion: String,
        @Query("region") region: String? = null,
        @Query("limit") limite: Int = 100,
        @Query("offset") inicio: Int = 0,
    ): JsonObject

    @GET("countries/v5/codes.alpha_2/{code}")
    suspend fun pais(@Header("Authorization") autorizacion: String, @Path("code") codigo: String): JsonObject
}

// Conservamos el nombre del JSON aunque nuestra variable este en espanol.
data class RespuestaGeoNames(
    @SerializedName("geonames") val lugares: List<CiudadGeoNames>?,
    @SerializedName("status") val estadoServicio: EstadoGeoNames?,
)
data class EstadoGeoNames(
    @SerializedName("value") val codigo: Int,
    @SerializedName("message") val mensaje: String?,
)
data class CiudadGeoNames(
    @SerializedName("geonameId") val idGeoNames: Long,
    @SerializedName("name") val nombre: String?,
    @SerializedName("population") val poblacion: Long?,
    @SerializedName("lat") val latitud: String?,
    @SerializedName("lng") val longitud: String?,
    @SerializedName("fcode") val tipoLugar: String?,
)

interface ServicioCiudades {
    @GET("searchJSON")
    suspend fun ciudades(
        @Query("country") pais: String,
        @Query("username") usuarioGeoNames: String,
        @Query("featureClass") claseLugar: String = "P",
        @Query("cities") ciudades: String? = "cities15000",
        @Query("maxRows") maximoFilas: Int = 10,
        @Query("orderby") ordenarPor: String = "population",
        @Query("featureCode") tiposDeLugar: List<String>? = null,
        @Query("style") estilo: String = "FULL",
        @Query("lang") idioma: String = "es",
    ): RespuestaGeoNames
}

interface ServicioClima {
    @GET("current.json")
    suspend fun climaActual(
        @Query("key") clave: String,
        @Query("q") coordenadas: String,
        @Query("lang") idioma: String = "es",
        @Query("aqi") aqi: String = "no",
    ): JsonObject
}
