package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShelfLifeViewModel

@Composable
fun ChatScreen(
    viewModel: ShelfLifeViewModel,
    onBack: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.isDark
    val chatHistory by viewModel.chatHistory.collectAsState()
    val loading by viewModel.chatLoading.collectAsState()
    val selectedRecipe by viewModel.selectedRecipe.collectAsState()

    var activeInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Helper options
    val templates = if (selectedRecipe != null) {
        listOf(
            "Can I make this cheaper?",
            "What can replace the main protein?",
            "How do I make this vegetarian?",
            "Which ingredients can I skip?"
        )
    } else {
        listOf(
            "Substitute eggs in baking?",
            "How to use expiring produce?",
            "Fast recipe with rice and chicken?",
            "Substitutions for soy sauce?"
        )
    }

    // Auto scroll to bottom when history length changes
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Kitchen AI Assistant",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Powered by ShelfLife AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) MaterialTheme.colorScheme.primary else SageGreen,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear conversation", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Chat canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) {
                        Color(0xFF20271F)
                    } else {
                        Color(0xFFFFFCF6)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDark) {
                                    Color(0xFF2E4A38)
                                } else {
                                    Color(0xFFDCECDD)
                                }
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = selectedRecipe?.let { "Ask about ${it.name}, substitutions, or changes." }
                                    ?: "Ask anything about substitutes, meal prep, or reducing kitchen waste!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 18.dp,
                            top = 18.dp,
                            end = 18.dp,
                            bottom = 18.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatHistory, key = { it.id }) { message ->
                            ChatBubble(
                                message = message.text,
                                isUser = message.isUser,
                                recipeUpdateSummary = message.recipeUpdate?.summary,
                                isApplied = message.isApplied,
                                onApplyRecipeUpdate = message.recipeUpdate?.let {
                                    { viewModel.applyRecipeUpdate(message.id) }
                                }
                            )
                        }

                        if (loading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "Kitchen AI is cooking up a reply...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Helper Templates Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val chipBg = if (isDark) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                } else {
                    Color.White
                }

                templates.forEach { temp ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipBg)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                CircleShape
                            )
                            .clickable { viewModel.sendChatMessage(temp) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = temp,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Input Block
            val inputRowBg = if (isDark) Color(0xFF2D382F) else Color.White

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = inputRowBg),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = activeInput,
                        onValueChange = { activeInput = it },
                        placeholder = {
                            Text(
                                selectedRecipe?.let { "Ask about this recipe" }
                                    ?: "Ask Kitchen AI about pantry planning..."
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1F241F) else Color(0xFFFFFCF6),
                            unfocusedContainerColor = if (isDark) Color(0xFF1F241F) else Color(0xFFFFFCF6),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("ai_assistant_input")
                    )

                    val canSend = activeInput.isNotBlank() && !loading
                    IconButton(
                        onClick = {
                            if (activeInput.isNotBlank()) {
                                viewModel.sendChatMessage(activeInput)
                                activeInput = ""
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) {
                                    MaterialTheme.colorScheme.primary
                                } else if (isDark) {
                                    Color(0xFF455246)
                                } else {
                                    Color(0xFFEDE7DF)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Send,
                            contentDescription = "Send",
                            tint = if (canSend) {
                                Color.White
                            } else if (isDark) {
                                Color(0xFFD7E8D9)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: String,
    isUser: Boolean,
    recipeUpdateSummary: String? = null,
    isApplied: Boolean = false,
    onApplyRecipeUpdate: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.isDark

    val bubbleBgColor = if (isUser) {
        if (isDark) Color(0xFF7EBC96) else SageGreen
    } else {
        if (isDark) Color(0xFF3B463D) else Color.White
    }
    val bubbleTextColor = if (isUser) {
        if (isDark) Color(0xFF102417) else Color.White
    } else {
        if (isDark) Color(0xFFF4F7F0) else MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                // AI Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👩🏼‍🍳", fontSize = 16.sp)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Bubble body
            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = bubbleBgColor
                    ),
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 20.dp else 8.dp,
                        topEnd = if (isUser) 8.dp else 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    )
                ) {
                    if (isUser) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = bubbleTextColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        MarkdownText(
                            text = message,
                            color = bubbleTextColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!isUser && recipeUpdateSummary != null && onApplyRecipeUpdate != null) {
                    Text(
                        text = recipeUpdateSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Button(
                        onClick = onApplyRecipeUpdate,
                        enabled = !isApplied,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isApplied) Icons.Default.Check else Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isApplied) "Applied to recipe" else "Use these ingredients",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, color: Color, modifier: Modifier = Modifier) {
    val annotatedString = buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            
            // Handle Headers (###)
            val isHeader = currentLine.startsWith("###")
            if (isHeader) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                    append(currentLine.replace("###", "").trim())
                }
            } else {
                // Handle Lists (-)
                if (currentLine.trim().startsWith("-")) {
                    append("• ")
                    currentLine = currentLine.trim().substring(1).trim()
                }
                
                // Handle Bold (**)
                var lastIndex = 0
                val boldRegex = Regex("""\*\*(.*?)\*\*""")
                val matches = boldRegex.findAll(currentLine)
                
                matches.forEach { match ->
                    append(currentLine.substring(lastIndex, match.range.first))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1])
                    }
                    lastIndex = match.range.last + 1
                }
                append(currentLine.substring(lastIndex))
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        lineHeight = 22.sp,
        modifier = modifier
    )
}
