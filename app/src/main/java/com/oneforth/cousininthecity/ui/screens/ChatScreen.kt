package com.oneforth.cousininthecity.ui.screens
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.tooling.preview.Preview

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.domain.model.MessageRole
import com.oneforth.cousininthecity.ui.viewmodels.ChatUiState
import com.oneforth.cousininthecity.ui.viewmodels.UiEvent
import com.oneforth.cousininthecity.util.NativeIntentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    uiEventFlow: kotlinx.coroutines.flow.Flow<UiEvent>,
    onSendMessage: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleListening: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect one-off UI Events (Jarvis Intents & Snackbars)
    LaunchedEffect(Unit) {
        uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.JarvisAddCalendar -> {
                    val success = NativeIntentUtils.silentlyAddCalendarEvent(
                        context, event.title, event.description, 
                        System.currentTimeMillis() + 86400000, // Dummy: +1 day
                        System.currentTimeMillis() + 90000000
                    )
                    if (!success) Toast.makeText(context, "Requires Calendar Permission!", Toast.LENGTH_SHORT).show()
                }
                is UiEvent.JarvisOpenMap -> {
                    NativeIntentUtils.openGoogleMaps(context, event.locationQuery)
                }
                is UiEvent.JarvisSaveNote -> {
                    NativeIntentUtils.saveToGoogleKeep(context, event.content)
                }
            }
        }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF0F0F11), Color(0xFF14244B)) // Gemini-like dark gradient
    } else {
        listOf(Color.White, Color(0xFFE3F2FD))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cousin ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Assistant", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Profile */ }) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ChatInputField(
                    onSendMessage = onSendMessage, 
                    isLoading = uiState.isLoading,
                    isListening = uiState.isListening,
                    onToggleListening = onToggleListening
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                androidx.compose.animation.Crossfade(
                    targetState = uiState.messages.isEmpty(),
                    label = "ChatStateAnimation"
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyChatState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.messages) { message ->
                                MessageBubble(message)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simplified Star Logo representation
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFB9B9), Color(0xFFDB4437), Color(0xFFF4B400))),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Let's jump in.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(text = message.content, color = textColor, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ChatInputField(
    onSendMessage: (String) -> Unit, 
    isLoading: Boolean,
    isListening: Boolean,
    onToggleListening: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val isPreview = LocalInspectionMode.current
    val toneGen = remember(isPreview) {
        if (!isPreview) {
            runCatching {
                ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
            }.getOrNull()
        } else null
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Attachment */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
            
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Ask Cousin...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )
            
            if (text.isNotBlank()) {
                Button(
                    onClick = {
                        onSendMessage(text)
                        text = ""
                    },
                    enabled = !isLoading,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("➤")
                }
            } else {
                IconButton(onClick = { 
                    if (isListening) {
                        toneGen?.startTone(ToneGenerator.TONE_PROP_ACK)
                    } else {
                        toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP)
                    }
                    onToggleListening() 
                }) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = "Mic"
                    )
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(role = MessageRole.USER, content = "Hi, I need a flight to Mumbai."),
                    ChatMessage(role = MessageRole.ASSISTANT, content = "Sure, I can help with that. When are you planning to travel?")
                ),
                isLoading = false
            ),
            uiEventFlow = kotlinx.coroutines.flow.emptyFlow(),
            onSendMessage = {},
            onOpenDrawer = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyChatScreenPreview() {
    MaterialTheme {
        ChatScreen(
            uiState = ChatUiState(messages = emptyList(), isLoading = false),
            uiEventFlow = kotlinx.coroutines.flow.emptyFlow(),
            onSendMessage = {},
            onOpenDrawer = {}
        )
    }
}
