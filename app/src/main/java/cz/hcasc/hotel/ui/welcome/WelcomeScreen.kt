package cz.hcasc.hotel.ui.welcome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cz.hcasc.hotel.R
import kotlin.math.abs

/**
 * Welcome gate screen (C.6):
 *  a) "Připojte OnLine" (no internet AND not activated) -> cannot continue
 *  b) "Aktivujte" (online but not activated) -> periodic activation checks with backoff
 *  c) "Vítejte doma" (activated) -> double-tap to enter app
 *
 * This UI is intentionally minimal and resilient.
 * Business logic (network/status polling) should live in a ViewModel;
 * this file focuses on rendering and the double-tap gate.
 */

enum class WelcomeState {
    REQUIRE_ONLINE,
    ACTIVATE,
    WELCOME
}

@Composable
fun WelcomeScreen(
    state: WelcomeState,
    statusText: String,
    deviceId: String,
    versionName: String,
    versionCode: Int,
    modifier: Modifier = Modifier,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onSubmitDisplayName: (String) -> Unit,
    onCheckStatus: () -> Unit,
    submitting: Boolean,
    checking: Boolean,
    onEnterApp: () -> Unit,
) {
    val bgTop = Color(0xFF0B111A)
    val bgBottom = Color(0xFF05070B)

    // Double-tap detector with basic debouncing and visual hint when in WELCOME.
    var lastTapAt by rememberSaveable { mutableLongStateOf(0L) }
    var tapPulse by rememberSaveable { mutableIntStateOf(0) }

    val pulseAlpha by animateFloatAsState(
        targetValue = if (tapPulse % 2 == 1) 0.75f else 1.0f,
        label = "tapPulseAlpha"
    )

    val canEnter = state == WelcomeState.WELCOME

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom)
                )
            )
            .pointerInput(canEnter) {
                detectTapGestures(
                    onDoubleTap = {
                        if (canEnter) onEnterApp()
                    },
                    onTap = {
                        if (!canEnter) return@detectTapGestures
                        val now = System.currentTimeMillis()
                        // If user taps repeatedly without matching the platform's double-tap,
                        // we provide a subtle pulse (no navigation).
                        if (abs(now - lastTapAt) < 600) {
                            tapPulse += 1
                        }
                        lastTapAt = now
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoBlock(
                modifier = Modifier
                    .alpha(pulseAlpha)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tady jste doma",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFE6EBF2),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            StatusCard(
                state = state,
                statusText = statusText,
                canEnter = canEnter,
                displayName = displayName,
                onDisplayNameChange = onDisplayNameChange,
                onSubmitDisplayName = onSubmitDisplayName,
                onCheckStatus = onCheckStatus,
                submitting = submitting,
                checking = checking
            )

            Spacer(modifier = Modifier.height(18.dp))

            FooterMeta(
                deviceId = deviceId,
                versionName = versionName,
                versionCode = versionCode
            )
        }
    }
}

@Composable
private fun LogoBlock(modifier: Modifier = Modifier) {
    // Note: resource name in structure is drawable/asc_logo.png
    // The actual resource ID will be R.drawable.asc_logo after Android resource merge.
    Surface(
        modifier = modifier,
        color = Color(0xFF0E1622),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(PaddingValues(horizontal = 18.dp, vertical = 16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.asc_logo),
                contentDescription = "ASC Hotel Chodov",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.size(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ASC Hotel Chodov",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFF2F5FA),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Interní hlášení • Pokojská / Recepce / Údržba",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAB4C3)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: WelcomeState,
    statusText: String,
    canEnter: Boolean,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onSubmitDisplayName: (String) -> Unit,
    onCheckStatus: () -> Unit,
    submitting: Boolean,
    checking: Boolean,
) {
    val border = Color(0xFF1D2A3A)
    val surface = Color(0xFF0B121B)

    val accent = when (state) {
        WelcomeState.REQUIRE_ONLINE -> Color(0xFFF2C14E) // yellow
        WelcomeState.ACTIVATE -> Color(0xFF33D6C5) // cyan
        WelcomeState.WELCOME -> Color(0xFF67E8F9) // brighter cyan
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = when (state) {
                        WelcomeState.REQUIRE_ONLINE -> "Připojte OnLine"
                        WelcomeState.ACTIVATE -> "Aktivujte"
                        WelcomeState.WELCOME -> "Vítejte doma"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE6EBF2)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB7C0CF)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Jméno pro administrátora",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD6DEE9),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                placeholder = { Text("Jméno a příjmení", color = Color(0xFF8FA1B6)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE6EBF2)),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Po odeslání se jméno objeví u čekajícího zařízení.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9AABC0)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onSubmitDisplayName(displayName.trim()) },
                        enabled = displayName.isNotBlank() && !submitting
                    ) {
                        Text(if (submitting) "Odesílám…" else "Odeslat jméno")
                    }
                    OutlinedButton(
                        onClick = onCheckStatus,
                        enabled = !checking
                    ) {
                        Text(if (checking) "Kontroluji…" else "Zkontrolovat stav")
                    }
                }
            }

            if (canEnter) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Dvakrát klepněte pro vstup",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD6DEE9),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtle divider-like bottom strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(border)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when (state) {
                    WelcomeState.REQUIRE_ONLINE -> "Bez připojení nelze zařízení aktivovat."
                    WelcomeState.ACTIVATE -> "Kontrola aktivace probíhá automaticky."
                    WelcomeState.WELCOME -> "Zařízení je aktivní."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFAAB4C3)
            )
        }
    }
}

@Composable
private fun FooterMeta(
    deviceId: String,
    versionName: String,
    versionCode: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Device ID: $deviceId",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7E8AA0),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verze: $versionName ($versionCode)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7E8AA0),
                textAlign = TextAlign.Center
            )
        }
    }
}
