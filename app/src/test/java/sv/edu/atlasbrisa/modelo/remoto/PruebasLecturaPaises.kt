package sv.edu.atlasbrisa.modelo.remoto

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class PruebasLecturaPaises {
    private fun json(texto: String) = JsonParser.parseString(texto).asJsonObject

    @Test fun `argentina conserva el cero y muestra guarani y espanol`() {
        // Estos campos tienen la misma forma que la respuesta real de Argentina.
        val pais = leerPais(json("""{
            "codes":{"alpha_2":"AR","alpha_3":"ARG","ccn3":"032","cioc":"ARG"},
            "languages":[
                {"bcp47":"gn","iso639_1":"gn","iso639_3":"grn","name":"Guarani","native_name":"Avañe'ẽ"},
                {"bcp47":"es","iso639_1":"es","iso639_3":"spa","name":"Spanish","native_name":"español"}
            ]
        }"""))
        assertEquals("AR", pais.iso2)
        assertEquals("ARG", pais.iso3)
        assertEquals("032", pais.codigoNumerico)
        assertEquals("ARG", pais.codigoCoi)
        assertEquals(listOf("Guaraní", "Español"), pais.idiomas)
    }

    @Test fun `el codigo coi se lee de su campo y no se copia del alfa tres`() {
        val pais = leerPais(json("""{
            "codes":{"alpha_2":"SV","alpha_3":"SLV","ccn3":"222","cioc":"ESA"}
        }"""))
        assertEquals("SLV", pais.iso3)
        assertEquals("ESA", pais.codigoCoi)
    }

    @Test fun `un codigo numerico corto se completa y los codigos ausentes quedan nulos`() {
        assertEquals("032", leerPais(json("""{"codes":{"ccn3":32}}""")).codigoNumerico)
        assertNull(leerPais(json("""{"codes":{"ccn3":"","cioc":" "}}""")).codigoNumerico)
        assertNull(leerPais(json("""{"codes":{"ccn3":"incorrecto"}}""")).codigoNumerico)
        val sinCodigos = leerPais(json("{}"))
        assertNull(sinCodigos.codigoNumerico)
        assertNull(sinCodigos.codigoCoi)
    }

    @Test fun `se traducen otros idiomas y se conserva un nombre si no hay traduccion`() {
        val pais = leerPais(json("""{
            "languages":[
                {"bcp47":"ja","name":"Japanese","native_name":"日本語"},
                {"iso639_1":"ar","name":"Arabic"},
                {"bcp47":"zz","iso639_1":"es","name":"Spanish"},
                {"name":"Spanish","native_name":"español"},
                {"bcp47":"zz","name":"Idioma local"},
                {"native_name":"Nombre nativo"},
                {"name":"","native_name":" "}
            ]
        }"""))
        assertEquals(listOf("Japonés", "Árabe", "Español", "Idioma local", "Nombre nativo"), pais.idiomas)
    }
}
