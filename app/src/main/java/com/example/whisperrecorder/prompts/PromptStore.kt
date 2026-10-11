package com.example.whisperrecorder.prompts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID


data class PromptItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val text: String,
    val builtIn: Boolean = false,
    val quizlet: Boolean = false
)

object PromptStore {
    private const val PREFS = "prompt_store"
    private const val KEY = "custom_prompts"
    private const val BACKUP_SCHEMA = "whisper-recorder-custom-prompts"
    private const val BACKUP_VERSION = 1
    const val CUSTOM_CATEGORY = "Custom"

    val builtIns = listOf(
        PromptItem(
            "lecture-study-guide", "Detailed Study Guide", "Comprehensive, structured notes for studying", "Lecture",
            "Use the attached transcript to create a detailed study guide. Treat it as an automated speech-to-text transcript, so correct obvious transcription errors only when the intended wording is clear and flag uncertain terminology instead of inventing corrections. Organize the material with clear headings and subheadings. Preserve clinically or academically important details, define key terms, explain mechanisms and relationships, identify concepts the lecturer emphasized or repeated, and include high-yield takeaways. Distinguish information that came from the transcript from any supplemental explanation you add. Do not omit important details merely to make the guide shorter.",
            true
        ),
        PromptItem(
            "lecture-summary", "Concise Lecture Summary", "Major concepts, supporting details, and takeaways", "Lecture",
            "Summarize the attached lecture transcript into a concise but complete set of notes. Identify the major topics, the most important supporting details, conclusions, examples that clarify a concept, and the key takeaways. Treat the source as an automated transcript and flag unclear or uncertain terminology rather than guessing. Keep the summary structured and easy to scan.",
            true
        ),
        PromptItem(
            "lecture-exam", "Exam Review", "High-yield and testable material", "Lecture",
            "Turn the attached lecture transcript into an exam review. Focus on definitions, distinctions, classifications, processes, cause-and-effect relationships, facts emphasized or repeated by the lecturer, likely testable details, and common areas of confusion. Organize the output by topic and finish with a high-yield review checklist. Treat uncertain transcription wording cautiously and do not invent facts that are not supported by the transcript.",
            true
        ),
        PromptItem(
            "lecture-flashcards", "Quizlet Flashcards", "Import-ready term and definition cards", "Lecture",
            "Create high-quality Quizlet flashcards from the attached lecture transcript. Output ONLY the import data with no heading, bullets, numbering, markdown, introduction, or closing text. Use exactly this format: Term,Definition;Term,Definition;Term,Definition. A comma separates the front of a card from its definition and a semicolon separates one card from the next. Do not use semicolons inside a card. Avoid unnecessary commas inside the term or definition; use dashes or parentheses instead where punctuation is needed. Focus on meaningful concepts rather than trivial facts, avoid duplicate cards, and flag uncertain transcript terminology instead of guessing.",
            true, true
        ),
        PromptItem(
            "lecture-practice", "Quizlet Practice Questions", "Questions with answers and rationales", "Lecture",
            "Create practice questions from the attached lecture transcript for Quizlet import. Output ONLY the import data with no heading, bullets, numbering, markdown, introduction, or closing text. Use exactly this format: Question,Answer and rationale;Question,Answer and rationale. A comma separates each question from its answer and rationale and a semicolon separates cards. Do not use semicolons inside a card. Avoid unnecessary commas within either side; use dashes or parentheses instead. Include a clear correct answer for every question and a useful rationale. For multiple-choice questions include all answer choices on the question side and put the correct choice plus rationale on the answer side. Do not invent content unsupported by the transcript.",
            true, true
        ),
        PromptItem(
            "lecture-nclex", "Quizlet NCLEX-Style Questions", "Clinical questions with answers and rationales", "Lecture",
            "Create clinically meaningful NCLEX-style practice questions using the attached lecture transcript. Output ONLY Quizlet import data in this exact format: Question with answer choices,Correct answer and rationale;Question with answer choices,Correct answer and rationale. A comma separates the card front and back and a semicolon separates cards. Do not use semicolons inside a card and avoid unnecessary commas within a card. Include the correct answer and a concise rationale for every question. When appropriate explain why distractors are less appropriate. Use only clinically defensible information supported by the transcript and flag uncertainty instead of inventing details.",
            true, true
        ),
        PromptItem(
            "meeting-summary", "Meeting Summary", "Discussion points, decisions, and open questions", "Meeting",
            "Summarize the attached meeting transcript. Capture the purpose of the meeting, major discussion points, important context, decisions made, unresolved questions, risks or blockers, and next steps. Do not invent names, commitments, deadlines, or decisions that were not actually stated. Clearly label uncertainty caused by transcription errors.",
            true
        ),
        PromptItem(
            "meeting-minutes", "Meeting Minutes", "Structured formal minutes", "Meeting",
            "Convert the attached meeting transcript into clear meeting minutes. Include meeting purpose, topics discussed, key points by topic, decisions, agreements, unresolved items, and next steps. Include owners and dates only when explicitly stated. If an owner or deadline is not stated, write 'Not specified' rather than guessing. Keep the minutes professional and concise.",
            true
        ),
        PromptItem(
            "meeting-actions", "Action Items", "Owners, due dates, priority, and context", "Meeting",
            "Extract actionable items from the attached meeting transcript. Present them in a concise table with Action, Owner, Due date, Priority, and Context. Include only actions supported by the transcript. Never invent an owner, deadline, or priority; write 'Not specified' when needed. After the table list blockers, dependencies, and unresolved questions separately.",
            true
        ),
        PromptItem(
            "meeting-exec", "Executive Brief", "Decisions, risks, blockers, and follow-up", "Meeting",
            "Create a short executive brief from the attached meeting transcript. Prioritize outcomes, decisions, major risks, blockers, dependencies, unresolved questions, and required follow-up. Keep background detail minimal unless it changes the meaning of a decision. Do not invent missing information.",
            true
        ),
        PromptItem(
            "meeting-decisions", "Decisions & Follow-Up", "Separate confirmed decisions from proposals", "Meeting",
            "Analyze the attached meeting transcript and separate confirmed decisions from ideas or proposals that were merely discussed. Then list follow-up actions, unresolved questions, dependencies, and risks. Include owners and deadlines only when explicitly stated. End with a concise checklist of what should happen next.",
            true
        )
    )

    fun all(context: Context): List<PromptItem> = builtIns + custom(context)

    fun custom(context: Context): List<PromptItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val title = o.optString("title").trim()
                val text = o.optString("text").trim()
                if (title.isBlank() || text.isBlank()) return@mapNotNull null
                PromptItem(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    title = title,
                    description = o.optString("description").trim(),
                    category = CUSTOM_CATEGORY,
                    text = text,
                    builtIn = false,
                    quizlet = o.optBoolean("quizlet", false)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveCustom(context: Context, item: PromptItem) {
        val items = custom(context).toMutableList()
        val normalized = item.copy(
            id = item.id.ifBlank { UUID.randomUUID().toString() },
            category = CUSTOM_CATEGORY,
            builtIn = false
        )
        val idx = items.indexOfFirst { it.id == normalized.id }
        if (idx >= 0) items[idx] = normalized else items.add(normalized)
        persist(context, items)
    }

    fun deleteCustom(context: Context, id: String) {
        persist(context, custom(context).filterNot { it.id == id })
    }

    fun exportCustomJson(context: Context): String {
        val promptArray = toJsonArray(custom(context))
        return JSONObject().apply {
            put("schema", BACKUP_SCHEMA)
            put("version", BACKUP_VERSION)
            put("exportedAtEpochMs", System.currentTimeMillis())
            put("prompts", promptArray)
        }.toString(2)
    }

    fun importPreviewCount(raw: String): Int = parseBackup(raw).size

    fun importCustomJson(context: Context, raw: String, replace: Boolean): Int {
        val incoming = parseBackup(raw)
        val result = if (replace) {
            incoming
        } else {
            val merged = LinkedHashMap<String, PromptItem>()
            custom(context).forEach { merged[it.id] = it }
            incoming.forEach { merged[it.id] = it }
            merged.values.toList()
        }
        persist(context, result)
        return incoming.size
    }

    private fun parseBackup(raw: String): List<PromptItem> {
        val root = JSONObject(raw)
        require(root.optString("schema") == BACKUP_SCHEMA) { "Not a Whisper Recorder custom-prompt backup." }
        require(root.optInt("version", -1) == BACKUP_VERSION) { "Unsupported custom-prompt backup version." }
        val arr = root.optJSONArray("prompts") ?: throw IllegalArgumentException("Backup does not contain prompts.")
        val items = ArrayList<PromptItem>(arr.length())
        val seenIds = HashSet<String>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: throw IllegalArgumentException("Prompt ${i + 1} is invalid.")
            val title = o.optString("title").trim()
            val text = o.optString("text").trim()
            require(title.isNotBlank() && text.isNotBlank()) { "Prompt ${i + 1} is missing a title or prompt text." }
            var id = o.optString("id").trim().ifBlank { UUID.randomUUID().toString() }
            if (!seenIds.add(id)) {
                id = UUID.randomUUID().toString()
                seenIds.add(id)
            }
            items += PromptItem(
                id = id,
                title = title,
                description = o.optString("description").trim(),
                category = CUSTOM_CATEGORY,
                text = text,
                builtIn = false,
                quizlet = o.optBoolean("quizlet", false)
            )
        }
        return items
    }

    private fun persist(context: Context, items: List<PromptItem>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, toJsonArray(items).toString())
            .apply()
    }

    private fun toJsonArray(items: List<PromptItem>): JSONArray = JSONArray().apply {
        items.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("description", p.description)
                put("category", CUSTOM_CATEGORY)
                put("text", p.text)
                put("quizlet", p.quizlet)
            })
        }
    }
}
