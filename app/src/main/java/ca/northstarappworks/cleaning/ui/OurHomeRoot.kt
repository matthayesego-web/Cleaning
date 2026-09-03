package ca.northstarappworks.cleaning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.sync.HouseholdSyncManager
import ca.northstarappworks.cleaning.sync.HouseholdSyncStatus

@Composable
fun OurHomeRoot(homeViewModel: HomeViewModel = viewModel()) {
    val syncState by homeViewModel.syncUiState.collectAsState()
    val currentUser by homeViewModel.currentUser.collectAsState()

    var pairingVisible by rememberSaveable {
        mutableStateOf(!homeViewModel.hadHouseholdAtLaunch)
    }
    var joinCode by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(syncState.status) {
        if (syncState.status == HouseholdSyncStatus.ERROR) pairingVisible = true
    }

    Box(Modifier.fillMaxSize()) {
        OurHomeApp(homeViewModel = homeViewModel)

        if (
            !pairingVisible &&
            syncState.status != HouseholdSyncStatus.PAIRED &&
            syncState.status != HouseholdSyncStatus.CONNECTING
        ) {
            Button(
                onClick = { pairingVisible = true },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                Icon(Icons.Default.Sync, null)
                Spacer(Modifier.width(8.dp))
                Text("Connect phones", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (pairingVisible) {
        PairingDialog(
            status = syncState.status,
            message = syncState.message,
            pairingCode = syncState.pairingCode,
            currentUser = currentUser,
            joinCode = joinCode,
            onJoinCodeChanged = { value ->
                joinCode = value
                    .uppercase()
                    .filter { it.isLetterOrDigit() }
                    .take(HouseholdSyncManager.PAIRING_CODE_LENGTH)
            },
            onCurrentUserChanged = homeViewModel::setCurrentUser,
            onCreate = homeViewModel::createHousehold,
            onJoin = { homeViewModel.joinHousehold(joinCode) },
            onDismiss = { pairingVisible = false }
        )
    }
}

@Composable
private fun PairingDialog(
    status: HouseholdSyncStatus,
    message: String?,
    pairingCode: String?,
    currentUser: Assignee,
    joinCode: String,
    onJoinCodeChanged: (String) -> Unit,
    onCurrentUserChanged: (Assignee) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {
        if (status != HouseholdSyncStatus.CONNECTING) onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(listOf(ForestDeep, Forest)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Home, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Connect Our Home",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink
                        )
                        Text(
                            "One shared list for Matt + Jessie",
                            color = MutedInk
                        )
                    }
                }

                when (status) {
                    HouseholdSyncStatus.PAIRED -> {
                        PairingSuccess(pairingCode, message)
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }

                    HouseholdSyncStatus.CONNECTING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Forest)
                            Text(message ?: "Connecting…", color = MutedInk, textAlign = TextAlign.Center)
                        }
                    }

                    HouseholdSyncStatus.NOT_PAIRED,
                    HouseholdSyncStatus.ERROR -> {
                        if (status == HouseholdSyncStatus.ERROR && !message.isNullOrBlank()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2EE))
                            ) {
                                Text(
                                    message,
                                    modifier = Modifier.padding(12.dp),
                                    color = Color(0xFF9F3D32)
                                )
                            }
                        }

                        Text(
                            "This phone belongs to",
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentUser == Assignee.MATT,
                                onClick = { onCurrentUserChanged(Assignee.MATT) },
                                label = { Text("Matt") }
                            )
                            FilterChip(
                                selected = currentUser == Assignee.JESSIE,
                                onClick = { onCurrentUserChanged(Assignee.JESSIE) },
                                label = { Text("Jessie") }
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Mint)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sync, null, tint = ForestDeep)
                                    Spacer(Modifier.width(8.dp))
                                    Text("First phone", fontWeight = FontWeight.ExtraBold, color = ForestDeep)
                                }
                                Text(
                                    "Tap Create home. You'll get a short code to enter on the other phone.",
                                    color = ForestDeep
                                )
                                Button(
                                    onClick = onCreate,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Create home", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            "Or join the home already created on the other phone",
                            color = MutedInk
                        )
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = onJoinCodeChanged,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Household code") },
                            placeholder = { Text("ABC123") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Button(
                            onClick = onJoin,
                            enabled = joinCode.length == HouseholdSyncManager.PAIRING_CODE_LENGTH,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                        ) {
                            Text("Join home", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Use offline for now", color = MutedInk)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingSuccess(pairingCode: String?, message: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Connected!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ForestDeep)
        Spacer(Modifier.height(5.dp))
        Text(
            message ?: "Your household is ready.",
            color = MutedInk,
            textAlign = TextAlign.Center
        )
        if (!pairingCode.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text("PAIRING CODE", color = MutedInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                pairingCode,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = ForestDeep
            )
            Text(
                "Enter this on the other phone.",
                color = MutedInk,
                textAlign = TextAlign.Center
            )
        }
    }
}
