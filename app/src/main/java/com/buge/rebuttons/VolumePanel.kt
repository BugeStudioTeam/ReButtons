package com.buge.rebuttons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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

@Composable
fun VolumePanel(
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else MaterialTheme.colorScheme

    MaterialTheme(colorScheme = scheme) {
        var pressedAction by remember { mutableStateOf<VolumeAction?>(null) }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, dragAmount ->
                            change.consume()
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
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                ExpressiveVolumeButton(
                    description = "Volume up",
                    icon = Icons.Filled.KeyboardArrowUp,
                    pressed = pressedAction == VolumeAction.UP,
                    onPressedChanged = { pressed ->
                        pressedAction = if (pressed) VolumeAction.UP else null
                    },
                    onClick = onVolumeUp,
                    top = true,
                )
                ExpressiveVolumeButton(
                    description = "Volume down",
                    icon = Icons.Filled.KeyboardArrowDown,
                    pressed = pressedAction == VolumeAction.DOWN,
                    onPressedChanged = { pressed ->
                        pressedAction = if (pressed) VolumeAction.DOWN else null
                    },
                    onClick = onVolumeDown,
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
    onPressedChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    top: Boolean,
) {
    val radius by animateDpAsState(
        targetValue = if (pressed) 34.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "button corner radius",
    )
    val shape = mirroredButtonShape(top = top, innerRadius = radius)

    Box(
        modifier = Modifier
            .width(68.dp)
            .height(68.dp)
            .padding(vertical = 2.dp)
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
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
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