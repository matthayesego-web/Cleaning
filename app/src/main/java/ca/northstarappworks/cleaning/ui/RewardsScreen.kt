package ca.northstarappworks.cleaning.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.RewardCoupon
import ca.northstarappworks.cleaning.model.RewardCouponStatus
import ca.northstarappworks.cleaning.model.RewardDefinition
import ca.northstarappworks.cleaning.model.starterRewards

@Composable
fun RewardsScreen(
    completions: List<CompletionRecord>,
    customRewards: List<RewardDefinition>,
    coupons: List<RewardCoupon>,
    currentUser: Assignee,
    onAddReward: (String, String, Int, Assignee) -> Unit,
    onDeleteReward: (RewardDefinition) -> Unit,
    onRedeem: (RewardDefinition) -> Unit,
    onRequestUse: (RewardCoupon) -> Unit,
    onApprove: (RewardCoupon) -> Unit,
    onDecline: (RewardCoupon) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewedOwner by remember(currentUser) { mutableStateOf(currentUser) }
    var showAddReward by remember { mutableStateOf(false) }

    val currentBalance = rewardPointsBalance(completions, coupons, currentUser)
    val pendingForMe = coupons.filter {
        it.status == RewardCouponStatus.PENDING && it.owner != currentUser
    }.sortedBy { it.requestedAt }
    val myWallet = coupons.filter {
        it.owner == currentUser && it.status != RewardCouponStatus.USED
    }.sortedByDescending { it.redeemedAt }
    val viewedCatalog = (
        starterRewards(viewedOwner) + customRewards.filter { it.owner == viewedOwner }
    ).sortedWith(compareBy<RewardDefinition> { it.cost }.thenBy { it.title })

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Rewards", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("Turn little wins into something fun.", color = MutedInk)
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MintSoft,
                        border = BorderStroke(1.dp, Hairline)
                    ) {
                        Icon(Icons.Default.Redeem, null, tint = ForestDeep, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .shadow(18.dp, RoundedCornerShape(29.dp), ambientColor = Forest.copy(alpha = .13f)),
                shape = RoundedCornerShape(29.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    Modifier.background(Brush.linearGradient(listOf(ForestDeep, Forest))).padding(21.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .12f)) {
                            Icon(Icons.Default.Stars, null, tint = Champagne, modifier = Modifier.padding(10.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${currentUser.label}’s balance", color = Color.White.copy(alpha = .68f), fontSize = 12.sp)
                            Text("$currentBalance points", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("10 points for each completed task", color = Color.White.copy(alpha = .62f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (pendingForMe.isNotEmpty()) {
            item {
                Text(
                    "Waiting for you",
                    Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink
                )
            }
            items(pendingForMe, key = { "approval-${it.id}" }) { coupon ->
                RewardApprovalCard(
                    coupon = coupon,
                    onApprove = { onApprove(coupon) },
                    onDecline = { onDecline(coupon) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My wallet", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("${myWallet.size} saved", color = MutedInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (myWallet.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Hairline),
                    colors = CardDefaults.cardColors(containerColor = Paper)
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MintSoft) {
                            Icon(Icons.Default.ConfirmationNumber, null, tint = Forest, modifier = Modifier.padding(10.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("No coupons saved yet", fontWeight = FontWeight.ExtraBold, color = Ink)
                            Text("Redeem something below and it will wait here until you use it.", color = MutedInk, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            items(myWallet, key = { "wallet-${it.id}" }) { coupon ->
                RewardWalletCard(
                    coupon = coupon,
                    onUse = { onRequestUse(coupon) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Catalogues", style = MaterialTheme.typography.titleLarge, color = Ink)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showAddReward = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Custom")
                    }
                }
                Spacer(Modifier.height(4.dp))
                OwnerChoice(viewedOwner) { viewedOwner = it }
            }
        }

        items(viewedCatalog, key = { "reward-${it.id}" }) { reward ->
            val balance = rewardPointsBalance(completions, coupons, reward.owner)
            RewardCatalogCard(
                reward = reward,
                isMine = reward.owner == currentUser,
                canAfford = balance >= reward.cost,
                onRedeem = { onRedeem(reward) },
                onDelete = if (reward.custom) ({ onDeleteReward(reward) }) else null,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Text(
                "Redeeming spends the points immediately. Using the coupon asks the other phone for confirmation.",
                Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                color = MutedInk,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showAddReward) {
        AddRewardDialog(
            initialOwner = viewedOwner,
            onDismiss = { showAddReward = false },
            onSave = { title, description, cost, owner ->
                onAddReward(title, description, cost, owner)
                viewedOwner = owner
                showAddReward = false
            }
        )
    }
}

@Composable
private fun RewardApprovalCard(
    coupon: RewardCoupon,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, Champagne.copy(alpha = .7f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAF2))
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFFFEED5)) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF8A5E2A), modifier = Modifier.padding(9.dp).size(19.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("${coupon.owner.label} wants to use", color = MutedInk, fontSize = 12.sp)
                    Text(coupon.title, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 17.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Text("Not now")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestDeep),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
private fun RewardWalletCard(coupon: RewardCoupon, onUse: () -> Unit, modifier: Modifier = Modifier) {
    val waiting = coupon.status == RewardCouponStatus.PENDING
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (waiting) Champagne.copy(alpha = .7f) else Hairline),
        colors = CardDefaults.cardColors(containerColor = Paper)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(13.dp), color = if (waiting) Color(0xFFFFF0DD) else MintSoft) {
                Icon(
                    if (waiting) Icons.Default.HourglassTop else Icons.Default.ConfirmationNumber,
                    null,
                    tint = if (waiting) Color(0xFF97612B) else Forest,
                    modifier = Modifier.padding(9.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(coupon.title, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(if (waiting) "Waiting for confirmation" else "Ready whenever you are", color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
            if (waiting) {
                RewardStatusPill("Pending", Color(0xFF7E542B), Color(0xFFFFF0DD))
            } else {
                Button(
                    onClick = onUse,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestDeep),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)
                ) { Text("Use", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun RewardCatalogCard(
    reward: RewardDefinition,
    isMine: Boolean,
    canAfford: Boolean,
    onRedeem: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Hairline),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(13.dp), color = MintSoft) {
                Icon(Icons.Default.CardGiftcard, null, tint = ForestDeep, modifier = Modifier.padding(9.dp).size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reward.title, fontWeight = FontWeight.ExtraBold, color = Ink)
                if (reward.description.isNotBlank()) {
                    Text(reward.description, color = MutedInk, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(5.dp))
                Text("${reward.cost} points", color = ForestDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Delete custom reward", tint = Color(0xFF9AA5A1))
                }
            }
            Button(
                onClick = onRedeem,
                enabled = isMine && canAfford,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestDeep),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    when {
                        !isMine -> reward.owner.label
                        canAfford -> "Redeem"
                        else -> "Save up"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AddRewardDialog(
    initialOwner: Assignee,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Assignee) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("50") }
    var owner by remember(initialOwner) { mutableStateOf(initialOwner) }
    val cost = costText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = Paper,
        title = {
            Column {
                Text("Add a reward", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Ink)
                Text("Make the catalogue yours.", color = MutedInk)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reward") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { value -> costText = value.filter(Char::isDigit).take(4) },
                    label = { Text("Points") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Catalogue", fontWeight = FontWeight.Bold, color = Ink)
                OwnerChoice(owner) { owner = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, cost, owner) },
                enabled = title.isNotBlank() && cost in 1..5000,
                colors = ButtonDefaults.buttonColors(containerColor = ForestDeep),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Add reward", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedInk) } }
    )
}

@Composable
private fun OwnerChoice(selected: Assignee, onSelected: (Assignee) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Assignee.MATT, Assignee.JESSIE).forEach { owner ->
            val active = owner == selected
            FilterChip(
                selected = active,
                onClick = { onSelected(owner) },
                label = { Text(owner.label, fontWeight = FontWeight.SemiBold) },
                leadingIcon = if (active) ({ Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }) else null
            )
        }
    }
}

@Composable
private fun RewardStatusPill(text: String, content: Color, background: Color) {
    Surface(shape = CircleShape, color = background) {
        Text(
            text,
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = content,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

private fun rewardPointsBalance(
    completions: List<CompletionRecord>,
    coupons: List<RewardCoupon>,
    owner: Assignee
): Int {
    val earned = completions.count { it.completedBy == owner } * HomeViewModel.POINTS_PER_TASK
    val spent = coupons.filter { it.owner == owner }.sumOf { it.cost }
    return earned - spent
}
