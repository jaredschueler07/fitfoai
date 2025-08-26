package com.runningcoach.v2.presentation.screen.aicoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runningcoach.v2.data.local.entity.AIConversationEntity
import com.runningcoach.v2.data.service.FitnessCoachAgent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AICoachViewModel(
    private val fitnessCoachAgent: FitnessCoachAgent
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hi! I'm your Fitness Coach AI Agent, trained on your personal health and fitness data. I can help you with training plans, nutrition advice, recovery strategies, and answer any questions about your running journey. What would you like to know?",
                isFromUser = false,
                timestamp = System.currentTimeMillis()
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        // Load conversation history
        loadConversationHistory()
        
        // Observe AI processing state
        viewModelScope.launch {
            fitnessCoachAgent.isProcessing.collect { isProcessing ->
                _isLoading.value = isProcessing
            }
        }
    }
    
    fun sendMessage(message: String, includeVoice: Boolean = false) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            try {
                // Add user message immediately
                val userMessage = ChatMessage(
                    text = message,
                    isFromUser = true,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + userMessage
                
                // Clear any previous errors
                _errorMessage.value = null
                
                // Send to AI agent
                val result = fitnessCoachAgent.sendMessage(message, includeVoice)
                
                if (result.isSuccess) {
                    val aiResponse = result.getOrThrow()
                    val aiMessage = ChatMessage(
                        text = aiResponse,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + aiMessage
                } else {
                    val errorMsg = "I'm having trouble connecting right now. Please try again in a moment."
                    val errorMessage = ChatMessage(
                        text = errorMsg,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + errorMessage
                    _errorMessage.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                val errorMsg = "Something went wrong. Please try again."
                val errorMessage = ChatMessage(
                    text = errorMsg,
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + errorMessage
                _errorMessage.value = e.message
            }
        }
    }
    
    fun generateTrainingPlan(
        goals: String,
        fitnessLevel: String,
        targetRace: String?,
        timeframe: String
    ) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                
                val result = fitnessCoachAgent.generateTrainingPlan(goals, fitnessLevel, targetRace, timeframe)
                
                if (result.isSuccess) {
                    val plan = result.getOrThrow()
                    val planMessage = ChatMessage(
                        text = "Here's your personalized training plan:\n\n$plan",
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + planMessage
                } else {
                    val errorMsg = "I couldn't generate a training plan right now. Please try again later."
                    val errorMessage = ChatMessage(
                        text = errorMsg,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _messages.value = _messages.value + errorMessage
                    _errorMessage.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    fun requestQuickAdvice(topic: String) {
        val quickRequests = mapOf(
            "training" to "Can you give me some quick training advice based on my recent runs?",
            "nutrition" to "What should I eat before and after my runs?",
            "recovery" to "How can I improve my recovery between training sessions?",
            "motivation" to "I need some motivation to keep up with my training.",
            "injury_prevention" to "What can I do to prevent running injuries?"
        )
        
        quickRequests[topic]?.let { message ->
            sendMessage(message)
        }
    }
    
    private fun loadConversationHistory() {
        viewModelScope.launch {
            try {
                fitnessCoachAgent.getConversationHistory().collect { history ->
                    if (history.isNotEmpty()) {
                        val chatMessages = history.map { entity ->
                            ChatMessage(
                                text = entity.message,
                                isFromUser = entity.isFromUser,
                                timestamp = entity.timestamp
                            )
                        }
                        
                        // Keep the welcome message at the top if no history exists
                        if (_messages.value.size == 1) {
                            _messages.value = listOf(_messages.value.first()) + chatMessages
                        } else {
                            _messages.value = chatMessages
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load conversation history"
            }
        }
    }
    
    fun clearConversation() {
        viewModelScope.launch {
            try {
                fitnessCoachAgent.clearConversationHistory()
                _messages.value = listOf(
                    ChatMessage(
                        text = "Hi! I'm your Fitness Coach AI Agent. How can I help you today?",
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear conversation"
            }
        }
    }
    
    fun stopAudio() {
        fitnessCoachAgent.stopCurrentAudio()
    }
    
    fun isPlayingAudio(): Boolean {
        return fitnessCoachAgent.isPlayingAudio()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        fitnessCoachAgent.stopCurrentAudio()
    }
}

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long
)
