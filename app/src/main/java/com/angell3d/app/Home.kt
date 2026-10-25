package com.angell3d.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.RowScope

@Composable
private fun RowScope.HomeCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(150.dp),
        colors = CardDefaults.cardColors(containerColor = Panel2)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 34.sp)
            Text(title, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun HomeScreen(onNav: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Angell3d", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Cyan)
        Text(
            "Crea tus series animadas desde tu celular",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard("🧍", "Estudio 3D", "Huesos, poses y cara") { onNav("studio") }
            HomeCard("🎞️", "Línea de tiempo", "Cámara, audio y títulos") { onNav("timeline") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard("📚", "Biblioteca", "Personajes, guiones y notas") { onNav("library") }
            HomeCard("🗺️", "Mapa de la historia", "Escenas y relaciones") { onNav("story") }
        }
        OutlinedButton(onClick = { onNav("about") }, modifier = Modifier.fillMaxWidth()) {
            Text("Acerca de · Créditos · Apoyar el proyecto")
        }
    }
}
