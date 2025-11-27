package io.github.howshous.ui.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    // Split by lines to handle bullet lists
    val lines = text.split("\n")
    
    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    // Bullet point
                    val content = trimmed.substring(2)
                    Text(
                        text = "• $content",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("**") && trimmed.endsWith("**") -> {
                    // Bold line
                    val content = trimmed.removePrefix("**").removeSuffix("**")
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                trimmed.isEmpty() -> {
                    // Empty line for spacing
                    Text("", modifier = Modifier.padding(vertical = 4.dp))
                }
                else -> {
                    // Regular text with inline bold support
                    val annotated = buildAnnotatedString {
                        var i = 0
                        while (i < trimmed.length) {
                            if (trimmed.startsWith("**", i)) {
                                val closing = trimmed.indexOf("**", startIndex = i + 2)
                                if (closing != -1) {
                                    val content = trimmed.substring(i + 2, closing)
                                    withStyle(
                                        MaterialTheme.typography.bodyMedium.toSpanStyle()
                                            .copy(fontWeight = FontWeight.Bold)
                                    ) {
                                        append(content)
                                    }
                                    i = closing + 2
                                    continue
                                }
                            }
                            append(trimmed[i])
                            i++
                        }
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

