package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.components.WmsTopAppBar
import com.example.ui.theme.MyApplicationTheme
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
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleUser = UserEntity(
      userId = "USR-01",
      name = "Pak Budi Hartono",
      email = "owner@miningcorp.co.id",
      role = UserRole.OWNER,
      warehouseId = "HQ-01",
      warehouseName = "Kantor Pusat / Direksi",
      token = "sample_token"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        WmsTopAppBar(
          currentUser = sampleUser,
          unreadCount = 3,
          isSocketConnected = true,
          isSandboxMode = false,
          onRoleClick = {},
          onNotificationClick = {},
          onSandboxToggle = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
