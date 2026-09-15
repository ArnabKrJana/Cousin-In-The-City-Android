package com.oneforth.cousininthecity.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.oneforth.cousininthecity.domain.model.ChatMessage
import com.oneforth.cousininthecity.ui.components.ChatShimmerBubble
import com.oneforth.cousininthecity.ui.components.EmptyChatState
import com.oneforth.cousininthecity.ui.components.MessageBubble
import com.oneforth.cousininthecity.ui.viewmodels.ChatUiState
import com.oneforth.cousininthecity.ui.viewmodels.UiEvent
import com.oneforth.cousininthecity.util.NativeIntentUtils
import com.oneforth.cousininthecity.util.VoiceToTextParser
import com.oneforth.cousininthecity.util.VoiceToTextParserState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    chatHistoryFlow: Flow<PagingData<ChatMessage>>,
    uiEventFlow: Flow<UiEvent>,
    onSendMessage: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val messages = chatHistoryFlow.collectAsLazyPagingItems()

    val voiceParser = if (isPreview) null else remember { VoiceToTextParser(context) }
    val voiceStateFlow = remember(voiceParser) {
        voiceParser?.state ?: MutableStateFlow(VoiceToTextParserState())
    }
    val voiceState by voiceStateFlow.collectAsState()

    DisposableEffect(voiceParser) {
        onDispose {
            voiceParser?.destroy()
        }
    }

    LaunchedEffect(voiceState.error) {
        voiceState.error?.let {
            snackbarHostState.showSnackbar(it)
            voiceParser?.reset()
        }
    }

    LaunchedEffect(messages.itemCount) {
        if (messages.itemCount > 0) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(uiEventFlow) {
        uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is UiEvent.JarvisAddCalendar -> {
                    NativeIntentUtils.addCalendarEvent(
                        context,
                        event.title,
                        event.date,
                        event.time
                    )
                }

                is UiEvent.JarvisOpenMap -> {
                    NativeIntentUtils.openGoogleMaps(context, event.locationQuery)
                }

                is UiEvent.JarvisSaveNote -> {
                    NativeIntentUtils.saveNote(context, event.title, event.note)
                }
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF121318), Color(0xFF1E222D))
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
                            Text(
                                "Assistant",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                    onStartListening = { voiceParser?.startListening() },
                    onStopListening = { voiceParser?.stopListening() },
                    onResetVoice = { voiceParser?.reset() },
                    isSpeaking = voiceState.isSpeaking,
                    spokenText = voiceState.spokenText
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Crossfade(
                    targetState = messages.itemCount == 0,
                    label = "ChatStateAnimation"
                ) { isEmpty ->
                    if (isEmpty) {
                        EmptyChatState()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            reverseLayout = true
                        ) {
                            if (uiState.isChatLoading) {
                                item {
                                    ChatShimmerBubble()
                                }
                            }
                            items(
                                count = messages.itemCount
                            ) { index ->
                                val message = messages[index]
                                if (message != null) {
                                    MessageBubble(message)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputField(
    onSendMessage: (String) -> Unit,
    isLoading: Boolean,
    onStartListening: () -> Unit = {},
    onStopListening: () -> Unit = {},
    onResetVoice: () -> Unit = {},
    isSpeaking: Boolean = false,
    spokenText: String = ""
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onStartListening()
            }
        }
    )

    LaunchedEffect(spokenText) {
        if (spokenText.isNotBlank()) {
            text = spokenText
        }
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
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(if (isSpeaking) "Listening..." else "Ask Cousin...") },
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
                        onResetVoice()
                    },
                    enabled = !isLoading,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("➤")
                }
            } else {
                IconButton(
                    onClick = {
                        if (isSpeaking) {
                            onStopListening()
                        } else {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                onStartListening()
                            } else {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Mic,
                        tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
            uiState = ChatUiState(isLoading = false),
            chatHistoryFlow = emptyFlow(),
            uiEventFlow = emptyFlow(),
            onSendMessage = {},
            onOpenDrawer = {}
        )
    }
}
