package com.runningcoach.v2.presentation.screen.aicoach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runningcoach.v2.presentation.components.CompactButton
import com.runningcoach.v2.presentation.components.icons.SendIcon
import com.runningcoach.v2.presentation.theme.AppColors

@Composable
fun AICoachScreen(
    viewModel: AICoachViewModel? = null, // Temporary nullable until we set up DI
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    
    // Use ViewModel if available, otherwise fall back to local state
    val messages by if (viewModel != null) {
        viewModel.messages.collectAsState()
    } else {
        remember { 
            mutableStateOf(
                listOf(
                    ChatMessage(
                        text = "Hi! I'm your Fitness Coach AI Agent, trained on your personal health and fitness data. I can help you with training plans, nutrition advice, recovery strategies, and answer any questions about your running journey. What would you like to know?",
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            )
        }
    }
    
    val isLoading by if (viewModel != null) {
        viewModel.isLoading.collectAsState()
    } else {
        remember { mutableStateOf(false) }
    }
    
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppColors.Surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp).padding(top = 40.dp)
            ) {
                Text(
                    text = "Fitness Coach AI",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnBackground
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Your personal AI fitness coach trained on your data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.Neutral400,
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip()
                }
            }
        }

        // Error banner (e.g., missing API key)
        val errorText by if (viewModel != null) viewModel.errorMessage.collectAsState() else remember { mutableStateOf<String?>(null) }
        if (!errorText.isNullOrBlank()) {
            Surface(
                color = AppColors.Neutral800,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = errorText ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }

        // Chat Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            items(messages) { message ->
                ChatBubble(
                    message = message,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Loading indicator
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // AI Coach Avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(AppColors.Primary, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.OnPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            color = AppColors.CardBackground,
                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = AppColors.Primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thinking...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.OnSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppColors.Surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { 
                        Text(
                            "Ask about training, nutrition, recovery...",
                            color = AppColors.Neutral400
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                CompactButton(
                    text = "",
                    onClick = {
                        if (messageText.isNotBlank()) {
                            if (viewModel != null) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            } else {
                                // Fallback to local state for demo
                                val userMessage = ChatMessage(
                                    text = messageText,
                                    isFromUser = true,
                                    timestamp = System.currentTimeMillis()
                                )
                                // This won't work with collectAsState, but it's just for demo
                                messageText = ""
                            }
                        }
                    },
                    enabled = messageText.isNotBlank(),
                    backgroundColor = if (messageText.isNotBlank()) AppColors.Primary else AppColors.Neutral700,
                    contentColor = AppColors.OnPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    SendIcon(
                        modifier = Modifier.size(20.dp),
                        tint = AppColors.OnPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderChip() {
    val provider = com.runningcoach.v2.BuildConfig.AI_PROVIDER.ifBlank { "GEMINI" }
    Surface(
        color = AppColors.Primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (provider.equals("GPT", ignoreCase = true)) "GPT" else "Gemini",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            // AI Coach Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AppColors.Primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.OnPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Surface(
            color = if (message.isFromUser) AppColors.Primary else AppColors.CardBackground,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isFromUser) AppColors.OnPrimary else AppColors.OnSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            
            // User Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(AppColors.Neutral700, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.OnSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



// Sample AI responses - in real app this would call the Fitness Coach AI Agent
private fun generateAIResponse(userMessage: String): String {
    val responses = mapOf(
        "training" to "Based on your current fitness level and running history, I recommend focusing on building your aerobic base with easy runs at 70-80% of your weekly volume. Your recent data shows good consistency, so we can start incorporating one tempo run per week.",
        
        "nutrition" to "For optimal running performance, aim for 1.4-1.8g of carbohydrates per lb of body weight on training days. Based on your workout intensity data, you'll want to fuel 2-3 hours before longer runs with easily digestible carbs.",
        
        "recovery" to "Your recent training load suggests you need 1-2 full rest days per week. I notice your sleep data shows some inconsistency - aim for 7-9 hours nightly for optimal recovery. Consider active recovery like gentle yoga on rest days.",
        
        "pace" to "Looking at your recent runs, your easy pace should be around 30-60 seconds slower than your 5K race pace. This helps build your aerobic engine without accumulating too much fatigue.",
        
        "plan" to "I've analyzed your fitness data and can create a personalized training plan. What's your target race distance and timeline? I'll factor in your current weekly mileage and progression rate."
    )
    
    val lowerMessage = userMessage.lowercase()
    val matchingKey = responses.keys.find { lowerMessage.contains(it) }
    
    return matchingKey?.let { responses[it] } 
        ?: "I'm here to help with your fitness journey! I can provide personalized advice on training plans, nutrition strategies, recovery protocols, and performance optimization based on your health and fitness data. What specific aspect would you like to focus on?"
}
