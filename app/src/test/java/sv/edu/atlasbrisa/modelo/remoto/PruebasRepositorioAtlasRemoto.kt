package sv.edu.atlasbrisa.modelo.remoto

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sv.edu.atlasbrisa.modelo.*

class PruebasRepositorioAtlasRemoto {
    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: RepositorioAtlasRemoto
    private val ciudadDeEjemplo = Ciudad(1, "San Salvador", 100, Coordenadas(13.69, -89.19), true)

    @Before fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        val retrofit = Retrofit.Builder().baseUrl(servidor.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build()
        repositorio = RepositorioAtlasRemoto(retrofit.create(ServicioPaises::class.java),
            retrofit.create(ServicioCiudades::class.java), retrofit.create(ServicioClima::class.java),
            "test-token", "test-user", "test-weather-key")
    }
    @After fun cerrar() { servidor.shutdown() }
    private fun responder(cuerpo: String, codigo: Int = 200) { servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo)) }
    private fun pais(iso: String = "SV") = """{
        "codes":{"alpha_2":"$iso","alpha_3":"SLV"},
        "names":{"common":"El Salvador","official":"Republic of El Salvador","translations":{"spa":{"common":"El Salvador","official":"República de El Salvador"}}},
        "capitals":[{"name":"San Salvador"}],"region":"Americas","subregion":"Central America",
        "population":6000000,"flag":{"url_png":"https://example.org/sv.png","emoji":"SV"},
        "currencies":[{"code":"USD","name":"Dollar","symbol":"$"}],
        "languages":[{"name":"Spanish","native_name":"Español"}],"coordinates":{"lat":13.7,"lng":-89.2}}
    """.trimIndent()
    private fun envolverRespuesta(objetos: String, hayMas: Boolean = false) = """{"data":{"objects":[$objetos],"meta":{"more":$hayMas}}}"""
    private val clima = """{"current":{"temp_c":25.0,"temp_f":77.0,"condition":{"text":"Soleado","icon":"//cdn.weatherapi.com/weather/64x64/day/113.png"},"wind_kph":8.0,"wind_mph":5.0,"humidity":60,"last_updated":"2026-10-22 12:00"}}"""

    @Test fun `se leen todas las paginas y se envia el token Bearer`() = runTest {
        responder(envolverRespuesta(pais("SV") + "," + pais("CR"), true))
        responder(envolverRespuesta(pais("MX")))
        val resultado = repositorio.paises("Americas")
        assertEquals(3, resultado.size)
        val first = servidor.takeRequest()
        val second = servidor.takeRequest()
        assertEquals("Bearer test-token", first.getHeader("Authorization"))
        assertEquals("Americas", first.requestUrl!!.queryParameter("region"))
        assertEquals("100", first.requestUrl!!.queryParameter("limit"))
        assertEquals("2", second.requestUrl!!.queryParameter("offset"))
        assertNull(first.requestUrl!!.queryParameter("api-key"))
    }

    @Test fun `se conservan los datos necesarios del pais`() = runTest {
        responder(envolverRespuesta(pais()))
        val resultado = repositorio.pais("SV")
        assertEquals("República de El Salvador", resultado.nombreOficial)
        assertEquals(listOf("San Salvador"), resultado.capitales)
        assertEquals(6000000L, resultado.poblacion)
        assertEquals(listOf("Español"), resultado.idiomas)
        assertTrue(resultado.monedas.single().contains("USD"))
        assertEquals(Coordenadas(13.7, -89.2), resultado.coordenadas)
        assertEquals("/countries/v5/codes.alpha_2/SV", servidor.takeRequest().path)
    }

    @Test fun `el token publico de muestra no se toma como dato real`() = runTest {
        responder("""{"data":{"objects":[${pais()}],"_demo":{"message":"sample"}}}""")
        val error = runCatching { repositorio.pais("SV") }.exceptionOrNull()
        assertTrue(error is ErrorAtlas)
        assertTrue(error!!.message!!.contains("demostración"))
    }

    @Test fun `si la capital queda fuera de las diez se incluye en lugar de la ultima`() = runTest {
        responder(envolverRespuesta(pais()))
        val elegido = repositorio.pais("SV")
        val ciudadesGrandes = (1..10).joinToString { """{"geonameId":$it,"name":"Ciudad $it","population":${20000 - it},"lat":"13.7","lng":"-89.2","fcode":"PPL"}""" }
        responder("""{"geonames":[$ciudadesGrandes]}""")
        responder("""{"geonames":[{"geonameId":99,"name":"San Salvador","population":5000,"lat":"13.69","lng":"-89.19","fcode":"PPLC"}]}""")
        val resultado = repositorio.ciudades(elegido)
        assertEquals(10, resultado.size)
        assertTrue(resultado.first().esCapital)
        assertEquals(99L, resultado.first().id)
        servidor.takeRequest()
        val consultaCiudades = servidor.takeRequest()
        val consultaCapital = servidor.takeRequest()
        assertEquals("cities15000", consultaCiudades.requestUrl!!.queryParameter("cities"))
        assertEquals("test-user", consultaCiudades.requestUrl!!.queryParameter("username"))
        assertNull(consultaCapital.requestUrl!!.queryParameter("cities"))
        assertTrue(consultaCapital.requestUrl!!.queryParameterValues("featureCode").contains("PPLC"))
    }

    @Test fun `se reconoce el error de GeoNames aunque responda HTTP 200`() = runTest {
        responder(envolverRespuesta(pais()))
        val elegido = repositorio.pais("SV")
        responder("""{"status":{"value":10,"message":"invalid user"}}""")
        val error = runCatching { repositorio.ciudades(elegido) }.exceptionOrNull()
        assertTrue(error is ErrorAtlas)
        assertTrue(error!!.message!!.contains("Free Web Services"))
    }

    @Test fun `un pais sin ciudades devuelve una lista vacia`() = runTest {
        responder(envolverRespuesta(pais()))
        val elegido = repositorio.pais("SV")
        responder("""{"status":{"value":15,"message":"no result"}}""")
        assertTrue(repositorio.ciudades(elegido).isEmpty())
    }

    @Test fun `el clima usa coordenadas espanol y se guarda diez minutos`() = runTest {
        responder(clima)
        val resultado = repositorio.clima(ciudadDeEjemplo)
        assertEquals(25.0, resultado.temperaturaC, 0.001)
        assertEquals(77.0, resultado.temperaturaF, 0.001)
        assertTrue(resultado.urlIcono!!.startsWith("https://"))
        assertEquals(resultado, repositorio.clima(ciudadDeEjemplo))
        assertEquals(1, servidor.requestCount)
        val solicitud = servidor.takeRequest()
        assertEquals("13.69,-89.19", solicitud.requestUrl!!.queryParameter("q"))
        assertEquals("es", solicitud.requestUrl!!.queryParameter("lang"))
        responder(clima)
        repositorio.clima(ciudadDeEjemplo, actualizar = true)
        assertEquals(2, servidor.requestCount)
    }

    @Test fun `el limite de consultas se diferencia de una clave invalida`() = runTest {
        responder("""{"error":{"code":2007,"message":"quota exceeded"}}""", 403)
        val error = runCatching { repositorio.clima(ciudadDeEjemplo) }.exceptionOrNull()
        assertTrue(error is ErrorAtlas)
        assertTrue(error!!.message!!.contains("límite"))
    }

    @Test fun `si no se encuentra una ubicacion se puede reintentar`() = runTest {
        responder("""{"error":{"code":1006,"message":"no location"}}""", 400)
        val error = runCatching { repositorio.clima(ciudadDeEjemplo) }.exceptionOrNull()
        assertTrue(error!!.message!!.contains("ubicación"))
    }
}
