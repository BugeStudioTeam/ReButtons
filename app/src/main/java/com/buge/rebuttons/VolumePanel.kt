package com.buge.rebuttons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VolumePanel(
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    isRightEdge: Boolean,
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
        var interactionTick by remember { mutableStateOf(0) }
        var pressedAction by remember { mutableStateOf<VolumeAction?>(null) }

        LaunchedEffect(expanded, interactionTick) {
            if (expanded) {
                delay(3000)
                expanded = false
                pressedAction = null
            }
        }

        LaunchedEffect(expanded) {
            if (!expanded) {
                delay(150)
                separated = false
            }
        }

        LaunchedEffect(expanding) {
            if (expanding) {
                delay(150)
                expanded = true
                expanding = false
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                horizontalAlignment = if (isRightEdge) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                ExpressiveVolumeButton(
                    description = "Volume up",
                    icon = Icons.Filled.KeyboardArrowUp,
                    pressed = pressedAction == VolumeAction.UP,
                    expanded = expanded,
                    separated = separated,
                    onPressedChanged = { pressed ->
                        pressedAction = if (pressed) VolumeAction.UP else null
                    },
                    onClick = {
                        interactionTick++
                        if (expanded) {
                            onVolumeUp()
                        } else {
                            separated = true
                            expanding = true
                        }
                    },
                    top = true,
                )
                ExpressiveVolumeButton(
                    description = "Volume down",
                    icon = Icons.Filled.KeyboardArrowDown,
                    pressed = pressedAction == VolumeAction.DOWN,
                    expanded = expanded,
                    separated = separated,
                    onPressedChanged = { pressed ->
                        pressedAction = if (pressed) VolumeAction.DOWN else null
                    },
                    onClick = {
                        interactionTick++
                        if (expanded) {
                            onVolumeDown()
                        } else {
                            separated = true
                            expanding = true
                        }
                    },
                    top = false,
                )
            }
        }
    }
}

private enum class VolumeAction { UP, DOWN }

@Composable
private fun ExpressiveVolumeButton(
    description: String,
    icon: ImageVector,
    pressed: Boolean,
    expanded: Boolean,
    separated: Boolean,
    onPressedChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    top: Boolean,
) {
    val width by animateDpAsState(
        targetValue = if (expanded) 68.dp else 6.dp,
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
            pressed -> 34.dp
            else -> 4.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "button corner radius",
    )
    val shape = if (expanded) {
        mirroredButtonShape(top = top, innerRadius = radius)
    } else {
        RoundedCornerShape(3.dp)
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(68.dp)
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
                    awaitFirstDown(requireUnconsumed = false)
                    onPressedChanged(true)
                    val released = waitForUpOrCancellation()
                    onPressedChanged(false)
                    if (released != null) onClick()
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

private fun mirroredButtonShape(top: Boolean, innerRadius: androidx.compose.ui.unit.Dp): RoundedCornerShape {
    val outerRadius = 34.dp
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
