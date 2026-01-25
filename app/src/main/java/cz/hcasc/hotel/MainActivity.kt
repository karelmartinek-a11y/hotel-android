package cz.hcasc.hotel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import cz.hcasc.hotel.net.ApiClient
import cz.hcasc.hotel.repo.DeviceRepo
import cz.hcasc.hotel.ui.frontdesk.FrontdeskScreen
import cz.hcasc.hotel.ui.housekeeping.HousekeepingScreen
import cz.hcasc.hotel.ui.maintenance.MaintenanceScreen
import cz.hcasc.hotel.ui.nav.NavScaffold
import cz.hcasc.hotel.ui.welcome.WelcomeScreen
import cz.hcasc.hotel.ui.welcome.WelcomeState
import cz.hcasc.hotel.BuildConfig

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Ensure singleton repos are initialized early (simple manual DI)
        val app = application as cz.hcasc.hotel.App
        val deviceRepo = app.deviceRepo

        setContent {
            MainRoot(deviceRepo = deviceRepo)
        }
    }
}

@Composable
private fun MainRoot(deviceRepo: DeviceRepo) {
    val deviceState by deviceRepo.deviceState.collectAsState()
    var displayName by rememberSaveable { mutableStateOf(deviceRepo.getDisplayName()) }
    val scope = rememberCoroutineScope()
    var submitting by rememberSaveable { mutableStateOf(false) }
    var checking by rememberSaveable { mutableStateOf(false) }

    // Keep device state fresh when app is opened.
    LaunchedEffect(Unit) {
        deviceRepo.refreshStateBestEffort()
    }

    if (!deviceState.isActivated) {
        val welcomeState = if (deviceState.isActivated) WelcomeState.WELCOME else WelcomeState.ACTIVATE
        WelcomeScreen(
            state = welcomeState,
            statusText = deviceState.statusText,
            deviceId = deviceState.deviceId,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            displayName = displayName,
            onDisplayNameChange = { displayName = it },
            onSubmitDisplayName = { name ->
                displayName = name
                submitting = true
                ApiClient.appScope.launch {
                    deviceRepo.saveDisplayName(name)
                    deviceRepo.refreshStateBestEffort()
                    submitting = false
                }
            },
            onCheckStatus = {
                checking = true
                ApiClient.appScope.launch {
                    deviceRepo.refreshStateBestEffort()
                    checking = false
                }
            },
            submitting = submitting,
            checking = checking,
            onEnterApp = {
                scope.launch { deviceRepo.refreshStateBestEffort() }
            }
        )
        return
    }

    NavScaffold.AppNavScaffold(
        startRoute = NavScaffold.ROUTE_HOUSEKEEPING,
        housekeeping = { HousekeepingScreen() },
        frontdesk = { FrontdeskScreen() },
        maintenance = { MaintenanceScreen() }
    )
}
