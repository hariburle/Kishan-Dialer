package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class RealScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val demoFavorites = listOf(
    FavoriteContact(id = 1, name = "Alex Johnson", phoneNumber = "+1 (555) 234-5678", label = "Mobile", speedDialSlot = 2, sortOrder = 0),
    FavoriteContact(id = 2, name = "Sarah Miller", phoneNumber = "+1 (555) 876-5432", label = "Work", speedDialSlot = 3, sortOrder = 1),
    FavoriteContact(id = 3, name = "Office Desk", phoneNumber = "+1 (555) 432-1098", label = "Office", speedDialSlot = 4, sortOrder = 2),
    FavoriteContact(id = 4, name = "Main Gate Intercom", phoneNumber = "555-0199", label = "Security", speedDialSlot = 5, sortOrder = 3),
    FavoriteContact(id = 5, name = "Emma Watson", phoneNumber = "+1 (555) 789-0123", label = "Personal", speedDialSlot = 6, sortOrder = 4),
    FavoriteContact(id = 6, name = "Dr. Robert Smith", phoneNumber = "+1 (555) 345-6789", label = "Clinic", speedDialSlot = 7, sortOrder = 5)
  )

  private val demoRecentCalls = listOf(
    RecentCall(id = 1, phoneNumber = "+1 (555) 234-5678", callerName = "Alex Johnson", callType = 1, timestamp = System.currentTimeMillis() - 15 * 60 * 1000, durationSeconds = 184),
    RecentCall(id = 2, phoneNumber = "+1 (555) 876-5432", callerName = "Sarah Miller", callType = 2, timestamp = System.currentTimeMillis() - 75 * 60 * 1000, durationSeconds = 42),
    RecentCall(id = 3, phoneNumber = "+1 (555) 999-0011", callerName = null, callType = 3, timestamp = System.currentTimeMillis() - 4 * 3600 * 1000, durationSeconds = 0, isSpam = true),
    RecentCall(id = 4, phoneNumber = "555-0199", callerName = "Main Gate Intercom", callType = 1, timestamp = System.currentTimeMillis() - 8 * 3600 * 1000, durationSeconds = 12, ruleMatched = "Gate Intercom DTMF Unlock")
  )

  private val demoContacts = listOf(
    DeviceContact(
      name = "Alex Johnson",
      phoneNumber = "+1 (555) 234-5678",
      label = "Mobile",
      isStarred = true,
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 234-5678", "Mobile"))
    ),
    DeviceContact(
      name = "David Chen",
      phoneNumber = "+1 (555) 345-6789",
      label = "Mobile",
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 345-6789", "Mobile"))
    ),
    DeviceContact(
      name = "Emma Watson",
      phoneNumber = "+1 (555) 789-0123",
      label = "Personal",
      isStarred = true,
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 789-0123", "Personal"))
    ),
    DeviceContact(
      name = "Emily Davis",
      phoneNumber = "+1 (555) 567-8901",
      label = "Home",
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 567-8901", "Home"))
    ),
    DeviceContact(
      name = "Main Gate Intercom",
      phoneNumber = "555-0199",
      label = "Gate",
      phoneNumbers = listOf(ContactPhoneNumber("555-0199", "Gate"))
    ),
    DeviceContact(
      name = "Office Desk",
      phoneNumber = "+1 (555) 432-1098",
      label = "Office",
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 432-1098", "Office"))
    ),
    DeviceContact(
      name = "Dr. Robert Smith",
      phoneNumber = "+1 (555) 345-6789",
      label = "Clinic",
      isStarred = true,
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 345-6789", "Clinic"))
    ),
    DeviceContact(
      name = "Sarah Miller",
      phoneNumber = "+1 (555) 876-5432",
      label = "Work",
      isStarred = true,
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 876-5432", "Work"))
    )
  )

  private val demoRules = listOf(
    CallerRule(
      id = 1,
      name = "VIP Client Auto-Answer",
      phoneNumberPattern = "+1 (555) 234*",
      autoAnswer = true,
      answerDelaySec = 2,
      isEnabled = true
    ),
    CallerRule(
      id = 2,
      name = "Gate Intercom DTMF Unlock",
      phoneNumberPattern = "555-0199",
      autoAnswer = true,
      answerDelaySec = 1,
      dtmfSequence = "9",
      autoHangup = true,
      hangupDelaySec = 2,
      isEnabled = true
    ),
    CallerRule(
      id = 3,
      name = "Telemarketer Auto-Hangup",
      phoneNumberPattern = "1800*",
      autoAnswer = false,
      autoHangup = true,
      isEnabled = true
    )
  )

  @Composable
  private fun PixelPhoneFrameWrapper(selectedTab: Int, content: @Composable () -> Unit) {
    MyApplicationTheme(darkTheme = true) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF0F172A))
      ) {
        // Pixel Status Bar (Safe Area avoiding camera hole)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 24.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "9:41",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.weight(1f))
          // Center gap for the camera punch hole
          Spacer(modifier = Modifier.width(36.dp))
          Spacer(modifier = Modifier.weight(1f))
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.SignalCellular4Bar, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.BatteryFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
          }
        }

        // Screen Body with Navigation Bar
        Scaffold(
          bottomBar = {
            NavigationBar {
              NavigationBarItem(
                selected = selectedTab == 0,
                onClick = {},
                icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                label = { Text("Favorites") }
              )
              NavigationBarItem(
                selected = selectedTab == 1,
                onClick = {},
                icon = { Icon(Icons.Default.Dialpad, contentDescription = "Keypad") },
                label = { Text("Keypad") }
              )
              NavigationBarItem(
                selected = selectedTab == 2,
                onClick = {},
                icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                label = { Text("Contacts") }
              )
              NavigationBarItem(
                selected = selectedTab == 3,
                onClick = {},
                icon = { Icon(Icons.Default.SmartToy, contentDescription = "Rules") },
                label = { Text("Rules") }
              )
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            content()
          }
        }
      }
    }
  }

  @Test
  fun capture_panel_1_favorites() {
    composeTestRule.setContent {
      PixelPhoneFrameWrapper(selectedTab = 0) {
        FavoritesScreen(
          favorites = demoFavorites,
          recentCalls = demoRecentCalls,
          onSelectNumber = {},
          onCallNumber = {},
          onCallWhatsApp = {},
          onCreateRule = {},
          onDeleteFavorite = {},
          onAddFavorite = { _, _, _, _, _ -> },
          onAssignSpeedDial = { _, _ -> },
          getPreferredCallingMode = { "ask_always" },
          deviceContacts = demoContacts
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/panel_1_favorites.png")
  }

  @Test
  fun capture_panel_2_keypad() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    composeTestRule.setContent {
      PixelPhoneFrameWrapper(selectedTab = 1) {
        DialerScreen(
          number = "+15552345678",
          favorites = demoFavorites,
          recentCalls = demoRecentCalls,
          deviceContacts = demoContacts,
          isDefaultDialer = true,
          context = context,
          onRoleChanged = {},
          onDigitPress = {},
          onDeleteDigit = {},
          onClearDigits = {},
          onSelectContactNumber = {},
          onPlaceCall = { _, _ -> },
          onPlaceWhatsAppCall = {},
          onSimulateCall = { _, _ -> },
          onCreateRuleForNumber = {},
          onAddFavorite = { _, _, _, _ -> },
          onDeleteFavorite = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/panel_2_keypad.png")
  }

  @Test
  fun capture_panel_3_contacts() {
    composeTestRule.setContent {
      PixelPhoneFrameWrapper(selectedTab = 2) {
        ContactsScreen(
          favorites = demoFavorites,
          recentCalls = demoRecentCalls,
          deviceContacts = demoContacts,
          onCallNumber = {},
          onSelectNumber = {},
          onToggleFavorite = { _, _, _, _ -> },
          onCreateRule = {},
          onPlaceWhatsAppCall = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/panel_3_contacts.png")
  }
}
