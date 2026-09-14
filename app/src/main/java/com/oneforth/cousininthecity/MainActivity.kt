package com.oneforth.cousininthecity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.oneforth.cousininthecity.ui.screens.ChatScreen
import com.oneforth.cousininthecity.ui.screens.DrawerContent
import com.oneforth.cousininthecity.ui.theme.*
import com.oneforth.cousininthecity.ui.viewmodels.ChatViewModel
import com.oneforth.cousininthecity.ui.viewmodels.ThreadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CousinInTheCityAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CousinApp()
                }
            }
        }
    }
}

@Composable
fun CousinApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val threadViewModel: ThreadViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()

    val threadUiState by threadViewModel.uiState.collectAsStateWithLifecycle()
    val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
        threadViewModel.loadThreads()
        chatViewModel.loadThread("1")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                threadUiState = threadUiState,
                onNewChat = {
                    threadViewModel.createNewThread("Relocation Plan- ${threadUiState.threads.size+1}") { newThreadId ->
                        chatViewModel.loadThread(newThreadId)
                        scope.launch { drawerState.close() }
                    }
                },
                onThreadSelected = { threadId ->
                    chatViewModel.loadThread(threadId)
                    scope.launch { drawerState.close() }
                },
                onTogglePin = { threadId, isPinned ->
                    threadViewModel.togglePin(threadId, isPinned)
                },
                onDeleteThread = { threadId ->
                    threadViewModel.deleteThread(threadId)
                },
                modifier = Modifier.width(320.dp)
            )
        }
    ) {
        NavHost(navController = navController, startDestination = "chat") {
            composable("chat") {
                ChatScreen(
                    uiState = chatUiState,
                    uiEventFlow = chatViewModel.uiEvent,
                    onSendMessage = { prompt -> chatViewModel.sendMessage(prompt) },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onToggleListening = { chatViewModel.toggleListening() }
                )
            }
        }
    }
}