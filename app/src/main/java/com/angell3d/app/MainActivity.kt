package com.angell3d.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.load(applicationContext)
        setContent {
            AngellTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val ctx = LocalContext.current

                    // Guardado automático (a los 0.4 s de dejar de editar)
                    LaunchedEffect(Unit) {
                        snapshotFlow { Store.toJson() }.collectLatest { json ->
                            delay(400)
                            withContext(Dispatchers.IO) { Store.write(ctx, json) }
                        }
                    }

                    var screen by remember { mutableStateOf("home") }
                    BackHandler(enabled = screen != "home") { screen = "home" }
                    val back = { screen = "home" }

                    when (screen) {
                        "studio" -> StudioScreen(back)
                        "timeline" -> TimelineScreen(back)
                        "library" -> LibraryScreen(back)
                        "story" -> StoryScreen(back)
                        "about" -> AboutScreen(back)
                        else -> HomeScreen { screen = it }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Store.write(applicationContext, Store.toJson())
    }
}
