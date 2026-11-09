package com.buge.rebuttons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReButtonsSettingsTheme {
                ReButtonsSettingsPage(onBack = ::finish)
            }
        }
    }
}

@Composable
private fun ReButtonsSettingsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        MaterialTheme.colorScheme
    }
    MaterialTheme(colorScheme = colors) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReButtonsSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { ReButtonsPreferences.get(context) }
    var scale by remember {
        mutableFloatStateOf(
            preferences.getFloat(
                ReButtonsPreferences.COMPONENT_SCALE,
                ReButtonsPreferences.DEFAULT_SCALE,
            ),
        )
    }
    var hideTime by remember {
        mutableStateOf(preferences.getBoolean(ReButtonsPreferences.HIDE_TIME, false))
    }
    val previewHeight by animateDpAsState(
        targetValue = (68f * scale).dp,
        animationSpec = spring(),
        label = "preview height",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ReButtons", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Accessibility controls",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.RestartAlt, "Back")
                    }
                },
                actions = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.padding(end = 20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessibilityNew,
                            contentDescription = "Accessibility",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Component", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Customize the floating controls",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    ComponentPreview(scale = scale, height = previewHeight)
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Component size") },
                        supportingContent = {
                            Text("${(scale * 100).toInt()}% of the default size")
                        },
                        leadingContent = {
                            Icon(Icons.Filled.Straighten, "Size")
                        },
                    )
                    Slider(
                        value = scale,
                        onValueChange = {
                            scale = it
                            preferences.edit()
                                .putFloat(ReButtonsPreferences.COMPONENT_SCALE, it)
                                .apply()
                        },
                        valueRange = 0.75f..1.5f,
                        steps = 2,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("75%", style = MaterialTheme.typography.labelSmall)
                        Text("100%", style = MaterialTheme.typography.labelSmall)
                        Text("150%", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Hide time") },
                        supportingContent = { Text("Remove the clock from the floating component") },
                        leadingContent = {
                            Icon(Icons.Filled.VisibilityOff, "Hide time")
                        },
                        trailingContent = {
                            Switch(
                                checked = hideTime,
                                onCheckedChange = {
                                    hideTime = it
                                    preferences.edit()
                                        .putBoolean(ReButtonsPreferences.HIDE_TIME, it)
                                        .apply()
                                },
                            )
                        },
                    )
                }
            }

            Text(
                "Changes apply to the accessibility overlay automatically.",
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComponentPreview(scale: Float, height: androidx.compose.ui.unit.Dp) {
    val width = (68f * scale).dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PreviewButton(width, height, top = true)
            PreviewButton(width, height, top = false)
        }
    }
}

@Composable
private fun PreviewButton(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    top: Boolean,
) {
    val shape = if (top) {
        RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 34.dp, bottomEnd = 34.dp)
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(MaterialTheme.colorScheme.primaryContainer, shape),
    )
}
