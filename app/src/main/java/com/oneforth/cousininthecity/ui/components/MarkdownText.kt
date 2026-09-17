package com.oneforth.cousininthecity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneforth.cousininthecity.ui.theme.CousinInTheCityAndroidTheme

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val blocks = parseMarkdownBlocks(markdown)

    SelectionContainer(modifier = modifier) {
        Column {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Header -> {
                        val fontSize = when (block.level) {
                            1 -> 22.sp
                            2 -> 19.sp
                            else -> 17.sp
                        }
                        Text(
                            text = parseInlineMarkdown(block.text, color),
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    is MarkdownBlock.CodeBlock -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    color = Color(0xFF1E1E1E),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = block.code,
                                color = Color(0xFFD4D4D4),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    }

                    is MarkdownBlock.BulletItem -> {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                text = "• ",
                                color = color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = parseInlineMarkdown(block.text, color),
                                color = color,
                                fontSize = 15.sp
                            )
                        }
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = parseInlineMarkdown(block.text, color),
                            color = color,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var inCodeBlock = false
    val codeBuilder = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeBuilder.toString().trimEnd()))
                codeBuilder.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            if (codeBuilder.isNotEmpty()) codeBuilder.append("\n")
            codeBuilder.append(line)
            continue
        }

        if (trimmed.isEmpty()) continue

        when {
            trimmed.startsWith("# ") -> blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
            trimmed.startsWith("## ") -> blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
            trimmed.startsWith("### ") -> blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
            trimmed.startsWith("* ") -> blocks.add(MarkdownBlock.BulletItem(trimmed.removePrefix("* ").trim()))
            trimmed.startsWith("- ") -> blocks.add(MarkdownBlock.BulletItem(trimmed.removePrefix("- ").trim()))
            else -> blocks.add(MarkdownBlock.Paragraph(line))
        }
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeBuilder.toString().trimEnd()))
    }

    return blocks
}

private fun parseInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            when {
                i + 1 < len && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33888888),
                                fontSize = 14.sp
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MarkdownTextPreview() {
    CousinInTheCityAndroidTheme {
        MarkdownText(
            markdown = """
                # Markdown Preview
                ## Features
                This is a **paragraph** with *italic* and `inline code`.

                * First bullet point
                * Second bullet point

                ```kotlin
                val message = "Hello, World!"
                println(message)
                ```
            """.trimIndent()
        )
    }
}
