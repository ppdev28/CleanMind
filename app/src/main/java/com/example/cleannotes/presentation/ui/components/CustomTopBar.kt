package com.example.cleannotes.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cleannotes.R

@Composable
fun CustomTopBar(onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(R.drawable.custom_top_bar_icon), null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(7.dp).size(28.dp))
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 12.dp))
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
