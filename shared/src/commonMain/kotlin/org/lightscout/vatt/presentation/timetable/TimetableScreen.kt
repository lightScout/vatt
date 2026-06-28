package org.lightscout.vatt.presentation.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.lightscout.vatt.domain.model.TimetableDay
import org.lightscout.vatt.presentation.common.formatDayHeader
import org.lightscout.vatt.presentation.common.shortLabel
import org.lightscout.vatt.presentation.components.ClassRow
import org.lightscout.vatt.presentation.components.ErrorView
import org.lightscout.vatt.presentation.components.LoadingView

@Composable
fun TimetableScreen(
    onOpenClass: (String) -> Unit,
    viewModel: TimetableViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> LoadingView()
        state.error != null && state.timetable == null ->
            ErrorView(state.error!!, onRetry = viewModel::load)
        state.timetable != null -> {
            val timetable = state.timetable!!
            val visibleDays: List<TimetableDay> = state.selectedDate?.let { sel ->
                timetable.days.filter { it.date == sel }
            } ?: timetable.days

            Column(Modifier.fillMaxSize()) {
                // Day filter chips: "All" + each day in the week.
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedDate == null,
                            onClick = { viewModel.selectDate(null) },
                            label = { Text("All") },
                        )
                    }
                    items(timetable.days) { day ->
                        FilterChip(
                            selected = state.selectedDate == day.date,
                            onClick = { viewModel.selectDate(day.date) },
                            label = { Text(day.date.shortLabel()) },
                        )
                    }
                }

                val daysWithClasses = visibleDays.filter { it.classes.isNotEmpty() }
                if (daysWithClasses.isEmpty()) {
                    ErrorView(
                        "No classes to show for this selection. The rolling week may be nearly over — try another day.",
                        onRetry = null,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                    ) {
                        daysWithClasses.forEach { day ->
                            item(key = "header-${day.date}") {
                                Text(
                                    day.date.formatDayHeader(),
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            items(day.classes, key = { it.classId }) { session ->
                                ClassRow(session = session, onClick = { onOpenClass(session.classId) })
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
