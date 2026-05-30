package s4y.yopt

import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import yopt.composeapp.generated.resources.Res
import yopt.composeapp.generated.resources.icon
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.util.prefs.Preferences
fun main() {
    application {
        val icon = painterResource(Res.drawable.icon)
        val prefs = Preferences.userRoot().node("s4y/yopt/window")
        val savedX = prefs.getFloat("x", Float.NaN)
        val savedY = prefs.getFloat("y", Float.NaN)
        val windowState = rememberWindowState(
            placement = when (prefs.get("placement", "Floating")) {
                "Maximized" -> WindowPlacement.Maximized
                "Fullscreen" -> WindowPlacement.Fullscreen
                else -> WindowPlacement.Floating
            },
            width = prefs.getFloat("width", 900f).dp,
            height = prefs.getFloat("height", 700f).dp,
            position = if (!savedX.isNaN() && !savedY.isNaN()) WindowPosition(savedX.dp, savedY.dp)
                       else WindowPosition.PlatformDefault
        )
        Window(
            onCloseRequest = {
                prefs.putFloat("width", windowState.size.width.value)
                prefs.putFloat("height", windowState.size.height.value)
                if (windowState.position.isSpecified) {
                    prefs.putFloat("x", windowState.position.x.value)
                    prefs.putFloat("y", windowState.position.y.value)
                }
                prefs.put("placement", windowState.placement.name)
                exitApplication()
            },
            state = windowState,
            title = "YoPt",
            icon = icon
        ) {
            App()
        }
    }
}

