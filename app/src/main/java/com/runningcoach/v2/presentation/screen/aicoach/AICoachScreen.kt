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
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { 
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
                
                Text(
                    text = "Your personal AI fitness coach trained on your data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Neutral400,
                    modifier = Modifier.padding(top = 4.dp)
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
                            // Add user message
                            messages = messages + ChatMessage(
                                text = messageText,
                                isFromUser = true,
                                timestamp = System.currentTimeMillis()
                            )
                            
                            // Simulate AI response (in real app, this would call the AI service)
                            val userMessage = messageText
                            messageText = ""
                            
                            // Add AI response after a short delay
                            messages = messages + ChatMessage(
                                text = generateAIResponse(userMessage),
                                isFromUser = false,
                                timestamp = System.currentTimeMillis()
                            )
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

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long
)

// Sample AI responses - in real app this would call the Fitness Coach AI Agent
private fun generateAIResponse(userMessage: String): String {
    val responses = mapOf(
        "training" to "Based on your current fitness level and running history, I recommend focusing on building your aerobic base with easy runs at 70-80% of your weekly volume. Your recent data shows good consistency, so we can start incorporating one tempo run per week.",
        
        "nutrition" to "For optimal running performance, aim for 3-4g of carbohydrates per kg of body weight on training days. Based on your workout intensity data, you'll want to fuel 2-3 hours before longer runs with easily digestible carbs.",
        
        "recovery" to "Your recent training load suggests you need 1-2 full rest days per week. I notice your sleep data shows some inconsistency - aim for 7-9 hours nightly for optimal recovery. Consider active recovery like gentle yoga on rest days.",
        
        "pace" to "Looking at your recent runs, your easy pace should be around 30-60 seconds slower than your 5K race pace. This helps build your aerobic engine without accumulating too much fatigue.",
        
        "plan" to "I've analyzed your fitness data and can create a personalized training plan. What's your target race distance and timeline? I'll factor in your current weekly mileage and progression rate."
    )
    
    val lowerMessage = userMessage.lowercase()
    val matchingKey = responses.keys.find { lowerMessage.contains(it) }
    
    return matchingKey?.let { responses[it] } 
        ?: "I'm here to help with your fitness journey! I can provide personalized advice on training plans, nutrition strategies, recovery protocols, and performance optimization based on your health and fitness data. What specific aspect would you like to focus on?"
}
