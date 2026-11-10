package com.buge.rebuttons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = ReButtonsSettings.get(this)
        lifecycleScope.launch {
            settings.preferences.drop(1).collect {
                sendBroadcast(Intent(ACTION_REFRESH).setPackage(packageName))
            }
        }

        setContent {
            val dark = isSystemInDarkTheme()
            val scheme = when {
                Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(this)
                Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(this)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = scheme) {
                ReButtonsSettingsScreen(settings = settings)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.buge.rebuttons.action.REFRESH"
    }
}

private enum class OrderSlot { Volume, Power }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReButtonsSettingsScreen(settings: ReButtonsSettings) {
    val context = LocalContext.current
    val view = LocalView.current
    val preferences by settings.preferences.collectAsState()
    var serviceEnabled by remember { mutableStateOf(AccessibilityStatus.isServiceEnabled(context)) }

    LifecycleResumeEffect(Unit) {
        serviceEnabled = AccessibilityStatus.isServiceEnabled(context)
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("ReButtons") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            ServiceStatusTile(
                enabled = serviceEnabled,
                onOpen = {
                    view.announceForAccessibility("Opening accessibility settings")
                    context.openAccessibilitySettings()
                },
            )
            SizeSection(
                scale = preferences.panelScale,
                onScaleChange = settings::setPanelScale,
            )
            CollapseSection(
                delayMs = preferences.collapseDelayMs,
                onDelayChange = { settings.setCollapseDelayMs(it.toLong()) },
            )
            OrderSection(
                powerFirst = preferences.powerFirst,
                showVolume = preferences.showVolume,
                showPower = preferences.showPower,
                onSwap = { settings.setPowerFirst(!preferences.powerFirst) },
            )
            VisibilitySection(
                showVolume = preferences.showVolume,
                showPower = preferences.showPower,
                onVolumeChange = settings::setShowVolume,
                onPowerChange = settings::setShowPower,
            )
            VersionFooter()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ServiceStatusTile(enabled: Boolean, onOpen: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "status container",
    )
    val content by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onErrorContainer,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "status content",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(container)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = if (enabled) Icons.Filled.Check else Icons.Filled.AccessibilityNew,
            contentDescription = null,
            tint = content,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (enabled) "Service is running" else "Service is off",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
            Text(
                text = if (enabled) "The floating panel is active"
                else "Enable ReButtons in system accessibility settings",
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.85f),
            )
        }
        TextButton(onClick = onOpen) {
            Text(if (enabled) "Open" else "Enable", color = content)
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SizeSection(scale: Float, onScaleChange: (Float) -> Unit) {
    SettingsCard {
        SectionHeader(title = "Component size", subtitle = "Scale the floating button panel")
        ExpressiveSlider(
            value = scale,
            valueRange = ReButtonsSettings.MIN_SCALE..ReButtonsSettings.MAX_SCALE,
            display = "${(scale * 100).roundToInt()}%",
            contentDescription = "Component size",
            onValueChange = onScaleChange,
        )
        ScalePreview(scale = scale)
    }
}

@Composable
private fun ScalePreview(scale: Float) {
    val button = (58 * scale).dp.coerceIn(40.dp, 84.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height((196 * scale).dp.coerceIn(150.dp, 250.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
            PreviewButton(Icons.Filled.KeyboardArrowUp, button)
            PreviewButton(Icons.Filled.KeyboardArrowDown, button)
            Spacer(Modifier.height((10 * scale).dp))
            PreviewButton(Icons.Filled.PowerSettingsNew, button, pill = true)
        }
    }
}

@Composable
private fun PreviewButton(icon: ImageVector, size: Dp, pill: Boolean = false) {
    Box(
        modifier = Modifier
            .width(size)
            .height(size)
            .clip(if (pill) CircleShape else RoundedCornerShape(percent = 32))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

@Composable
private fun CollapseSection(delayMs: Long, onDelayChange: (Int) -> Unit) {
    val seconds = (delayMs / 1000).toInt().coerceIn(1, 10)
    SettingsCard {
        SectionHeader(
            title = "Collapse delay",
            subtitle = "Auto-hide the panel after inactivity",
        )
        ExpressiveSlider(
            value = seconds.toFloat(),
            valueRange = 1f..10f,
            display = if (seconds == 1) "1 second" else "$seconds seconds",
            contentDescription = "Collapse delay",
            onValueChange = {
                val rounded = it.roundToInt()
                if (rounded != seconds) onDelayChange(rounded * 1000)
            },
        )
    }
}

@Composable
private fun OrderSection(
    powerFirst: Boolean,
    showVolume: Boolean,
    showPower: Boolean,
    onSwap: () -> Unit,
) {
    val hasBoth = showVolume && showPower
    val effectivePowerFirst = powerFirst || !showVolume
    val slots = when {
        hasBoth && effectivePowerFirst -> listOf(OrderSlot.Power, OrderSlot.Volume)
        hasBoth -> listOf(OrderSlot.Volume, OrderSlot.Power)
        showVolume -> listOf(OrderSlot.Volume)
        showPower -> listOf(OrderSlot.Power)
        else -> emptyList()
    }

    SettingsCard {
        SectionHeader(
            title = "Button order",
            subtitle = "Arrange the volume and operation controls",
        )
        if (slots.size < 2) {
            Text(
                text = "Enable both buttons to reorder them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                slots.forEachIndexed { index, slot ->
                    OrderChip(
                        slot = slot,
                        modifier = Modifier.weight(1f),
                        onFling = { direction ->
                            val expectedSwap = if (index == 0) direction > 0 else direction < 0
                            if (expectedSwap) onSwap()
                        },
                    )
                }
            }
            FilledTonalButton(onClick = onSwap, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Swap order")
            }
        }
    }
}

@Composable
private fun OrderChip(
    slot: OrderSlot,
    modifier: Modifier,
    onFling: (direction: Int) -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 64.dp.toPx() }
    val label = if (slot == OrderSlot.Volume) "Volume" else "Power"
    val icon = if (slot == OrderSlot.Volume) Icons.Filled.KeyboardArrowUp else Icons.Filled.PowerSettingsNew
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(64.dp)
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .semantics { contentDescription = "$label position" }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch { offset.snapTo(offset.value + delta) }
                },
                onDragStopped = { velocity ->
                    val travelled = offset.value
                    val farEnough = abs(travelled) > threshold || abs(velocity) > 900f
                    scope.launch {
                        offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    if (farEnough) {
                        val direction = when {
                            abs(velocity) > 900f -> velocity.roundToInt()
                            else -> travelled.roundToInt()
                        }
                        if (direction > 0) onFling(1) else onFling(-1)
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun VisibilitySection(
    showVolume: Boolean,
    showPower: Boolean,
    onVolumeChange: (Boolean) -> Unit,
    onPowerChange: (Boolean) -> Unit,
) {
    SettingsCard {
        SectionHeader(
            title = "Visible buttons",
            subtitle = "At least one button must stay visible",
        )
        ToggleRow(
            title = "Volume buttons",
            subtitle = "Volume up and down controls",
            checked = showVolume,
            locked = showVolume && !showPower,
            onChange = onVolumeChange,
        )
        ToggleRow(
            title = "Operation menu button",
            subtitle = "Power and quick action menu",
            checked = showPower,
            locked = showPower && !showVolume,
            onChange = onPowerChange,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    locked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val alpha = if (locked) 0.5f else 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !locked, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onChange(it) },
            enabled = !locked,
        )
    }
}

@Composable
private fun ExpressiveSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    display: String,
    contentDescription: String,
    onValueChange: (Float) -> Unit,
) {
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span == 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = display,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { this.contentDescription = contentDescription },
        ) {
            val trackWidthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            val next = (fraction + delta / trackWidthPx).coerceIn(0f, 1f)
                            onValueChange(valueRange.start + next * span)
                        },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(fraction)
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(fraction.coerceAtLeast(0.0001f)),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionFooter() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }
    Text(
        text = "ReButtons $version",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    )
}

private fun Context.openAccessibilitySettings() {
    val component = ComponentName(this, VolumeAccessibilityService::class.java)
    val direct = Intent(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS)
        .putExtra(Intent.EXTRA_COMPONENT_NAME, component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(direct) }
        .onFailure {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure {
                Toast.makeText(this, "Unable to open accessibility settings", Toast.LENGTH_SHORT).show()
            }
        }
}
