package org.lightscout.vatt.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lightscout.vatt.domain.model.ClassSession
import org.lightscout.vatt.domain.model.UserBookingStatus
import org.lightscout.vatt.presentation.common.formatTime

@Composable
fun ClassRow(
    session: ClassSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(76.dp)) {
            Text(session.startsAt.formatTime(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                session.endsAt.formatTime(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        ImageRefTile(
            imageRef = session.type.imageRef,
            label = session.title,
            modifier = Modifier.size(44.dp),
            cornerRadius = 10,
        )
        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(session.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                session.trainer,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        ClassStatusBadge(session)
    }
}

@Composable
fun ClassStatusBadge(session: ClassSession, modifier: Modifier = Modifier) {
    val success = Color(0xFF1D9E75)
    val warn = Color(0xFFBA7517)
    when (session.userBookingStatus) {
        UserBookingStatus.BOOKED -> StatusBadge("Booked", success, modifier)
        UserBookingStatus.WAITLISTED -> StatusBadge("Waitlisted", warn, modifier)
        else -> when {
            session.isFull -> StatusBadge("Full", MaterialTheme.colorScheme.error, modifier)
            else -> Text(
                "${session.available} spots",
                modifier = modifier,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}
