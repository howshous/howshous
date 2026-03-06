package io.github.howshous.ui.screens.main_tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.NearWhite
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.TenantGreen
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.viewmodels.MessageAuthor
import io.github.howshous.ui.viewmodels.TenantAIHelperViewModel
import io.github.howshous.ui.util.MarkdownText
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun TenantAIHelperScreen(nav: NavController, tenantAIHelperViewModel: TenantAIHelperViewModel = viewModel()) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val messages by tenantAIHelperViewModel.messages.collectAsState()
    val isThinking by tenantAIHelperViewModel.isThinking.collectAsState()
    val listingCache by tenantAIHelperViewModel.listingCache.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentPrompt by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Initialize chat history when user ID is available
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            tenantAIHelperViewModel.initializeChat(uid)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceLight)
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DebouncedIconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("AI Boarding House Guide", style = MaterialTheme.typography.titleMedium)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isTenant = message.author == MessageAuthor.TENANT
                    Surface(
                        color = if (isTenant) TenantGreen else NearWhite,
                        contentColor = if (isTenant) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isTenant) "You" else "AI Assistant",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            if (isTenant) {
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                val parsed = remember(message.text) { parseAiMessage(message.text) }
                                MarkdownText(text = parsed.cleanedText)
                                if (parsed.listingIds.isNotEmpty()) {
                                    LaunchedEffect(parsed.listingIds) {
                                        tenantAIHelperViewModel.ensureListings(parsed.listingIds)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    ListingRecommendations(
                                        recommendations = parsed.recommendations,
                                        listingIds = parsed.listingIds,
                                        listingCache = listingCache,
                                        onListingClick = { id -> nav.navigate("listing/$id") }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    if (isThinking) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLight)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = currentPrompt,
                    onValueChange = { currentPrompt = it },
                    placeholder = { Text("Describe your budget, location, or amenities...") },
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (currentPrompt.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Tell me what you're looking for so I can help.")
                            }
                        } else {
                            tenantAIHelperViewModel.sendMessage(currentPrompt)
                            currentPrompt = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isThinking,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TenantGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isThinking) "Thinking..." else "Ask the AI")
                }
            }
        }
    }
}

private data class ParsedAiMessage(
    val cleanedText: String,
    val listingIds: List<String>,
    val recommendations: List<AiRecommendation>
)

private fun parseAiMessage(text: String): ParsedAiMessage {
    val jsonBlockRegex = Regex("```json\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
    val tagRegex = Regex("\\[\\[LISTING:([^\\]]+)\\]\\]")
    val idLineRegex = Regex("(?im)^\\s*(?:listing\\s*id|id)\\s*[:#]\\s*([A-Za-z0-9_-]+)\\s*$")

    val ids = mutableListOf<String>()
    val recommendations = mutableListOf<AiRecommendation>()
    val spansToRemove = mutableListOf<IntRange>()

    jsonBlockRegex.findAll(text).forEach { match ->
        val jsonText = match.groupValues.getOrNull(1)?.trim().orEmpty()
        parseRecommendationsFromJson(jsonText)?.let { recs ->
            recommendations.addAll(recs)
            recs.forEach { rec ->
                if (rec.id.isNotBlank()) ids.add(rec.id)
            }
        }
        spansToRemove.add(match.range)
    }

    if (recommendations.isEmpty()) {
        extractJsonCandidates(text).forEach { candidate ->
            val recs = parseRecommendationsFromJson(candidate.text).orEmpty()
            if (recs.isNotEmpty()) {
                recommendations.addAll(recs)
                recs.forEach { rec ->
                    if (rec.id.isNotBlank()) ids.add(rec.id)
                }
                spansToRemove.add(candidate.start..(candidate.endExclusive - 1))
            }
        }
    }

    tagRegex.findAll(text).forEach { match ->
        match.groupValues.getOrNull(1)?.trim()?.let { id ->
            if (id.isNotBlank()) ids.add(id)
        }
    }
    idLineRegex.findAll(text).forEach { match ->
        match.groupValues.getOrNull(1)?.trim()?.let { id ->
            if (id.isNotBlank()) ids.add(id)
        }
    }

    val cleaned = removeSpans(text, spansToRemove)
        .replace(tagRegex, "")
        .replace(idLineRegex, "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    return ParsedAiMessage(
        cleanedText = cleaned,
        listingIds = ids.distinct(),
        recommendations = recommendations.distinctBy { it.id.ifBlank { it.title } }
    )
}

private data class AiRecommendation(
    val id: String,
    val title: String,
    val price: Int,
    val location: String,
    val amenities: List<String>
)

private fun parseRecommendationsFromJson(jsonText: String): List<AiRecommendation>? {
    return try {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) return emptyList()

        fun recFromObj(obj: JSONObject): AiRecommendation {
            return AiRecommendation(
                id = obj.optString("id", ""),
                title = obj.optString("title", ""),
                price = obj.optInt("price", 0),
                location = obj.optString("location", ""),
                amenities = obj.optJSONArray("amenities")?.toStringList().orEmpty()
            )
        }

        fun recsFromArray(array: JSONArray): List<AiRecommendation> {
            return (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                recFromObj(obj)
            }
        }

        if (trimmed.startsWith("[")) {
            return recsFromArray(JSONArray(trimmed))
        }

        val root = JSONObject(trimmed)
        val array = root.optJSONArray("recommendations") ?: root.optJSONArray("listings")
        if (array != null) return recsFromArray(array)

        if (root.has("title") || root.has("price") || root.has("location")) {
            return listOf(recFromObj(root))
        }
        emptyList()
    } catch (_: Exception) {
        null
    }
}

private fun JSONArray.toStringList(): List<String> {
    return (0 until length()).mapNotNull { idx ->
        optString(idx, null)
    }
}

private data class JsonCandidate(
    val text: String,
    val start: Int,
    val endExclusive: Int
)

private fun extractJsonCandidates(text: String): List<JsonCandidate> {
    val candidates = mutableListOf<JsonCandidate>()
    var depth = 0
    var start = -1
    var inString = false
    var escaping = false

    for (i in text.indices) {
        val c = text[i]
        if (inString) {
            if (escaping) {
                escaping = false
            } else if (c == '\\') {
                escaping = true
            } else if (c == '"') {
                inString = false
            }
            continue
        }

        when (c) {
            '"' -> inString = true
            '{' -> {
                if (depth == 0) start = i
                depth++
            }
            '}' -> {
                if (depth > 0) {
                    depth--
                    if (depth == 0 && start >= 0) {
                        candidates.add(JsonCandidate(text.substring(start, i + 1), start, i + 1))
                        start = -1
                    }
                }
            }
        }
    }

    return candidates
}

private fun removeSpans(text: String, spans: List<IntRange>): String {
    if (spans.isEmpty()) return text

    val merged = spans
        .sortedBy { it.first }
        .fold(mutableListOf<IntRange>()) { acc, range ->
            if (acc.isEmpty()) {
                acc.add(range)
            } else {
                val last = acc.last()
                if (range.first <= last.last + 1) {
                    acc[acc.lastIndex] = last.first..maxOf(last.last, range.last)
                } else {
                    acc.add(range)
                }
            }
            acc
        }

    val sb = StringBuilder()
    var index = 0
    for (range in merged) {
        if (index < range.first) {
            sb.append(text.substring(index, range.first))
        }
        index = range.last + 1
    }
    if (index < text.length) {
        sb.append(text.substring(index))
    }
    return sb.toString()
}

@Composable
private fun ListingRecommendations(
    recommendations: List<AiRecommendation>,
    listingIds: List<String>,
    listingCache: Map<String, io.github.howshous.data.models.Listing>,
    onListingClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (recommendations.isNotEmpty()) {
            recommendations.forEach { rec ->
                ListingRecommendationCard(
                    title = rec.title.ifBlank { listingCache[rec.id]?.title.orEmpty() },
                    price = rec.price.takeIf { it > 0 } ?: (listingCache[rec.id]?.price ?: 0),
                    location = rec.location.ifBlank { listingCache[rec.id]?.location.orEmpty() },
                    amenities = if (rec.amenities.isNotEmpty()) rec.amenities else listingCache[rec.id]?.amenities.orEmpty(),
                    onClick = { if (rec.id.isNotBlank()) onListingClick(rec.id) }
                )
            }
        } else {
            listingIds.forEach { listingId ->
                val listing = listingCache[listingId]
                ListingRecommendationCard(
                    title = listing?.title ?: "Listing",
                    price = listing?.price ?: 0,
                    location = listing?.location ?: "",
                    amenities = listing?.amenities ?: emptyList(),
                    onClick = { onListingClick(listingId) }
                )
            }
        }
    }
}

@Composable
private fun ListingRecommendationCard(
    title: String,
    price: Int,
    location: String,
    amenities: List<String>,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (title.isNotBlank()) title else "Listing",
                style = MaterialTheme.typography.titleSmall
            )
            if (price > 0) {
                Text("₱$price/month", style = MaterialTheme.typography.bodySmall)
            }
            if (location.isNotBlank()) {
                Text(location, style = MaterialTheme.typography.bodySmall)
            }
            if (amenities.isNotEmpty()) {
                Text(
                    "Amenities: ${amenities.take(4).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onClick) {
                Text("View listing")
            }
        }
    }
}

