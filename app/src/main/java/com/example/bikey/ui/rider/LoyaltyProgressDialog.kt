package com.example.bikey.ui.rider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bikey.ui.network.LoyaltyProgressDTO
import com.example.bikey.ui.network.bikeAPI
import com.example.bikey.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoyaltyProgressDialog(
    userId: String,
    onDismiss: () -> Unit
) {
    var progressData by remember { mutableStateOf<LoyaltyProgressDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        scope.launch {
            try {
                val response = bikeAPI.getLoyaltyProgress(userId)
                if (response.isSuccessful && response.body() != null) {
                    progressData = response.body()
                    isLoading = false
                } else {
                    errorMessage = "Failed to load loyalty progress"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Loyalty Progress",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoGreen
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EcoGreen)
                        }
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    progressData != null -> {
                        LoyaltyProgressContent(progressData!!)
                    }
                }
            }
        }
    }
}

@Composable
fun LoyaltyProgressContent(data: LoyaltyProgressDTO) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Current Tier Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (data.currentTier) {
                    "GOLD" -> Color(0xFFFFD700)
                    "SILVER" -> Color(0xFFC0C0C0)
                    "BRONZE" -> Color(0xFFCD7F32)
                    else -> Color(0xFFE0E0E0)
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Current Tier",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = data.currentTierDisplayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discount: ${data.currentDiscount}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Total Rides: ${data.totalCompletedTrips}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next Tier Section
        if (data.nextTier != null) {
            Text(
                text = "Next Tier: ${data.nextTierDisplayName}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Text(
                text = "Unlock ${data.nextTierDiscount}% discount",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Requirements Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Requirements to Unlock:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Display requirements based on current tier
                    when (data.currentTier) {
                        "NONE" -> {
                            BronzeRequirements(data)
                        }
                        "BRONZE" -> {
                            SilverRequirements(data)
                        }
                        "SILVER" -> {
                            GoldRequirements(data)
                        }
                    }
                }
            }
        } else {
            // Max tier reached
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Congratulations!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Text(
                        text = "You've reached the highest tier!",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // All Tier Features Section
        Text(
            text = "All Loyalty Tiers",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreen
        )
        Text(
            text = "See what benefits await you",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bronze Tier Features
        TierFeatureCard(
            tierName = "Bronze Tier",
            tierColor = Color(0xFFCD7F32),
            discount = "5% off",
            features = listOf(
                "5% discount on all trips"
            ),
            isUnlocked = data.currentTier == "BRONZE" || data.currentTier == "SILVER" || data.currentTier == "GOLD"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Silver Tier Features
        TierFeatureCard(
            tierName = "Silver Tier",
            tierColor = Color(0xFFC0C0C0),
            discount = "10% off",
            features = listOf(
                "10% discount on all trips",
                "Extra 2 minutes reservation hold time"
            ),
            isUnlocked = data.currentTier == "SILVER" || data.currentTier == "GOLD"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Gold Tier Features
        TierFeatureCard(
            tierName = "Gold Tier",
            tierColor = Color(0xFFFFD700),
            discount = "15% off",
            features = listOf(
                "15% discount on all trips",
                "Extra 5 minutes reservation hold time"
            ),
            isUnlocked = data.currentTier == "GOLD"
        )
    }
}

@Composable
fun BronzeRequirements(data: LoyaltyProgressDTO) {
    val progress = data.currentProgress
    val requirements = data.requirementsForNext

    val completedTrips = progress["completedTrips"]?.toIntOrNull() ?: 0
    val incompleteTrips = progress["incompleteTrips"]?.toIntOrNull() ?: 0
    val meetsNoIncomplete = progress["meetsNoIncompleteRequirement"]?.toBoolean() ?: false
    val requiredTrips = requirements["requiredTrips"]?.toIntOrNull() ?: 10

    // Calculate what user needs to do
    val tripsNeeded = if (completedTrips >= requiredTrips) 0 else requiredTrips - completedTrips
    val tripsMet = completedTrips >= requiredTrips

    // No missed reservations within the last year
    RequirementItem(
        text = "No missed reservations (last year)",
        current = completedTrips,
        required = requiredTrips,
        isMet = tripsMet && meetsNoIncomplete,
        extraInfo = if (incompleteTrips > 0) "You have $incompleteTrips missed reservation(s) in the last year" else "No missed reservations",
        actionNeeded = if (incompleteTrips > 0) "Complete trips without missing any reservations" else null
    )

    // Returned all bikes successfully (lifetime requirement)
    RequirementItem(
        text = "All bikes returned successfully (lifetime)",
        current = null,
        required = null,
        isMet = meetsNoIncomplete,
        extraInfo = if (incompleteTrips > 0) "You have incomplete trips in your history" else "All bikes returned",
        actionNeeded = if (!meetsNoIncomplete) "You must have returned ALL bikes successfully (lifetime requirement). Any incomplete trip in your entire history blocks Bronze tier." else null
    )

    // At least 10 completed trips in the last year
    RequirementItem(
        text = "Surpass 10 trips (last year)",
        current = completedTrips,
        required = requiredTrips,
        isMet = tripsMet,
        actionNeeded = if (!tripsMet) "Complete $tripsNeeded more trip${if (tripsNeeded != 1) "s" else ""}" else null
    )

    // Add warning message if they have enough trips but incomplete trips are blocking them
    if (tripsMet && !meetsNoIncomplete) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Almost There!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF856404)
                    )
                    Text(
                        text = "You have enough trips ($completedTrips/$requiredTrips) but you have incomplete trips preventing Bronze tier unlock. All bikes ever taken must be returned successfully.",
                        fontSize = 12.sp,
                        color = Color(0xFF856404)
                    )
                }
            }
        }
    }
}

@Composable
fun SilverRequirements(data: LoyaltyProgressDTO) {
    val progress = data.currentProgress
    val requirements = data.requirementsForNext

    val monthsMeetingReq = progress["monthsMeetingRequirement"]?.toIntOrNull() ?: 0
    val requiredMonths = requirements["monthsRequired"]?.toIntOrNull() ?: 3
    val minTripsPerMonth = requirements["minTripsPerMonth"]?.toIntOrNull() ?: 5

    val monthsNeeded = if (monthsMeetingReq >= requiredMonths) 0 else requiredMonths - monthsMeetingReq

    // Covers Bronze tier eligibility
    RequirementItem(
        text = "Bronze tier eligibility covered",
        current = null,
        required = null,
        isMet = true, // If they see this screen, Bronze is covered
        extraInfo = "You have Bronze tier",
        actionNeeded = null
    )

    // At least 5 reservations successfully claimed in last year
    RequirementItem(
        text = "5+ successful reservations (last year)",
        current = monthsMeetingReq,
        required = 5,
        isMet = monthsMeetingReq >= 5,
        actionNeeded = if (monthsMeetingReq < 5) "Complete ${5 - monthsMeetingReq} more successful reservations" else null
    )

    // At least 5 trips per month for last 3 months
    RequirementItem(
        text = "5+ trips/month for 3 months",
        current = monthsMeetingReq,
        required = requiredMonths,
        isMet = monthsMeetingReq >= requiredMonths,
        actionNeeded = if (monthsNeeded > 0) "Complete $minTripsPerMonth trips/month for $monthsNeeded more month${if (monthsNeeded != 1) "s" else ""}" else null
    )
}

@Composable
fun GoldRequirements(data: LoyaltyProgressDTO) {
    val progress = data.currentProgress
    val requirements = data.requirementsForNext

    val weeksMeetingReq = progress["weeksMeetingRequirement"]?.toIntOrNull() ?: 0
    val requiredWeeks = requirements["weeksRequired"]?.toIntOrNull() ?: 12
    val minTripsPerWeek = requirements["minTripsPerWeek"]?.toIntOrNull() ?: 5

    val weeksNeeded = if (weeksMeetingReq >= requiredWeeks) 0 else requiredWeeks - weeksMeetingReq

    // Covers Silver tier eligibility
    RequirementItem(
        text = "Silver tier eligibility covered",
        current = null,
        required = null,
        isMet = true, // If they see this screen, Silver is covered
        extraInfo = "You have Silver tier",
        actionNeeded = null
    )

    // 5 trips every week for last 3 months
    RequirementItem(
        text = "5+ trips/week for 3 months (12 weeks)",
        current = weeksMeetingReq,
        required = requiredWeeks,
        isMet = weeksMeetingReq >= requiredWeeks,
        actionNeeded = if (weeksNeeded > 0) "Complete $minTripsPerWeek trips/week for $weeksNeeded more week${if (weeksNeeded != 1) "s" else ""}" else null
    )
}

@Composable
fun RequirementItem(
    text: String,
    current: Int?,
    required: Int?,
    isMet: Boolean,
    extraInfo: String? = null,
    actionNeeded: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            if (current != null && required != null) {
                Text(
                    text = "Progress: $current / $required",
                    fontSize = 12.sp,
                    color = if (isMet) Color(0xFF4CAF50) else DarkGreen,
                    fontWeight = FontWeight.SemiBold
                )
                // Progress bar
                LinearProgressIndicator(
                    progress = { (current.toFloat() / required.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(6.dp),
                    color = if (isMet) Color(0xFF4CAF50) else DarkGreen,
                    trackColor = Color(0xFFE0E0E0),
                )
            }
            if (extraInfo != null) {
                Text(
                    text = extraInfo,
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            // Show what action is needed
            if (!isMet && actionNeeded != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3CD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Text(
                        text = actionNeeded,
                        fontSize = 12.sp,
                        color = Color(0xFF856404),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Icon(
            imageVector = if (isMet) Icons.Default.Star else Icons.Default.Close,
            contentDescription = null,
            tint = if (isMet) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun TierFeatureCard(
    tierName: String,
    tierColor: Color,
    discount: String,
    features: List<String>,
    isUnlocked: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) tierColor.copy(alpha = 0.15f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isUnlocked) 2.dp else 1.dp,
            color = if (isUnlocked) tierColor else Color.LightGray
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = null,
                        tint = tierColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tierName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                        Text(
                            text = discount,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor
                        )
                    }
                }
                if (isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tierColor
                    ) {
                        Text(
                            text = "UNLOCKED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Features list
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUnlocked) "✓" else "•",
                        fontSize = 14.sp,
                        color = if (isUnlocked) tierColor else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = feature,
                        fontSize = 13.sp,
                        color = if (isUnlocked) DarkGreen else Color.Gray
                    )
                }
            }
        }
    }
}
