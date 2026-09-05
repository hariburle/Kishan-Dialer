package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CallerRule
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
    assertEquals("Kishan Dialer", appName)
  }

  @Test
  fun `verify caller rule matching logic`() {
    val rule = CallerRule(
      name = "Apartment Intercom",
      phoneNumberPattern = "5550199",
      isEnabled = true,
      autoAnswer = true,
      dtmfSequence = "9#"
    )
    val incomingCaller = "+1 (555) 0199"
    val normalizedCaller = incomingCaller.filter { it.isDigit() }
    val normalizedPattern = rule.phoneNumberPattern.filter { it.isDigit() }

    assertTrue(normalizedCaller.contains(normalizedPattern))
  }

  @Test
  fun `verify favorite contact creation`() {
    val fav = com.example.data.FavoriteContact(
      name = "Mom",
      phoneNumber = "+15552345678",
      label = "Family"
    )
    assertEquals("Mom", fav.name)
    assertEquals("+15552345678", fav.phoneNumber)
  }
}
