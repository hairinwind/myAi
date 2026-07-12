package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ChatViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("网页翻译助手", appName)
  }

  @Test
  fun `chatViewModel sendMessage with non-URL input`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ChatViewModel(app)
    
    // Clear any existing messages
    viewModel.clearChat()
    
    // Send "test"
    viewModel.sendMessage("test")
    
    // Wait for coroutines to complete/propagate in database
    val database = com.example.data.AppDatabase.getDatabase(app)
    var dbMessages = emptyList<com.example.data.MessageEntity>()
    
    // Retry/wait a little for the DB insert to complete
    for (i in 1..20) {
      dbMessages = database.messageDao().getAllMessages().first()
      if (dbMessages.isNotEmpty()) break
      kotlinx.coroutines.delay(100)
    }
    
    // Verify messages in DB
    println("DEBUG: DB Messages count = ${dbMessages.size}")
    dbMessages.forEach { msg ->
      println("DEBUG: Msg = $msg")
    }
    
    assertTrue("Should have messages in DB", dbMessages.isNotEmpty())
  }
}
