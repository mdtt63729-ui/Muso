package com.muso.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Echo Music settings list (ported): entries rendered as connected rounded card groups —
 * the first and last cards in a group carry 24dp outer corners, middle cards 4dp, so a
 * whole group reads as one capsule. Icons sit in 40dp rounded boxes, titles in
 * titleMedium, descriptions in bodyMedium, and cards animate size changes.
 */
data class Material3SettingsItem(
    val icon: Painter? = null,
    val customIcon: (@Composable () -> Unit)? = null,
    val title: String,
    val description: String? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val tintIcon: Boolean = true,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, item ->
                val shape: Shape = when {
                    items.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(
                        topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp
                    )
                    index == items.size - 1 -> RoundedCornerShape(
                        topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp
                    )
                    else -> RoundedCornerShape(4.dp)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Material3SettingsItemRow(item)
                }
            }
        }
    }
}

@Composable
private fun Material3SettingsItemRow(item: Material3SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && item.onClick != null,
                onClick = item.onClick ?: {}
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBox: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val tint =
                    if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                if (item.customIcon != null) {
                    item.customIcon.invoke()
                } else if (item.icon != null) {
                    if (item.showBadge) {
                        BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                            Icon(
                                painter = item.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        iconBox()

        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    color = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(item.title)
            }

            item.description?.let {
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = if (!item.enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(it)
                }
            }
        }
    }
}
