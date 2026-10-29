package sv.edu.atlasbrisa.controlador

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import sv.edu.atlasbrisa.modelo.*

@OptIn(ExperimentalCoroutinesApi::class)
class PruebasControladorAtlas {
    private class RepositorioDePrueba : RepositorioAtlas {
        val paisDeEjemplo = Pais("SV", "SLV", "El Salvador", "República de El Salvador", listOf("San Salvador"),
            "Americas", "Central America", 6000000, null, "", listOf("USD"), listOf("Español"), Coordenadas(13.7, -89.2))
        var fallarPais = false
        var consultasActivas = 0
        var maximoSimultaneo = 0
        var isoSolicitado: String? = null
        override suspend fun regiones() = regionesConocidas
        override suspend fun paises(region: String) = listOf(paisDeEjemplo)
        override suspend fun pais(iso2: String): Pais {
            isoSolicitado = iso2
            if (fallarPais) throw ErrorAtlas("No se pudo consultar el país")
            return paisDeEjemplo.copy(iso2 = iso2)
        }
        override suspend fun ciudades(pais: Pais): List<Ciudad> {
            delay(100)
            return (1L..10L).map { Ciudad(it, "Ciudad $it", 100, Coordenadas(13.7, -89.2), it == 1L) }
        }
        override suspend fun clima(ciudad: Ciudad, actualizar: Boolean): Clima {
            consultasActivas++
            maximoSimultaneo = maxOf(maximoSimultaneo, consultasActivas)
            try {
                delay(100)
                if (ciudad.id == 2L && !actualizar) throw ErrorAtlas("Ubicación no encontrada")
                return Clima(25.0, 77.0, "Soleado", null, 8.0, 5.0, 60, "hoy")
            } finally { consultasActivas-- }
        }
    }

    @Test fun `si falla el clima de una ciudad las otras nueve siguen funcionando`() = runTest {
        val repositorio = RepositorioDePrueba()
        val controlador = ControladorAtlas(repositorio, repositorio, this)
        controlador.abrirPais("SV")
        advanceUntilIdle()
        val estado = controlador.estado.value
        assertEquals(10, estado.ciudades.size)
        assertEquals(9, estado.ciudades.count { it.clima != null })
        assertEquals(1, estado.ciudades.count { it.error != null })
        assertFalse(estado.ciudades.any { it.cargando })
        assertNull(estado.error)
        assertTrue(repositorio.maximoSimultaneo <= 3)
        controlador.reintentarClima(2)
        advanceUntilIdle()
        assertEquals(10, controlador.estado.value.ciudades.count { it.clima != null })
    }

    @Test fun `al volver se cancelan las consultas pendientes del pais`() = runTest {
        val repositorio = RepositorioDePrueba()
        val controlador = ControladorAtlas(repositorio, repositorio, this)
        controlador.abrirRegion(regionesConocidas.first())
        advanceUntilIdle()
        controlador.abrirPais("SV")
        runCurrent()
        controlador.volver()
        advanceUntilIdle()
        assertEquals(Pantalla.PAISES, controlador.estado.value.pantalla)
        assertTrue(controlador.estado.value.ciudades.isEmpty())
        assertNull(controlador.estado.value.pais)
    }

    @Test fun `reintentar vuelve a consultar el mismo pais que fallo`() = runTest {
        val repositorio = RepositorioDePrueba().apply { fallarPais = true }
        val controlador = ControladorAtlas(repositorio, repositorio, this)
        controlador.abrirPais("CR")
        advanceUntilIdle()
        assertNotNull(controlador.estado.value.error)
        repositorio.fallarPais = false
        controlador.reintentar()
        advanceUntilIdle()
        assertEquals("CR", repositorio.isoSolicitado)
        assertEquals("CR", controlador.estado.value.pais!!.iso2)
        assertNull(controlador.estado.value.error)
    }

    @Test fun `cambiar de modo limpia la navegacion anterior`() = runTest {
        val repositorio = RepositorioDePrueba()
        val controlador = ControladorAtlas(repositorio, repositorio, this)
        controlador.abrirPais("SV")
        runCurrent()
        controlador.iniciar(demo = true)
        advanceUntilIdle()
        assertTrue(controlador.estado.value.demo)
        assertEquals(Pantalla.REGIONES, controlador.estado.value.pantalla)
        assertTrue(controlador.estado.value.ciudades.isEmpty())
    }
}
