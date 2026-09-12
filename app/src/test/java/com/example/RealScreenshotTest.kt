package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.telecom.ActiveCallInfo
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.InCallScreen
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

  @Test
  fun capture_dialer_screen() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        DialerScreen(
          number = "5474",
          favorites = listOf(
            FavoriteContact(id = 1, name = "Kishan Burle", phoneNumber = "+15550192834", label = "Mobile", speedDialSlot = 2),
            FavoriteContact(id = 2, name = "Apartment Intercom", phoneNumber = "5550199", label = "Gate", speedDialSlot = 9)
          ),
          recentCalls = listOf(
            RecentCall(id = 1, phoneNumber = "+15550192834", callerName = "Kishan Burle", callType = 1, timestamp = System.currentTimeMillis() - 3600000, durationSeconds = 124)
          ),
          deviceContacts = listOf(
            DeviceContact(
              name = "Kishan Burle",
              phoneNumber = "+15550192834",
              phoneNumbers = listOf(ContactPhoneNumber("+15550192834", "Mobile"))
            ),
            DeviceContact(
              name = "Sarah Miller",
              phoneNumber = "+15554381290",
              phoneNumbers = listOf(ContactPhoneNumber("+15554381290", "Work"))
            )
          ),
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
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/screenshot_dialer.png")
  }

  @Test
  fun capture_incall_screen() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        InCallScreen(
          callInfo = ActiveCallInfo(
            id = "call_123",
            phoneNumber = "+1 (555) 019-2834",
            displayName = "Kishan Burle",
            state = android.telecom.Call.STATE_RINGING,
            connectTimeMillis = 0L,
            isIncoming = true
          ),
          isMuted = false,
          isSpeakerOn = false,
          automationStep = null,
          lastDtmfKey = null,
          showKeypad = false,
          onToggleKeypad = {},
          onAnswer = {},
          onDecline = {},
          onDisconnect = {},
          onToggleMute = {},
          onToggleSpeaker = {},
          onPlayDtmf = {},
          onStopDtmf = {},
          callAnswerStyle = "swipe_slider"
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/screenshot_incall.png")
  }

  @Test
  fun capture_rules_screen() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        RulesScreen(
          rules = listOf(
            CallerRule(
              id = 1,
              name = "VIP Client Auto-Answer",
              phoneNumberPattern = "+1 (555) 019*",
              answerDelaySec = 2,
              dtmfSequence = "",
              isEnabled = true
            ),
            CallerRule(
              id = 2,
              name = "Gate Intercom DTMF Unlock",
              phoneNumberPattern = "5550199",
              answerDelaySec = 1,
              dtmfSequence = "9",
              isEnabled = true
            )
          ),
          automationLogs = emptyList(),
          onToggleRule = {},
          onSaveRule = {},
          onDeleteRule = {},
          onClearLogs = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/screenshot_rules.png")
  }
}
