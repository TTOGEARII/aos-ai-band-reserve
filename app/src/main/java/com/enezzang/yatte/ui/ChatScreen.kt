package com.enezzang.yatte.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.enezzang.yatte.data.MAX_MESSAGE_LENGTH
import com.enezzang.yatte.data.ToolCallTrace
import com.enezzang.yatte.ui.theme.EnezzangYatteTheme

/** 웹 index.vue의 TOOL_LABELS와 같은 표기 — 두 화면을 오가도 같은 말로 보이게. */
private val TOOL_LABELS = mapOf(
    "notion_search" to "노션 검색",
    "notion_get_database" to "DB 스키마 조회",
    "notion_query_database" to "DB 조회",
    "notion_create_page" to "페이지 생성",
    "notion_update_page" to "페이지 수정",
    "notion_get_page" to "페이지 조회",
    "notion_get_blocks" to "본문 조회",
    "notion_append_blocks" to "본문 작성",
    "search_studios" to "합주실 검색",
    "get_available_slots" to "빈 시간 조회",
    "get_reservation_url" to "예약 링크",
    "search_youtube" to "유튜브 검색",
    "search_sheet_music" to "악보 검색",
)

/** 빈 대화에서 바로 눌러 시작하는 예시 질문 — 이 앱의 주특기(합주실)부터. */
private val SUGGESTIONS = listOf(
    "홍대 근처 합주실 찾아줘",
    "이번 주 토요일 20시~22시에 비는 합주실 있어?",
    "그라운드 합정 내일 빈 시간 알려줘",
    "방금 예약한 합주 일정 노션 캘린더에 등록해줘",
)

/**
 * 메인 채팅. 목록이 위, 입력바가 아래 고정.
 * 에이전트 답변 속 URL은 본문 링크로도, 아래 버튼(네이버 예약 열기 등)으로도 열 수 있다 —
 * 예약 링크가 이 앱의 목적지라 본문 틈에 숨기지 않는다.
 */
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isSending: Boolean,
    needsGeminiKey: Boolean,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 새 턴이 붙거나 답변 대기 표시가 생기면 맨 아래로 따라간다.
    LaunchedEffect(messages.size, isSending) {
        val count = messages.size + if (isSending) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = EnezzangYatteTheme.gutter,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatHero(
                        needsGeminiKey = needsGeminiKey,
                        onPickSuggestion = { input = it },
                        onOpenSettings = onOpenSettings,
                    )
                }
            } else if (needsGeminiKey) {
                item { GeminiKeyCard(onOpenSettings) }
            }

            itemsIndexed(messages) { _, message ->
                when (message.role) {
                    Role.User -> UserBubble(message.content)
                    Role.Assistant -> AssistantBubble(message)
                }
            }

            if (isSending) {
                item { ThinkingBubble() }
            }
        }

        Hairline()
        InputBar(
            value = input,
            onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) input = it },
            isSending = isSending,
            onSend = {
                val text = input
                input = ""
                onSend(text)
            },
            onCancel = onCancel,
        )
    }
}

/** 첫 화면. 뭘 할 수 있는지 + 바로 눌러볼 질문. */
@Composable
private fun EmptyChatHero(
    needsGeminiKey: Boolean,
    onPickSuggestion: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tokens = EnezzangYatteTheme.tokens
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("오늘 어디서 맞출까요?", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "서울 합주실의 실시간 빈 시간을 찾아 예약 링크를 드려요.\n" +
                "예약을 마치면 노션 캘린더 기록, 곡·커버 영상·악보 검색도 돼요.",
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.muted,
        )

        if (needsGeminiKey) {
            GeminiKeyCard(onOpenSettings)
        }

        Text(
            text = "이렇게 물어보세요",
            style = MaterialTheme.typography.labelMedium,
            color = tokens.muted,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SUGGESTIONS.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onPickSuggestion(suggestion) },
                    label = {
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    },
                )
            }
        }
    }
}

/** Gemini 키가 없으면 에이전트가 아예 안 돈다 — 첫 걸음을 카드로 안내한다. */
@Composable
private fun GeminiKeyCard(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "시작하려면 Gemini API 키가 필요해요",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            "aistudio.google.com에서 무료로 발급받아 설정에 넣으면 바로 쓸 수 있어요. " +
                "노션·유튜브 키는 선택이에요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        FilledTonalButton(onClick = onOpenSettings) {
            Text("설정에서 키 입력", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun UserBubble(content: String) {
    val tokens = EnezzangYatteTheme.tokens
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.onBubbleUser,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(
                    tokens.bubbleUser,
                    RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun AssistantBubble(message: ChatMessage) {
    val tokens = EnezzangYatteTheme.tokens
    val uriHandler = LocalUriHandler.current
    val blocks = parseReply(message.content)
    val chips = extractLinkChips(message.content)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                tokens.bubbleAssistant,
                RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp),
            )
            .border(
                1.dp,
                tokens.hairline,
                RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 에이전트가 어떤 도구를 거쳤는지. 실패한 도구가 있으면 답을 읽을 때 감안하게 된다.
        if (message.toolCalls.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                message.toolCalls.forEach { trace -> ToolTraceTag(trace) }
            }
        }

        // 복사해서 밴드 단톡에 붙여넣는 게 흔한 흐름이라 본문은 선택 가능하게 둔다.
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                blocks.forEach { block ->
                    when (block) {
                        is ReplyBlock.Heading -> Text(
                            text = annotate(block.spans),
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.onBubbleAssistant,
                        )

                        is ReplyBlock.Bullet -> Row {
                            Text(
                                "•  ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.muted,
                            )
                            Text(
                                text = annotate(block.spans),
                                style = MaterialTheme.typography.bodyLarge,
                                color = tokens.onBubbleAssistant,
                            )
                        }

                        is ReplyBlock.Paragraph -> Text(
                            text = annotate(block.spans),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tokens.onBubbleAssistant,
                        )
                    }
                }
            }
        }

        if (chips.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { chip ->
                    when (chip.kind) {
                        // 예약 링크는 이 앱의 목적지 — 가장 눈에 띄는 버튼으로.
                        LinkKind.NaverBooking -> FilledTonalButton(
                            onClick = { uriHandler.openUri(chip.url) },
                        ) { Text(chip.label, style = MaterialTheme.typography.labelLarge) }

                        else -> OutlinedButton(
                            onClick = { uriHandler.openUri(chip.url) },
                        ) {
                            Text(
                                chip.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTraceTag(trace: ToolCallTrace) {
    val tokens = EnezzangYatteTheme.tokens
    val color = if (trace.ok) tokens.traceOk else tokens.traceFail
    Text(
        text = (if (trace.ok) "✓ " else "✗ ") + (TOOL_LABELS[trace.tool] ?: trace.tool),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

/** 답변 대기 말풍선. 도구 호출이 겹치면 수십 초 걸릴 수 있어 "기다리는 중"이 보여야 한다. */
@Composable
private fun ThinkingBubble() {
    val tokens = EnezzangYatteTheme.tokens
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .background(
                tokens.bubbleAssistant,
                RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp),
            )
            .border(1.dp, tokens.hairline, RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(
            "빈 시간 찾는 중… 도구를 여러 번 부르면 조금 걸려요.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.muted,
        )
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val tokens = EnezzangYatteTheme.tokens
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.inputBg)
            .padding(horizontal = EnezzangYatteTheme.gutter, vertical = 10.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "예: 홍대 합주실 토요일 저녁 빈 시간",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tokens.muted,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = tokens.hairline,
            ),
            supportingText = if (value.length > MAX_MESSAGE_LENGTH - 500) {
                { Text("${value.length} / $MAX_MESSAGE_LENGTH") }
            } else {
                null
            },
            modifier = Modifier.weight(1f),
        )

        if (isSending) {
            // 보내는 중엔 전송이 "중지"로 바뀐다 — 긴 검색을 기다릴지 말지는 사용자가 정한다.
            Button(
                onClick = onCancel,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(56.dp),
            ) {
                Text("중지", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Button(
                onClick = onSend,
                enabled = value.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(56.dp),
            ) {
                Text("보내기", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** 인라인 스팬 → AnnotatedString. 링크는 밑줄 + primary 색, 탭하면 바로 열린다. */
@Composable
private fun annotate(spans: List<InlineSpan>): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        spans.forEach { span ->
            when (span) {
                is InlineSpan.Plain -> append(span.text)
                is InlineSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(span.text)
                }

                is InlineSpan.Link -> withLink(
                    LinkAnnotation.Url(
                        span.url,
                        TextLinkStyles(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                    ),
                ) { append(span.text) }
            }
        }
    }
}
