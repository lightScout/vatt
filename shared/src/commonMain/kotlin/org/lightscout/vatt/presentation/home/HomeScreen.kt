package org.lightscout.vatt.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.lightscout.vatt.domain.model.HomeBlock
import org.lightscout.vatt.domain.model.HomeTile
import org.lightscout.vatt.presentation.common.formatTime
import org.lightscout.vatt.presentation.components.ClassStatusBadge
import org.lightscout.vatt.presentation.components.ErrorView
import org.lightscout.vatt.presentation.components.ImageRefTile
import org.lightscout.vatt.presentation.components.LoadingView
import org.lightscout.vatt.presentation.components.SectionHeader

@Composable
fun HomeScreen(
    onOpenClass: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> LoadingView()
        state.error != null && state.manifest == null ->
            ErrorView(state.error!!, onRetry = viewModel::load)
        else -> {
            val blocks = state.manifest?.blocks.orEmpty()
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
            ) {
                items(blocks) { block ->
                    // Server-driven: render what we know, skip the rest (e.g. the `experimental` block).
                    HomeBlockView(block, onOpenClass)
                }
            }
        }
    }
}

@Composable
private fun HomeBlockView(block: HomeBlock, onOpenClass: (String) -> Unit) {
    when (block) {
        is HomeBlock.Greeting -> Text(
            block.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        is HomeBlock.Hero -> Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(block.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                block.subtitle?.let {
                    Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        is HomeBlock.MyClub -> {
            SectionHeader("Your home club")
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(block.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    block.addressLine?.let { Muted(it) }
                    block.openingHoursToday?.let { Muted("Today: $it") }
                    block.phoneNumber?.let { Muted(it) }
                }
            }
        }

        is HomeBlock.ClassCarousel -> {
            SectionHeader(block.title ?: "Classes")
            if (block.items.isEmpty()) {
                Muted("No upcoming classes here right now — check the timetable.")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(block.items) { session ->
                        Card(
                            modifier = Modifier.width(150.dp).clip(RoundedCornerShape(12.dp)),
                            onClick = { onOpenClass(session.classId) },
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(session.startsAt.formatTime(), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text(session.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2)
                                Text(session.trainer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                ClassStatusBadge(session)
                            }
                        }
                    }
                }
            }
        }

        is HomeBlock.Rewards -> TileSection(block.title ?: "Your rewards", block.items)
        is HomeBlock.Goals -> TileSection(block.title ?: "Your goals", block.items)

        is HomeBlock.Promotion -> Card(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp)) {
                ImageRefTile(block.imageRef, block.title, modifier = Modifier.size(56.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(block.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    block.subtitle?.let { Muted(it) }
                }
            }
        }

        is HomeBlock.Unknown -> Unit // unrecognised server block — render nothing
    }
}

@Composable
private fun TileSection(title: String, tiles: List<HomeTile>) {
    SectionHeader(title)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(tiles) { tile ->
            Column(modifier = Modifier.width(140.dp)) {
                ImageRefTile(
                    imageRef = tile.imageRef,
                    label = tile.title,
                    modifier = Modifier.fillMaxWidth().size(90.dp),
                )
                Text(tile.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
                tile.subtitle?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                tile.badge?.let {
                    Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
}
