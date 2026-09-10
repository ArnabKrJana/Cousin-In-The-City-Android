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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneforth.cousininthecity.domain.model.ChatThread
import com.oneforth.cousininthecity.ui.components.ThreadItem
import com.oneforth.cousininthecity.ui.viewmodels.ThreadUiState

@Composable
fun DrawerContent(
    threadUiState: ThreadUiState,
    onNewChat: () -> Unit,
    onThreadSelected: (String) -> Unit,
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
            
            Text(
                "Recents", 
                modifier = Modifier.padding(horizontal = 24.dp), 
                fontSize = 14.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Thread List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (threadUiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(threadUiState.threads) { thread ->
                            ThreadItem(
                                thread = thread,
                                onClick = { onThreadSelected(thread.id) }
                            )
                        }
                    }
                }
            }
            
            // Bottom Profile Area
            Row(
                modifier = Modifier.visible(false)
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.onPrimary)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Arnab Kumar Jana", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawerContentPreview() {
    MaterialTheme {
        DrawerContent(
            threadUiState = ThreadUiState(
                threads = listOf(
                    ChatThread( "1", title = "Mumbai Relocation", deviceId = "test"),
                    ChatThread( "2", title = "Bangalore Budgeting", deviceId = "test")
                ),
                isLoading = false
            ),
            onNewChat = {},
            onThreadSelected = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DrawerContentLoadingPreview() {
    MaterialTheme {
        DrawerContent(
            threadUiState = ThreadUiState(
                threads = emptyList(),
                isLoading = true
            ),
            onNewChat = {},
            onThreadSelected = {}
        )
    }
}
