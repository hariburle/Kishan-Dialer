package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.ui.screens.CallLogScreen
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
    FavoriteContact(id = 4, name = "Main Gate Intercom", phoneNumber = "555-0199", label = "Security", speedDialSlot = 9, sortOrder = 3)
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
      name = "Sarah Miller",
      phoneNumber = "+1 (555) 876-5432",
      label = "Work",
      isStarred = true,
      phoneNumbers = listOf(ContactPhoneNumber("+1 (555) 876-5432", "Work"))
    )
  )

  @Composable
  private fun TestAppScaffold(selectedTab: Int, content: @Composable () -> Unit) {
    MyApplicationTheme(darkTheme = true) {
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
              icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
              label = { Text("Recents") }
            )
            NavigationBarItem(
              selected = selectedTab == 2,
              onClick = {},
              icon = { Icon(Icons.Default.Dialpad, contentDescription = "Keypad") },
              label = { Text("Keypad") }
            )
            NavigationBarItem(
              selected = selectedTab == 3,
              onClick = {},
              icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
              label = { Text("Contacts") }
            )
            NavigationBarItem(
              selected = selectedTab == 4,
              onClick = {},
              icon = { Icon(Icons.Default.SmartToy, contentDescription = "Rules") },
              label = { Text("Rules") }
            )
          }
        }
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          content()
        }
      }
    }
  }

  @Test
  fun capture_panel_1_favorites() {
    composeTestRule.setContent {
      TestAppScaffold(selectedTab = 0) {
        FavoritesScreen(
          favorites = demoFavorites,
          recentCalls = demoRecentCalls,
          onSelectNumber = {},
          onCallNumber = {},
          onCallWhatsApp = {},
          onCreateRule = {},
          onDeleteFavorite = {},
          onAddFavorite = { _, _, _, _ -> },
          onAssignSpeedDial = { _, _ -> },
          deviceContacts = demoContacts
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/panel_1_favorites.png")
  }

  @Test
  fun capture_panel_2_recents() {
    composeTestRule.setContent {
      TestAppScaffold(selectedTab = 1) {
        CallLogScreen(
          recentCalls = demoRecentCalls,
          favorites = demoFavorites,
          onCallBack = {},
          onCreateRuleForNumber = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/panel_2_recents.png")
  }

  @Test
  fun capture_panel_3_keypad() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    composeTestRule.setContent {
      TestAppScaffold(selectedTab = 2) {
        DialerScreen(
          number = "2539",
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
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/panel_3_keypad.png")
  }

  @Test
  fun capture_panel_4_contacts() {
    composeTestRule.setContent {
      TestAppScaffold(selectedTab = 3) {
        ContactsScreen(
          favorites = demoFavorites,
          recentCalls = demoRecentCalls,
          deviceContacts = demoContacts,
          onCallNumber = {},
          onSelectNumber = {},
          onToggleFavorite = { _, _, _, _ -> },
          onCreateRule = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/panel_4_contacts.png")
  }
}
