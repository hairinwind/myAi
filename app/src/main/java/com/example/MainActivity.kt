package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private var chatViewModel: ChatViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val chatViewModelInstance: ChatViewModel = viewModel()
          chatViewModel = chatViewModelInstance
          ChatScreen(viewModel = chatViewModelInstance)
        }
      }
    }
    handleSharedText(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleSharedText(intent)
  }

  private fun handleSharedText(intent: Intent?) {
    if (intent != null && intent.action == Intent.ACTION_SEND && "text/plain" == intent.type) {
      val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
      if (!sharedText.isNullOrBlank()) {
        lifecycleScope.launch {
          // Wait for ViewModel instance to be set up in the Compose layout
          while (chatViewModel == null) {
            delay(50)
          }
          chatViewModel?.sendMessage(sharedText)
        }
      }
      // Consume intent so configuration changes (e.g., rotation) do not re-send
      intent.action = null
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
