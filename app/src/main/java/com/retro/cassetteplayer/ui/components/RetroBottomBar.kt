package com.retro.cassetteplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.retro.cassetteplayer.ui.theme.TextSecondary
import com.retro.cassetteplayer.ui.theme.Silver
import com.retro.cassetteplayer.ui.theme.HotlineAmber
import com.retro.cassetteplayer.ui.theme.HotlineOrange

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** Bottom navigation styled like a deck's selector switches with an LED above the active one. */
@Composable
fun RetroBottomBar(
    destinations: List<BottomDestination>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navyBrushedMetal()
            .drawBehind {
                drawLine(Silver.copy(alpha = 0.55f), Offset(0f, 0f), Offset(size.width, 0f), 1.5.dp.toPx())
            }
            .navigationBarsPadding()
            .height(68.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        destinations.forEach { destination ->
            val selected = destination.route == currentRoute
            val tint = if (selected) HotlineAmber else TextSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(role = Role.Tab) { onSelect(destination.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val ledShape = RoundedCornerShape(2.dp)
                Box(
                    Modifier
                        .size(width = 20.dp, height = 3.dp)
                        .shadow(if (selected) 4.dp else 0.dp, ledShape, spotColor = HotlineOrange)
                        .background(if (selected) HotlineOrange else Color.Black.copy(alpha = 0.5f), ledShape)
                )
                Spacer(Modifier.height(6.dp))
                Icon(destination.icon, contentDescription = destination.label, tint = tint)
                Text(
                    text = destination.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                )
            }
        }
    }
}
