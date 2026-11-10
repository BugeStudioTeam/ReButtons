package com.buge.rebuttons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VolumePanel(
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onPowerDialog: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    isRightEdge: Boolean,
    scale: Float,
    collapseDelayMs: Int,
    volumeEnabled: Boolean,
    powerEnabled: Boolean,
    onCollapsed: () -> Unit,
    onExpandStart: () -> Unit,
) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else MaterialTheme.colorScheme

    MaterialTheme(colorScheme = scheme) {
        var expanded by remember { mutableStateOf(true) }
        var separated by remember { mutableStateOf(true) }
        var expanding by remember { mutableStateOf(false) }
        var collapsedButton by remember { mutableStateOf(false) }
        var interactionTick by remember { mutableStateOf(0) }
        var holdingCount by remember { mutableStateOf(0) }
        var pressedAction by remember { mutableStateOf<VolumeAction?>(null) }

        val buttonSize = (68 * scale).dp
        val circleSize = (48 * scale).dp
        val powerGap by animateDpAsState(
            targetValue = if (separated) (12f * scale).dp else 0.dp,
            animationSpec = tween(durationMillis = 120),
            label = "power spacer",
        )

        val expandRequest: () -> Unit = {
            if (!expanding && !expanded) {
                expanding = true
            }
        }

        LaunchedEffect(expanded, interactionTick, holdingCount, collapseDelayMs) {
            if (expanded && holdingCount == 0) {
                delay(collapseDelayMs.toLong())
                expanded = false
                pressedAction = null
            }
        }

        LaunchedEffect(expanded) {
            if (!expanded) {
                delay(150)
                separated = false
                delay(140)
                collapsedButton = true
                onCollapsed()
            } else {
                collapsedButton = false
            }
        }

        LaunchedEffect(expanding) {
            if (expanding) {
                separated = true
                onExpandStart()
                delay(150)
                expanded = true
                expanding = false
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding((4f * scale).dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, dragAmount ->
                            change.consume()
                            interactionTick++
                            onDrag(dragAmount.x, dragAmount.y)
                        },
                    )
                },
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (isRightEdge) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (collapsedButton) {
                    CollapsedCircle(
                        description = "Expand",
                        size = circleSize,
                        isRightEdge = isRightEdge,
                        onExpandRequest = expandRequest,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = (8f * scale).dp),
                        horizontalAlignment = if (isRightEdge) Alignment.End else Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (volumeEnabled) {
                            ExpressiveVolumeButton(
                                description = "Volume up",
                                icon = Icons.Filled.KeyboardArrowUp,
                                pressed = pressedAction == VolumeAction.UP,
                                expanded = expanded,
                                separated = separated,
                                size = buttonSize,
                                onPressedChanged = { pressed ->
                                    pressedAction = if (pressed) VolumeAction.UP else null
                                    holdingCount = if (pressed) holdingCount + 1 else (holdingCount - 1).coerceAtLeast(0)
                                },
                                onVolumeAction = onVolumeUp,
                                onExpandRequest = expandRequest,
                                top = true,
                            )
                            ExpressiveVolumeButton(
                                description = "Volume down",
                                icon = Icons.Filled.KeyboardArrowDown,
                                pressed = pressedAction == VolumeAction.DOWN,
                                expanded = expanded,
                                separated = separated,
                                size = buttonSize,
                                onPressedChanged = { pressed ->
                                    pressedAction = if (pressed) VolumeAction.DOWN else null
                                    holdingCount = if (pressed) holdingCount + 1 else (holdingCount - 1).coerceAtLeast(0)
                                },
                                onVolumeAction = onVolumeDown,
                                onExpandRequest = expandRequest,
                                top = false,
                            )
                        }
                        if (powerEnabled) {
                            if (volumeEnabled) {
                                Box(modifier = Modifier.height(powerGap))
                            }
                            PowerButton(
                                description = "Power menu",
                                expanded = expanded,
                                separated = separated,
                                size = buttonSize,
                                onPressedChanged = { pressed ->
                                    holdingCount = if (pressed) holdingCount + 1 else (holdingCount - 1).coerceAtLeast(0)
                                },
                                onPowerDialog = onPowerDialog,
                                onExpandRequest = expandRequest,
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class VolumeAction { UP, DOWN }

@Composable
private fun CollapsedCircle(
    description: String,
    size: Dp,
    isRightEdge: Boolean,
    onExpandRequest: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position
                    val slop = viewConfiguration.touchSlop
                    val released = waitForUpOrCancellation()
                    if (released != null && (released.position - downPosition).getDistance() <= slop) {
                        onExpandRequest()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isRightEdge) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ExpressiveVolumeButton(
    description: String,
    icon: ImageVector,
    pressed: Boolean,
    expanded: Boolean,
    separated: Boolean,
    size: Dp,
    onPressedChanged: (Boolean) -> Unit,
    onVolumeAction: () -> Unit,
    onExpandRequest: () -> Unit,
    top: Boolean,
) {
    val currentExpanded by rememberUpdatedState(expanded)
    val width by animateDpAsState(
        targetValue = if (expanded) size else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "button width",
    )
    val gap by animateDpAsState(
        targetValue = if (separated) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 120),
        label = "button gap",
    )
    val radius by animateDpAsState(
        targetValue = when {
            !expanded -> 3.dp
            pressed -> size / 2
            else -> 4.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "button corner radius",
    )
    val shape: RoundedCornerShape = when {
        expanded -> mirroredButtonShape(top = top, innerRadius = radius, outerRadius = size / 2)
        separated -> RoundedCornerShape(3.dp)
        top -> RoundedCornerShape(
            topStart = 3.dp,
            topEnd = 3.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        )
        else -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp,
        )
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(size)
            .padding(vertical = gap)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = shape,
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onPressedChanged(true)
                    val downPosition = down.position
                    val slop = viewConfiguration.touchSlop
                    val armed = currentExpanded
                    if (armed) {
                        onVolumeAction()
                        var up = false
                        var dragging = false
                        while (!up && !dragging) {
                            val event = withTimeoutOrNull(150) { awaitPointerEvent() }
                            if (event == null) {
                                onVolumeAction()
                            } else {
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change != null) {
                                    if ((change.position - downPosition).getDistance() > slop) {
                                        dragging = true
                                    } else if (!change.pressed) {
                                        up = true
                                    }
                                } else if (event.changes.all { !it.pressed }) {
                                    up = true
                                }
                            }
                        }
                    } else {
                        val released = waitForUpOrCancellation()
                        if (released != null && (released.position - downPosition).getDistance() <= slop) {
                            onExpandRequest()
                        }
                    }
                    onPressedChanged(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (expanded) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PowerButton(
    description: String,
    expanded: Boolean,
    separated: Boolean,
    size: Dp,
    onPressedChanged: (Boolean) -> Unit,
    onPowerDialog: () -> Unit,
    onExpandRequest: () -> Unit,
) {
    val currentExpanded by rememberUpdatedState(expanded)
    var pressed by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (expanded) size else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "power width",
    )
    val gap by animateDpAsState(
        targetValue = if (separated) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 120),
        label = "power gap",
    )
    val corner by animateDpAsState(
        targetValue = if (pressed) 12.dp else size / 2,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "power corner",
    )
    val shape: RoundedCornerShape = when {
        expanded -> RoundedCornerShape(corner)
        separated -> RoundedCornerShape(3.dp)
        else -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 3.dp,
            bottomEnd = 3.dp,
        )
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(size)
            .padding(vertical = gap)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = shape,
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onPressedChanged(true)
                    pressed = true
                    val downPosition = down.position
                    val slop = viewConfiguration.touchSlop
                    val armed = currentExpanded
                    val released = waitForUpOrCancellation()
                    pressed = false
                    onPressedChanged(false)
                    val isTap = released != null &&
                        (released.position - downPosition).getDistance() <= slop
                    if (isTap) {
                        if (armed) onPowerDialog() else onExpandRequest()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (expanded) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun mirroredButtonShape(top: Boolean, innerRadius: Dp, outerRadius: Dp): RoundedCornerShape {
    return if (top) {
        RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius,
        )
    } else {
        RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius,
        )
    }
}
