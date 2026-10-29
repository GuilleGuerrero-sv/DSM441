package sv.edu.atlasbrisa.modelo

import kotlinx.coroutines.delay

// Estos datos de ejemplo sirven para practicar mientras consigues las claves.
class RepositorioAtlasDemo : RepositorioAtlas {
    private val paises = listOf(
        paisDeEjemplo("SV", "SLV", "El Salvador", "República de El Salvador", "San Salvador", "Americas", "América Central", 6029976, "Dólar estadounidense · USD · $", "Español", 13.7942, -88.8965, "222", "ESA"),
        paisDeEjemplo("CR", "CRI", "Costa Rica", "República de Costa Rica", "San José", "Americas", "América Central", 5044197, "Colón costarricense · CRC · ₡", "Español", 9.7489, -83.7534, "188", "CRC"),
        paisDeEjemplo("ES", "ESP", "España", "Reino de España", "Madrid", "Europe", "Europa del Sur", 47351567, "Euro · EUR · €", "Español", 40.4637, -3.7492, "724", "ESP"),
        paisDeEjemplo("JP", "JPN", "Japón", "Japón", "Tokio", "Asia", "Asia Oriental", 125836021, "Yen · JPY · ¥", "Japonés", 36.2048, 138.2529, "392", "JPN"),
        paisDeEjemplo("ZA", "ZAF", "Sudáfrica", "República de Sudáfrica", "Pretoria", "Africa", "África Austral", 59308690, "Rand · ZAR · R", "Zulú / Inglés", -30.5595, 22.9375, "710", "RSA"),
        paisDeEjemplo("AU", "AUS", "Australia", "Mancomunidad de Australia", "Canberra", "Oceania", "Australia y Nueva Zelanda", 25687041, "Dólar australiano · AUD · $", "Inglés", -25.2744, 133.7751, "036", "AUS"),
        Pais("AQ", "ATA", "Antártida", "Antártida", emptyList(), "Antarctic", "Región antártica", 0,
            "https://flagcdn.com/w160/aq.png", "🇦🇶", emptyList(), emptyList(), Coordenadas(-82.0, 0.0), codigoNumerico = "010"),
    )
    private val ciudades = mapOf(
        "SV" to listOf(
            Ciudad(3583361, "San Salvador", 525990, Coordenadas(13.6894, -89.1872), true),
            Ciudad(3583334, "Santa Ana", 176661, Coordenadas(13.9942, -89.5597), false),
            Ciudad(3583446, "San Miguel", 161880, Coordenadas(13.4833, -88.1833), false),
            Ciudad(3583096, "Soyapango", 329708, Coordenadas(13.7102, -89.1399), false),
        ),
        "CR" to listOf(Ciudad(3621849, "San José", 335007, Coordenadas(9.9333, -84.0833), true), Ciudad(3623486, "Alajuela", 47494, Coordenadas(10.0167, -84.2167), false)),
        "ES" to listOf(Ciudad(3117735, "Madrid", 3223334, Coordenadas(40.4165, -3.7026), true), Ciudad(3128760, "Barcelona", 1621537, Coordenadas(41.3888, 2.159), false)),
        "JP" to listOf(Ciudad(1850147, "Tokio", 8336599, Coordenadas(35.6895, 139.6917), true), Ciudad(1853909, "Osaka", 2592413, Coordenadas(34.6937, 135.5022), false)),
        "ZA" to listOf(Ciudad(964137, "Pretoria", 741651, Coordenadas(-25.7449, 28.1878), true), Ciudad(993800, "Johannesburgo", 2026469, Coordenadas(-26.2023, 28.0436), false)),
        "AU" to listOf(Ciudad(2172517, "Canberra", 367752, Coordenadas(-35.2835, 149.1281), true), Ciudad(2147714, "Sídney", 4627345, Coordenadas(-33.8679, 151.2073), false)),
    )

    override suspend fun regiones(): List<Region> {
        delay(250)
        return regionesConocidas.map { region -> region.copy(cantidadPaises = paises.count { it.region == region.codigo }) }
    }

    override suspend fun paises(region: String): List<Pais> {
        delay(250)
        return paises.filter { it.region == region }.sortedBy { it.nombreComun }
    }

    override suspend fun pais(iso2: String): Pais {
        delay(200)
        return paises.first { it.iso2 == iso2 }
    }

    override suspend fun ciudades(pais: Pais): List<Ciudad> {
        delay(350)
        return ciudades[pais.iso2].orEmpty()
    }

    override suspend fun clima(ciudad: Ciudad, actualizar: Boolean): Clima {
        delay(400)
        val temperaturaC = 20.0 + ciudad.id % 10
        return Clima(temperaturaC, temperaturaC * 1.8 + 32, "Parcialmente nublado · ejemplo",
            "https://cdn.weatherapi.com/weather/64x64/day/116.png", 9.4, 5.8, 68, "Dato ilustrativo; sin actualización en vivo")
    }

    private fun paisDeEjemplo(iso2: String, iso3: String, comun: String, oficial: String,
        capital: String, region: String, subregion: String, poblacion: Long, moneda: String,
        idioma: String, latitud: Double, longitud: Double, codigoNumerico: String, codigoCoi: String) = Pais(
        iso2, iso3, comun, oficial, listOf(capital), region, subregion, poblacion,
        "https://flagcdn.com/w160/${iso2.lowercase()}.png", "", listOf(moneda), listOf(idioma), Coordenadas(latitud, longitud),
        codigoNumerico = codigoNumerico, codigoCoi = codigoCoi,
    )
}
