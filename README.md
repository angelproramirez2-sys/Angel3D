# Angell3d — versión 0.1.0
Creado por Angel, con la ayuda de Claude (Anthropic).

## Cómo convertirlo en APK con GitHub
1. Crea un repositorio nuevo en GitHub.
2. Sube TODO el contenido de esta carpeta (incluida la carpeta oculta `.github`).
   Si tu navegador no deja subir `.github`, en GitHub usa "Add file → Create new file",
   escribe como nombre `.github/workflows/build.yml` y pega el contenido del archivo `build.yml`.
3. Entra a la pestaña **Actions**. La compilación "Compilar APK" empieza sola (tarda unos minutos).
   Si no empieza: Actions → Compilar APK → Run workflow.
4. Al terminar, abre la ejecución y baja el archivo **Angell3d-APK** (sección Artifacts).
5. Descomprímelo, pasa `app-debug.apk` a tu teléfono e instálalo (permite "instalar apps desconocidas").

## Para activar las donaciones
Abre `app/src/main/java/com/angell3d/app/About.kt` y pon tu enlace (Ko-fi, PayPal.me, Mercado Pago…)
en `DONATION_URL`. Nunca pongas el número de tu tarjeta en la app.

## Qué incluye esta versión
- Estudio 3D: esqueleto con huesos, poses y animación por fotogramas clave, cámara de vista, piso.
- Cara: ojos, cejas y boca con poses (feliz, enojado, sorprendido) y parpadeo; puede usar la foto de un personaje.
- Línea de tiempo: pistas de personaje, escenario, cámara, audio, títulos 2D/3D y relaciones; bloques que se arrastran.
- Audio: importar música y efectos de sonido.
- Biblioteca: fichas de personajes (foto y boceto), guiones y notas.
- Mapa de la historia: escenas reordenables y árbol de relaciones con fotos, ligado a las escenas.
- Importar modelos 3D (se guardan; el visor llega en la siguiente versión).
- Guardado automático, créditos y botón de donar.
