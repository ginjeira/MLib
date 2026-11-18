package app.spotygram

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val OfflineGreen = Color(0xFF1ED760)

@Composable
fun OfflineDownloadButton(
    downloaded: Boolean,
    downloading: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val tint = if (downloaded) OfflineGreen else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        enabled = enabled && !downloaded && !downloading,
        modifier = Modifier
            .size(40.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(if (downloaded) OfflineGreen.copy(alpha = 0.13f) else Color.Transparent, CircleShape)
                .border(1.5.dp, tint.copy(alpha = if (downloaded) 1f else 0.65f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                downloading -> CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                )
                else -> Icon(
                    Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
