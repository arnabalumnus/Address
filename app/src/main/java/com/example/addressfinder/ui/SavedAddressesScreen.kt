package com.example.addressfinder.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.addressfinder.SavedAddressesViewModel
import com.example.addressfinder.data.AddressEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    onBack: () -> Unit,
    viewModel: SavedAddressesViewModel = viewModel()
) {
    val savedAddresses by viewModel.savedAddresses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (savedAddresses.isEmpty()) {
                EmptySavedAddresses()
            } else {
                SwipeCardStack(
                    items = savedAddresses,
                    onDelete = viewModel::deleteAddress
                )
            }
        }
    }
}

@Composable
private fun EmptySavedAddresses() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No saved addresses yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Fetch your address from the home screen and it will show up here.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

/** Approximate horizontal drag distance, in dp, that commits a swipe. */
private val SwipeCommitThreshold = 120.dp

@Composable
private fun SwipeCardStack(items: List<AddressEntity>, onDelete: (AddressEntity) -> Unit) {
    var currentIndex by remember(items) { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (currentIndex < items.size) "${currentIndex + 1} / ${items.size}" else "${items.size} / ${items.size}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentIndex >= items.size) {
                AllCaughtUp(onRestart = { currentIndex = 0 })
            } else {
                val visibleCount = minOf(3, items.size - currentIndex)
                for (depth in visibleCount - 1 downTo 0) {
                    val item = items[currentIndex + depth]
                    if (depth == 0) {
                        SwipeableCard(
                            key = item.id,
                            item = item,
                            onNext = { currentIndex++ },
                            onDelete = {
                                onDelete(item)
                                currentIndex++
                            }
                        )
                    } else {
                        StackedCardBackground(depth = depth)
                    }
                }
            }
        }

        if (currentIndex < items.size) {
            SwipeHint()
        }
    }
}

@Composable
private fun AllCaughtUp(onRestart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Replay,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text("You've gone through them all", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRestart) {
            Text("Start over")
        }
    }
}

@Composable
private fun SwipeHint() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            "Swipe right for next • Swipe left to delete",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StackedCardBackground(depth: Int) {
    val scale = 1f - depth * 0.05f
    val yOffsetDp = (depth * 12).dp
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(0.72f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = yOffsetDp.toPx()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1f - depth * 0.2f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {}
}

@Composable
private fun SwipeableCard(key: Long, item: AddressEntity, onNext: () -> Unit, onDelete: () -> Unit) {
    val offsetX = remember(key) { Animatable(0f) }
    val offsetY = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { SwipeCommitThreshold.toPx() }
    val flingDistancePx = with(density) { 1000.dp.toPx() }

    val progress = (offsetX.value / thresholdPx).coerceIn(-1f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(0.72f)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = progress * 12f
            }
            .pointerInput(key) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f)
                        }
                    },
                    onDragEnd = {
                        val current = offsetX.value
                        when {
                            current > thresholdPx -> scope.launch {
                                offsetX.animateTo(flingDistancePx, tween(250))
                                onNext()
                            }

                            current < -thresholdPx -> scope.launch {
                                offsetX.animateTo(-flingDistancePx, tween(250))
                                onDelete()
                            }

                            else -> {
                                scope.launch { offsetX.animateTo(0f, spring()) }
                                scope.launch { offsetY.animateTo(0f, spring()) }
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AddressCardContent(item)

            SwipeBadge(
                text = "NEXT ▶",
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                alignment = Alignment.TopEnd,
                alpha = progress.coerceIn(0f, 1f)
            )
            SwipeBadge(
                text = "◀ DELETE",
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                alignment = Alignment.TopStart,
                alpha = (-progress).coerceIn(0f, 1f)
            )
        }
    }
}

@Composable
private fun BoxScope.SwipeBadge(text: String, color: Color, contentColor: Color, alignment: Alignment, alpha: Float) {
    if (alpha <= 0f) return
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(16.dp)
            .alpha(alpha)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = color)) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = contentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AddressCardContent(item: AddressEntity) {
    val rows = listOfNotNull(
        item.thoroughfare?.let { "Street" to it },
        item.subLocality?.let { "Sub-locality" to it },
        item.locality?.let { "City" to it },
        item.subAdminArea?.let { "District" to it },
        item.adminArea?.let { "State/Region" to it },
        item.postalCode?.let { "Postal code" to it },
        item.countryName?.let { "Country" to "$it (${item.countryCode ?: "-"})" },
        "Coordinates" to "${item.latitude}, ${item.longitude}"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.fullAddressLine ?: "Unknown address",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatTimestamp(item.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(rows) { (label, value) ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun formatTimestamp(timestampMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return formatter.format(Date(timestampMillis))
}
