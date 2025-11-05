package com.example.bikey.ui.pricing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bikey.ui.theme.*
import com.example.bikey.ui.UserContext
import com.example.bikey.ui.PricingPlan
import com.example.bikey.ui.network.pricingPlanApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Composable
fun PricingScreen(
    onBack: () -> Unit,
    onRegister: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Choose Your Plan",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                brush = Brush.linearGradient(
                    colors = listOf(EcoGreen, DarkGreen)
                )
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Flexible options for every rider",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Pay As You Go Plan
        PricingCard(
            title = "Pay As You Go",
            price = "$1",
            eBikePrice = "$0.75",
            period = "for 45 minutes",
            eBikePeriod = "for 2 hours",
            features = listOf(
                "No commitment",
                "Only pay to unlock",
                "Perfect for occasional rides"
            ),
            isPopular = false,
            isSelecting = onRegister == null,
            pricingPlan = PricingPlan.DEFAULT_PAY_NOW
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Plan (Popular)
        PricingCard(
            title = "Monthly Pass",
            price = "$14.99",
            period = "per month",
            features = listOf(
                "Unlimited number of 45-minute rides",
                "No unlock fees",
                "Cancel anytime"
            ),
            isPopular = true,
            isSelecting = onRegister == null,
            pricingPlan = PricingPlan.MONTHLY_SUBSCRIPTION
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Annual Plan
        PricingCard(
            title = "Annual Pass",
            price = "$119.99",
            period = "per year",
            features = listOf(
                "Save $60 per year",
                "Exclusive events + all Monthly Pass benefits",
                "Best value!"
            ),
            isPopular = false,
            isSelecting = onRegister == null,
            pricingPlan = PricingPlan.ANNUAL_SUBSCRIPTION
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Overtime rate: $0.20/min, or $0.10/min for e-bikes\nE-bike fee: $0.20/min for electricity use",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        onRegister?.let {
            Button(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoGreen
                )
            ) {
                Text(
                    text = "Register",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PureWhite,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EcoGreen
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, EcoGreen)
        ) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PricingCard(
    title: String,
    price: String,
    eBikePrice: String? = null,
    period: String,
    eBikePeriod: String? = null,
    features: List<String>,
    isPopular: Boolean,
    isSelecting: Boolean = false,
    pricingPlan: PricingPlan
) {
    val coroutineScope = rememberCoroutineScope()
    val _events = MutableSharedFlow<PricingPlanEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    var state by remember { mutableStateOf(PricingPlanState()) }
    fun set(upd: PricingPlanState.() -> PricingPlanState) { state = state.upd() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isPopular) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(EcoGreen, DarkGreen)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isPopular) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EcoGreen)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Popular",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "MOST POPULAR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = PureWhite
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EcoGreen
                    )
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            eBikePrice?.let { eBikePeriod?.let {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "OR",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))}
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = eBikePrice,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EcoGreen
                        )
                    )
                    Text(
                        text = "$eBikePeriod with an e-bike",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }}

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                features.forEach { feature ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Included",
                            tint = EcoGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if(isSelecting) {Button(
                onClick = {
                    set { copy(isLoading = true, errorMsg = null, successMsg = null) }
                    coroutineScope.launch {
                        try {
                            UserContext.id?.let {
                                val res =
                                    pricingPlanApi.changePricingPlan(UserContext.id!!, pricingPlan.toString())
                                if (res.isSuccessful && (res.body() ?: false)) {
                                    UserContext.user?.pricingPlan = pricingPlan
                                    val planChange = if(pricingPlan == PricingPlan.DEFAULT_PAY_NOW) "Updated" else "Purchased"
                                    set { copy(isLoading = false, successMsg = "Successfully ${planChange.lowercase()} pricing plan!") }
                                    _events.emit(PricingPlanEvent.Success("Pricing Plan ${planChange}!", pricingPlan))
                                    UserContext.nav?.navigate("home")
                                } else {
                                    val err = "Operation failed."
                                    set { copy(isLoading = false, errorMsg = err) }
                                    _events.emit(PricingPlanEvent.ShowMessage(err))
                                }
                            } ?: {
                                coroutineScope.launch {
                                    val err = "User not logged in."
                                    set { copy(isLoading = false, errorMsg = err) }
                                    _events.emit(PricingPlanEvent.ShowMessage(err))
                                }
                            }
                        } catch (_: Exception) {
                            val err = "Network error. Please check your connection and try again."
                            set { copy(isLoading = false, errorMsg = err) }
                            _events.emit(PricingPlanEvent.ShowMessage(err))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = UserContext.pricingPlan != pricingPlan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular) EcoGreen else DarkGreen
                )
            ) {
                Text(
                    text = if(UserContext.pricingPlan == pricingPlan) {if(pricingPlan == PricingPlan.DEFAULT_PAY_NOW) "Selected" else "Subscribed"} else {if(pricingPlan == PricingPlan.DEFAULT_PAY_NOW) "Select Plan" else "Purchase Plan"},
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PureWhite,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }}
        }
    }
}

data class PricingPlanState(
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val successMsg: String? = null
)

sealed interface PricingPlanEvent {
    data class Success(val msg: String, val pricingPlan: PricingPlan) : PricingPlanEvent
    data class ShowMessage(val message: String) : PricingPlanEvent
}