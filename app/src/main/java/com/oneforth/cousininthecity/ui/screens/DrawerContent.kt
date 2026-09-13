package com.oneforth.cousininthecity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneforth.cousininthecity.ui.components.ThreadItem
import com.oneforth.cousininthecity.ui.viewmodels.ThreadUiState

@Composable
fun DrawerContent(
    threadUiState: ThreadUiState,
    onNewChat: () -> Unit,
    onThreadSelected: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.fillMaxWidth(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cousin Assistant", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            // New Chat Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp).clip(shape = CircleShape)
                    .clickable { onNewChat() },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("New chat", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Thread List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (threadUiState.isLoading && threadUiState.threads.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        val pinned = threadUiState.threads.filter { it.isPinned }
                        val recents = threadUiState.threads.filter { !it.isPinned }

                        if (pinned.isNotEmpty()) {
                            item {
                                Text(
                                    "Pinned",
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(pinned) { thread ->
                                ThreadItem(
                                    thread = thread,
                                    onClick = { onThreadSelected(thread.id) },
                                    onTogglePin = { onTogglePin(thread.id, thread.isPinned) }
                                )
                            }
                        }

                        if (recents.isNotEmpty()) {
                            item {
                                Text(
                                    "Recents",
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(recents) { thread ->
                                ThreadItem(
                                    thread = thread,
                                    onClick = { onThreadSelected(thread.id) },
                                    onTogglePin = { onTogglePin(thread.id, thread.isPinned) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

