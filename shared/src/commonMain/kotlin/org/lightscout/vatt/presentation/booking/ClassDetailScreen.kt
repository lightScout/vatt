package org.lightscout.vatt.presentation.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.lightscout.vatt.domain.model.BookingStatus
import org.lightscout.vatt.domain.model.ClassSession
import org.lightscout.vatt.presentation.common.formatDateFull
import org.lightscout.vatt.presentation.common.formatTime
import org.lightscout.vatt.presentation.components.ErrorView
import org.lightscout.vatt.presentation.components.ImageRefTile

private val Success = Color(0xFF1D9E75)
private val Warn = Color(0xFFBA7517)

@Composable
fun ClassDetailScreen(
    classId: String,
    onBookingCancelled: () -> Unit,
    viewModel: BookingViewModel = koinViewModel(),
) {
    LaunchedEffect(classId) { viewModel.bind(classId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.notFound) {
        ErrorView("We couldn't open this class. Please go back and try again.", onRetry = null)
        return
    }
    val session = state.current ?: return

    LaunchedEffect(state.cancelled) { if (state.cancelled) onBookingCancelled() }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ClassHeader(session)
            DetailCard(session)

            when {
                state.result != null -> ConfirmationCard(state, viewModel)
                session.hasReservation -> ExistingReservationCard(session)
                else -> Unit
            }

            state.error?.let { ErrorBanner(it) }

            ActionButtons(state, viewModel)
        }
    }

    if (state.waitlistPrompt) {
        WaitlistDialog(onConfirm = viewModel::confirmWaitlist, onDismiss = viewModel::dismissWaitlist)
    }
    if (state.cancelForfeitPrompt) {
        ForfeitDialog(onConfirm = viewModel::confirmCancel, onDismiss = viewModel::dismissCancelForfeit)
    }
}

@Composable
private fun ClassHeader(session: ClassSession) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ImageRefTile(session.type.imageRef, session.title, modifier = Modifier.size(56.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(session.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("with ${session.trainer}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailCard(session: ClassSession) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailRow("Date", session.startsAt.formatDateFull())
            DetailRow("Time", "${session.startsAt.formatTime()} — ${session.endsAt.formatTime()}")
            DetailRow("Availability", "${session.available} of ${session.spots} spots")
            if (session.waitlistCount > 0) DetailRow("Waitlist", "${session.waitlistCount} waiting")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConfirmationCard(state: BookingUiState, viewModel: BookingViewModel) {
    val booking = state.result!!
    val waitlisted = booking.status == BookingStatus.WAITLISTED
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = (if (waitlisted) Warn else Success).copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (waitlisted) "You're on the waitlist" else "Booking confirmed!",
                color = if (waitlisted) Warn else Success,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            if (waitlisted && booking.waitlistPosition != null) {
                Text("Position ${booking.waitlistPosition}", color = Warn, fontSize = 13.sp)
            }
            Text("Booking ID: ${booking.bookingId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    state.reminderFeedback?.let {
        Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = viewModel::setLocalReminder, modifier = Modifier.weight(1f)) { Text("Set Reminder") }
        OutlinedButton(onClick = viewModel::addToCalendar, modifier = Modifier.weight(1f)) { Text("Add to Calendar") }
    }
}

@Composable
private fun ExistingReservationCard(session: ClassSession) {
    val waitlisted = session.isWaitlistedByUser
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = (if (waitlisted) Warn else Success).copy(alpha = 0.12f))) {
        Text(
            if (waitlisted) "You're on the waitlist for this class" else "You're booked for this class",
            modifier = Modifier.padding(16.dp),
            color = if (waitlisted) Warn else Success,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))) {
        Text(message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
}

@Composable
private fun ActionButtons(state: BookingUiState, viewModel: BookingViewModel) {
    val session = state.current ?: return
    val hasReservation = state.hasReservation
    val within12h = viewModel.isWithinForfeitWindow()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (hasReservation) {
            if (within12h && !state.cancelled) {
                Text(
                    "This class starts within 12 hours. Cancelling may forfeit any associated cost.",
                    color = Warn,
                    fontSize = 12.sp,
                )
            }
            Button(
                onClick = viewModel::onCancelClicked,
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                ButtonContent(state.isProcessing, "Cancel Booking")
            }
        } else {
            Button(
                onClick = viewModel::onBookClicked,
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ButtonContent(state.isProcessing, if (session.isFull) "Join Waitlist" else "Book Class")
            }
        }
    }
}

@Composable
private fun ButtonContent(processing: Boolean, label: String) {
    if (processing) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
    }
    Text(if (processing) "Please wait…" else label)
}

@Composable
private fun WaitlistDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Class is full") },
        text = { Text("This class is fully booked. Would you like to join the waitlist? You'll be notified if a spot opens up.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Join Waitlist") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@Composable
private fun ForfeitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel within 12 hours?") },
        text = { Text("This class starts within 12 hours. Cancelling now may forfeit any cost associated with the booking. Do you want to continue?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Cancel anyway") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep booking") } },
    )
}
