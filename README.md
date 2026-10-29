# Atlas Brisa

Aplicación Android en Kotlin para el Desafío Práctico #03 de Desarrollo de Software para Móviles. Permite explorar regiones, países, hasta diez ciudades por país, su ubicación y su clima actual.

Código del desafío en la rama [`desafio-3`](https://github.com/GuilleGuerrero-sv/DSM441/tree/desafio-3) del repositorio DSM441. La carpeta `SEMANA2-HG` se conserva del trabajo anterior; la aplicación Android de este desafío está en la raíz y su módulo es `app`.

## Abrir y ejecutar en Android Studio

1. Selecciona **Open** y abre la carpeta que contiene `settings.gradle.kts` (localmente `AtlasBrisa`; si clonas el repositorio puede llamarse `DSM441`). No crees otro proyecto ni pegues todo en `ActividadPrincipal.kt`.
2. Espera la sincronización de Gradle. Si pide un JDK, selecciona el **Embedded JDK** de Android Studio. El proyecto usa Gradle 9.4.1, Android Gradle Plugin 9.2.1, Kotlin 2.4.10 y SDK de compilación 37, compatibles con el Android Studio instalado en esta computadora. El teléfono puede usar Android 8.0 o superior (API 26).
3. Copia `secrets.properties.example` como `secrets.properties` si todavía no existe. Completa tus credenciales de REST Countries, GeoNames y WeatherAPI; para GeoNames debes habilitar **Free Web Services** en tu cuenta.
4. Selecciona un emulador o conecta tu teléfono con depuración USB y pulsa **Run**.
5. Mientras consigues las claves, pulsa **Explorar demostración**. La franja amarilla identifica los datos ilustrativos. El fondo del mapa y las imágenes necesitan Internet.

Al cambiar una credencial debes volver a compilar y ejecutar la aplicación. Las claves se leen al compilar, no al iniciar la app.

**Compatibilidad con Android Studio:** AGP y Gradle tienen versiones distintas en este proyecto: **AGP 9.2.1** y **Gradle 9.4.1**, siguiendo [la tabla oficial de compatibilidad](https://developer.android.com/build/releases/agp-9-2-0-release-notes). Después de actualizar estos archivos, selecciona **File > Sync Project with Gradle Files** o **Sync Now** y espera a que termine antes de pulsar Run.

## APK

El APK de demostración está disponible localmente en `entregables/atlas-brisa-debug.apk`. Los APK quedan excluidos de Git; no se descargan al clonar el repositorio.

La compilación de demostración no incluye credenciales personales. Para usar los servicios reales, cada persona debe completar su archivo local y generar su propio APK. El APK configurado contiene esos valores: consérvalo localmente y no lo subas al repositorio ni a una Release pública. Si necesitas un enlace público al APK, publica únicamente una compilación de demostración sin claves y agrega aquí ese enlace.

Para generar un APK desde la terminal de Android Studio:

```powershell
.\gradlew.bat :app:assembleDebug
```

Resultado: `app/build/outputs/apk/debug/app-debug.apk`. La versión de depuración está firmada para instalarse; no necesita una cuenta de Google Play.

## Qué implementa

- Regiones y países paginados de REST Countries **v5**, con `Authorization: Bearer`.
- Nombre común y oficial, capitales, región, subregión, población, bandera, códigos ISO alfa-2/alfa-3/numérico y COI, monedas, idiomas en español y coordenadas del país.
- Tarjeta de capital con fondo grana suave, borde y distintivo con estrella; conserva su identidad al seleccionar ciudades.
- Ciudades de GeoNames ordenadas por población. Si la capital no aparece entre las primeras diez, se consulta por separado y se incluye sin superar diez ciudades.
- Clima independiente por ciudad usando sus coordenadas: °C/°F, condición, ícono, viento en km/h o mph, humedad y última actualización.
- Mapa integrado con osmdroid y OpenStreetMap. Marcadores con nombre y clima; selección desde la lista y botón para volver a encuadrar todas las ciudades.
- Estados de carga, reintento, credenciales inválidas o ausentes, límite de consultas, ubicación no encontrada, conexión fallida, tiempo agotado y países sin ciudades.
- Hasta tres consultas meteorológicas simultáneas y caché meteorológica en memoria de diez minutos para reducir solicitudes. Un fallo de clima no bloquea las demás ciudades.
- Nombre e ícono propios. Interfaz en español, búsqueda de país o capital sin depender de tildes, y restauración de país/región al recrearse la actividad.

## Arquitectura MVC

El código propio usa nombres en español y comentarios sencillos. La paleta azul, grana y amarilla está inspirada en el FC Barcelona.

```text
app/src/main/java/sv/edu/atlasbrisa/
├── ActividadPrincipal.kt                 Entrada y ciclo de vida
├── modelo/                         Modelo: datos y repositorios
│   ├── Modelos.kt                   País, ciudad, clima y contrato
│   ├── RepositorioAtlasDemo.kt       Datos ilustrativos explícitos
│   └── remoto/                     Retrofit, Gson, mapeo y errores
├── controlador/ControladorAtlas.kt   Eventos, navegación y consultas
└── vista/                          Vista: funciones de Compose y mapa
```

La vista recibe un estado y comunica acciones al controlador. El controlador invoca los repositorios y actualiza el estado. El modelo procesa datos y errores de los servicios; no dibuja pantallas. Se usa `StateFlow` como transporte de estado, sin una capa de ViewModel.

## Credenciales y repositorio

`secrets.properties`, sus copias, `local.properties`, archivos `.env`, APK y paquetes ZIP están excluidos de Git. `secrets.properties.example` sí se incluye y contiene únicamente los nombres de las variables, sin valores. No copies claves al README ni a los archivos Kotlin. También pueden suministrarse como variables de entorno: `REST_COUNTRIES_TOKEN`, `GEONAMES_USERNAME`, `WEATHER_API_KEY`.

Sube la carpeta **AtlasBrisa**, donde está este README, no su carpeta contenedora. Después de clonar el proyecto, copia `secrets.properties.example` a `secrets.properties`, completa tus propias credenciales y ejecuta la app.

Las claves incluidas mediante `BuildConfig` pueden extraerse de un APK. Esta separación evita publicarlas en el código fuente y cumple el mecanismo solicitado para el ejercicio; no equivale a una bóveda de secretos. Usa claves del ejercicio y revócalas si las expones. Para un producto público, la gestión de secretos requeriría un servicio intermediario.

Para compartir este desafío utiliza el enlace de la rama `desafio-3`. Cada persona configura sus propias claves después de clonar el proyecto. Las claves y las carpetas de compilación quedan fuera de Git.

## Pruebas y capturas

Capturas de la demostración: [regiones](entregables/capturas/regiones.png), [países](entregables/capturas/paises.png), [mapa y clima](entregables/capturas/mapa-clima.png).

Comprobación con las cuentas configuradas: [mapa y clima reales](entregables/capturas/ciudad-clima-real.png).

Diseño actualizado con colores blaugrana: [inicio](entregables/capturas/inicio-blaugrana.png) y [clima y mapa](entregables/capturas/clima-blaugrana.png).

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Las pruebas automatizadas usan respuestas simuladas para comprobar contratos y fallos sin consumir tu cuota. Se completó además el recorrido real en el emulador con las tres cuentas. Falta instalar y verificar la aplicación en el teléfono físico que llevarás a la presentación.

## Servicios y atribuciones

- [REST Countries v5](https://restcountries.com/docs/countries)
- [GeoNames Search](https://www.geonames.org/export/geonames-search.html), datos bajo [CC BY](https://www.geonames.org/export/)
- [WeatherAPI.com](https://www.weatherapi.com/docs/)
- [osmdroid 6.1.20](https://github.com/osmdroid/osmdroid) y [OpenStreetMap contributors](https://www.openstreetmap.org/copyright)
- [Política de uso de mapas de OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/): consultas del área visible, atribución, identificación propia y caché; no se ofrecen descargas masivas.

osmdroid es una de las opciones admitidas expresamente por el enunciado. Su repositorio está archivado; para este ejercicio se fija la versión y se documenta su uso. Los mapas no requieren una cuarta API key.
