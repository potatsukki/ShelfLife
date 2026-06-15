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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Go back")
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Kitchen AI Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Powered by ShelfLife AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) MaterialTheme.colorScheme.primary else SageGreen,
                        fontSize = 10.sp
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
                .imePadding() // slide up neatly when soft keyboard draws
        ) {
            // Quick instructions alert banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Text(
                        text = selectedRecipe?.let { "Ask about ${it.name}, substitutions, or changes." }
                            ?: "Ask anything about substitutes, meal prep, or reducing kitchen waste!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Message Thread viewport
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
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
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Kitchen AI is cooking up a reply...", style = MaterialTheme.typography.bodySmall, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText)
                        }
                    }
                }
            }

            // Floating Helper Templates Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isDark = MaterialTheme.colorScheme.isDark
                val chipBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

                templates.forEach { temp ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipBg)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { viewModel.sendChatMessage(temp) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(temp, style = MaterialTheme.typography.labelSmall, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SoftGrayText)
                    }
                }
            }

            // Bottom Input Block
            val isDark = MaterialTheme.colorScheme.isDark
            val inputRowBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(inputRowBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = activeInput,
                    onValueChange = { activeInput = it },
                    placeholder = {
                        Text(
                            selectedRecipe?.let { "Ask about this recipe or ingredient substitutions" }
                                ?: "Ask Kitchen AI about pantry planning..."
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_assistant_input")
                )

                IconButton(
                    onClick = {
                        if (activeInput.isNotBlank()) {
                            viewModel.sendChatMessage(activeInput)
                            activeInput = ""
                        }
                    },
                    enabled = activeInput.isNotBlank() && !loading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (activeInput.isNotBlank() && !loading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send",
                        tint = if (activeInput.isNotBlank() && !loading) Color.White else SoftGrayText
                    )
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
        if (isDark) MaterialTheme.colorScheme.primaryContainer else DeepWalnutText
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    }
    val bubbleTextColor = if (isUser) {
        if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
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
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
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
