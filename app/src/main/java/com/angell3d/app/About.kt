package com.angell3d.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Config {
    /**
     * Pon aquí el enlace de tu página de donaciones (Ko-fi, PayPal.me, Mercado Pago, etc.).
     * Ejemplo: "https://ko-fi.com/tunombre"
     * Mientras esté vacío, el botón de donar aparece desactivado.
     * NUNCA pongas aquí el número de tu tarjeta.
     */
    const val DONATION_URL = ""
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Header("Acerca de", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Angell3d", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Cyan)
            Text("Versión 0.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Créditos", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Creado por Angel")
            Text("Con la ayuda de Claude (Anthropic)")

            Text("Apoya el proyecto", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Las donaciones son voluntarias y no desbloquean funciones: " +
                    "todo Angell3d es para todos. Tu apoyo ayuda a seguir mejorando la app.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.DONATION_URL)))
                },
                enabled = Config.DONATION_URL.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (Config.DONATION_URL.isNotBlank()) "💙 Donar" else "Donaciones próximamente")
            }

            Text("Aviso sobre derechos", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Cada usuario es responsable de los modelos, imágenes, música y sonidos que importa. " +
                    "Usa solo material propio o con licencia para publicarlo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
