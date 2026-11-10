package com.buge.rebuttons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReButtonsSettingsTheme {
                ReButtonsSettingsPage()
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

@Composable
private fun ReButtonsSettingsPage() {
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
    var collapseDelay by remember {
        mutableFloatStateOf(
            preferences.getInt(
                ReButtonsPreferences.COLLAPSE_DELAY,
                ReButtonsPreferences.DEFAULT_COLLAPSE_DELAY,
            ).toFloat(),
        )
    }
    var powerOnTop by remember {
        mutableStateOf(
            preferences.getBoolean(
                ReButtonsPreferences.POWER_ON_TOP,
                ReButtonsPreferences.DEFAULT_POWER_ON_TOP,
            ),
        )
    }
    var volumeEnabled by remember {
        mutableStateOf(
            preferences.getBoolean(
                ReButtonsPreferences.VOLUME_ENABLED,
                ReButtonsPreferences.DEFAULT_VOLUME_ENABLED,
            ),
        )
    }
    var powerEnabled by remember {
        mutableStateOf(
            preferences.getBoolean(
                ReButtonsPreferences.POWER_ENABLED,
                ReButtonsPreferences.DEFAULT_POWER_ENABLED,
            ),
        )
    }
    val save = remember(preferences) {
        { key: String, value: Any ->
            preferences.edit().apply {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Int -> putInt(key, value)
                }
            }.apply()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
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
                        supportingContent = { Text("${(scale * 100).toInt()}% of the default size") },
                    )
                    Slider(
                        value = scale,
                        onValueChange = {
                            scale = it
                            save(ReButtonsPreferences.COMPONENT_SCALE, it)
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
                        headlineContent = { Text("Collapse delay") },
                        supportingContent = {
                            Text("Auto-hide the panel after ${(collapseDelay / 1000).toInt()} s")
                        },
                    )
                    Slider(
                        value = collapseDelay,
                        onValueChange = {
                            collapseDelay = it
                            save(ReButtonsPreferences.COLLAPSE_DELAY, it.toInt())
                        },
                        valueRange = ReButtonsPreferences.MIN_COLLAPSE_DELAY.toFloat()..
                            ReButtonsPreferences.MAX_COLLAPSE_DELAY.toFloat(),
                        steps = 8,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("1 s", style = MaterialTheme.typography.labelSmall)
                        Text("5 s", style = MaterialTheme.typography.labelSmall)
                        Text("10 s", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Button order") },
                        supportingContent = { Text("Choose which control appears at the top") },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !powerOnTop,
                                    onClick = {
                                        powerOnTop = false
                                        save(ReButtonsPreferences.POWER_ON_TOP, false)
                                    },
                                    label = { Text("Volume") },
                                )
                                FilterChip(
                                    selected = powerOnTop,
                                    onClick = {
                                        powerOnTop = true
                                        save(ReButtonsPreferences.POWER_ON_TOP, true)
                                    },
                                    label = { Text("Menu") },
                                )
                            }
                        },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Volume controls") },
                        supportingContent = { Text("Show Volume Up and Volume Down") },
                        trailingContent = {
                            Switch(
                                checked = volumeEnabled,
                                enabled = powerEnabled,
                                onCheckedChange = {
                                    volumeEnabled = it
                                    save(ReButtonsPreferences.VOLUME_ENABLED, it)
                                },
                            )
                        },
                    )
                    ListItem(
                        headlineContent = { Text("Power menu") },
                        supportingContent = { Text("Show the system power menu button") },
                        trailingContent = {
                            Switch(
                                checked = powerEnabled,
                                enabled = volumeEnabled,
                                onCheckedChange = {
                                    powerEnabled = it
                                    save(ReButtonsPreferences.POWER_ENABLED, it)
                                },
                            )
                        },
                    )
                }
            }

            Text(
                "At least one control group must remain enabled.",
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
