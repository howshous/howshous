package io.github.howshous.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.howshous.R
import io.github.howshous.data.models.Listing
import io.github.howshous.ui.theme.MutedGray
import io.github.howshous.ui.theme.NearWhite
import io.github.howshous.ui.theme.PricePointGreen
import io.github.howshous.ui.theme.slightlyGray
import io.github.howshous.utils.CurrencyFormatter

@Composable
fun ListingCard(
    listing: Listing,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showStatus: Boolean = false,
    showViews: Boolean = false
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = clickableModifier,
        colors = CardDefaults.cardColors(containerColor = NearWhite)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(84.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (listing.photos.isNotEmpty()) {
                AsyncImage(
                    model = listing.photos[0],
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MutedGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        tint = slightlyGray
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 120.dp)
            ) {
                Text(
                    listing.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    listing.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = slightlyGray
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    CurrencyFormatter.formatRent(listing.price),
                    style = MaterialTheme.typography.labelLarge,
                    color = PricePointGreen
                )
                val maxOccupancy = listing.capacity.coerceAtLeast(1)
                val currentOccupancy = listing.currentOccupancy.coerceAtLeast(0).coerceAtMost(maxOccupancy)
                val remainingSlots = (maxOccupancy - currentOccupancy).coerceAtLeast(0)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Slots: $currentOccupancy / $maxOccupancy occupied, $remainingSlots available",
                    style = MaterialTheme.typography.labelSmall,
                    color = slightlyGray
                )

                Spacer(Modifier.height(4.dp))
                ReviewSummaryRow(summary = listing.reviewSummary, compact = true)

                if (showViews) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.i_eyeopen),
                            contentDescription = null,
                            tint = slightlyGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "${listing.uniqueViewCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = slightlyGray
                        )
                    }
                }
            }

            if (showStatus) {
                val displayStatus = listing.status
                val statusColor = when (displayStatus) {
                    "active" -> PricePointGreen
                    "under_review" -> Color(0xFFE27D23)
                    "inactive" -> slightlyGray
                    "rejected", "delisted" -> Color(0xFFB00020)
                    else -> slightlyGray
                }
                Text(
                    displayStatus.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}
