package com.neet.backend.openai

import com.neet.backend.model.GenerateQuestionRequest
import com.neet.backend.model.MockTestSlotRequest

object PromptBuilder {

    private const val MAX_EXCLUDED_STEMS = 20

    private val formattingRules = """
        All text fields are Markdown: use **bold**, bullet lists, and short paragraphs.
        For simple chemical formulas, use Unicode subscript/superscript characters
        directly (e.g. H₂O, CO₂, x², v₀, Δ, °C).

        For real mathematical expressions (fractions, square roots, exponents beyond a
        simple square, integrals, vectors), use LaTeX. This renderer uses a specific,
        non-standard convention — follow it exactly or the math will not render:
        - Inline (on the same line as surrounding text), wrap with double dollar signs on
          both sides with no line break: ${'$'}${'$'}v^2 = u^2 + 2as${'$'}${'$'}
        - As a standalone block, put the opening ${'$'}${'$'} alone on its own line, the
          LaTeX content on the next line(s), and a closing ${'$'}${'$'} alone on its own
          line.
        - Do NOT use a single dollar sign for math — it will not render and will show up
          as a literal character to the student.
        - A ${'$'}${'$'}...${'$'}${'$'} block must contain ONLY the mathematical expression itself —
          never symbol meanings, units, or a "where X is..." explanation inside it (not even
          wrapped in \text{}). LaTeX math mode ignores spaces between bare words, so any English
          explanation placed inside the block renders as an unreadable, unbroken run of letters
          the student cannot read. Always write that explanation as plain Markdown immediately
          after the closing ${'$'}${'$'}, in a normal sentence with spaces between words.
    """.trimIndent()

    val systemPrompt = """
        You are an expert NEET (India) question setter, strictly NCERT-syllabus-aligned.
        Generate exactly one high-quality, unambiguous multiple-choice question for the
        given subject, topic, and difficulty. Exactly one option must be correct.

        $formattingRules

        Before finalizing, work through any calculation privately and double-check it.
        The correctOptionKey field and the reasoning in whyCorrect must agree with each
        other and with your final arithmetic — never state a different answer inside
        whyCorrect than the option you selected as correct.

        Output must match the provided JSON schema exactly, with no extra commentary.
    """.trimIndent()

    fun userPrompt(request: GenerateQuestionRequest): String = buildString {
        appendLine("Subject: ${request.subject}")
        appendLine("Topic: ${request.topic}")
        appendLine("Difficulty: ${request.difficulty}")
        val excluded = request.excludeStems.take(MAX_EXCLUDED_STEMS)
        if (excluded.isNotEmpty()) {
            appendLine(
                "Avoid generating a question whose stem duplicates or closely " +
                    "resembles any of these previously-asked stems:",
            )
            excluded.forEach { stem -> appendLine("- $stem") }
        }
        append("Generate exactly one NEET MCQ now.")
    }

    val verificationSystemPrompt = """
        You are a meticulous NEET answer-key verifier. You will be shown a multiple-choice
        question, its four options, a proposed correct option, and a proposed explanation.

        Independently work through the question yourself from the stem and options alone —
        do not simply trust the proposed answer. Then decide the true correct option.

        If your independent answer matches the proposed one and its explanation is sound
        and internally consistent, you may reuse that explanation. If it does not match,
        or the explanation is inconsistent (for example, if it second-guesses itself
        partway through or names a different option than the one it concludes with),
        output your own verified correct option and a new explanation that is fully
        consistent with it.

        $formattingRules

        Output must match the provided JSON schema exactly, with no extra commentary.
    """.trimIndent()

    fun verificationUserPrompt(
        stem: String,
        options: List<GeneratedOption>,
        proposedCorrectOptionKey: String,
        proposedExplanation: GeneratedExplanation,
    ): String = buildString {
        appendLine("Question stem:")
        appendLine(stem)
        appendLine()
        appendLine("Options:")
        options.forEach { option -> appendLine("${option.key}. ${option.text}") }
        appendLine()
        appendLine("Proposed correct option: $proposedCorrectOptionKey")
        appendLine()
        appendLine("Proposed explanation:")
        appendLine("Why correct: ${proposedExplanation.whyCorrect}")
        appendLine("Why others are wrong: ${proposedExplanation.whyOthersWrong}")
        appendLine("Key concept: ${proposedExplanation.keyConcept}")
        appendLine()
        append("Verify this and return the true correct option with a consistent explanation.")
    }

    // --- Batch variants, used by mock-test bulk generation (see MockTestGenerator) ---

    val batchSystemPrompt = """
        You are an expert NEET (India) question setter, strictly NCERT-syllabus-aligned.
        You will be given a numbered list of question slots, each with a subject, topic,
        and difficulty. Generate exactly one high-quality, unambiguous multiple-choice
        question per slot, in the same order as the list. Exactly one option per question
        must be correct. No two questions in this batch should have duplicate or
        near-duplicate stems, even across different slots.

        $formattingRules

        Before finalizing each question, work through any calculation privately and
        double-check it. Each question's correctOptionKey field and the reasoning in its
        whyCorrect must agree with each other and with your final arithmetic.

        Output must match the provided JSON schema exactly — an array of exactly as many
        questions as there are slots, in the same order — with no extra commentary.
    """.trimIndent()

    fun batchUserPrompt(slots: List<MockTestSlotRequest>): String = buildString {
        appendLine("Generate ${slots.size} NEET MCQs, one per slot below, in this exact order:")
        slots.forEachIndexed { index, slot ->
            appendLine(
                "${index + 1}. Subject: ${slot.subject}, Topic: ${slot.topic}, " +
                    "Difficulty: ${slot.difficulty}",
            )
        }
    }

    val batchVerificationSystemPrompt = """
        You are a meticulous NEET answer-key verifier. You will be shown a numbered list of
        multiple-choice questions, each with its four options, a proposed correct option,
        and a proposed explanation.

        For EACH question independently, work through it yourself from the stem and
        options alone — do not simply trust the proposed answer. Then decide the true
        correct option.

        If your independent answer for a question matches the proposed one and its
        explanation is sound and internally consistent, you may reuse that explanation. If
        it does not match, or the explanation is inconsistent (for example, if it
        second-guesses itself partway through or names a different option than the one it
        concludes with), output your own verified correct option and a new explanation
        that is fully consistent with it.

        $formattingRules

        Output must match the provided JSON schema exactly — an array of exactly as many
        verifications as there are questions, in the same order they were given — with no
        extra commentary.
    """.trimIndent()

    fun batchVerificationUserPrompt(drafts: List<GeneratedQuestionContent>): String = buildString {
        appendLine("Verify these ${drafts.size} questions, in order:")
        drafts.forEachIndexed { index, draft ->
            appendLine()
            appendLine("${index + 1}.")
            appendLine("Question stem:")
            appendLine(draft.stem)
            appendLine("Options:")
            draft.options.forEach { option -> appendLine("${option.key}. ${option.text}") }
            appendLine("Proposed correct option: ${draft.correctOptionKey}")
            appendLine("Proposed explanation:")
            appendLine("Why correct: ${draft.explanation.whyCorrect}")
            appendLine("Why others are wrong: ${draft.explanation.whyOthersWrong}")
            appendLine("Key concept: ${draft.explanation.keyConcept}")
        }
    }

    // --- Topic notes (concepts/formulas reference sheet), see NotesRoutes ---

    val notesSystemPrompt = """
        You are an expert NEET (India) tutor, strictly NCERT-syllabus-aligned. Produce a
        concise but complete reference sheet of the key concepts and formulas for the given
        subject and topic — the kind of thing a student would reread the night before the
        exam to refresh their memory, not a full textbook chapter.

        Structure contentMarkdown as:
        - A short list of the core concepts, each as a **bolded term** followed by a
          one-to-two sentence explanation.
        - Every important formula for this topic, each on its own line, with a brief note
          of what each symbol means and when the formula applies.
        - A few common mistakes or easily-confused points specific to this topic, if any
          are genuinely notable (omit this section entirely rather than padding it).

        Additionally, produce the SAME concepts and formulas again as `cards` — one entry per
        concept and per formula from contentMarkdown, each with:
        - term: the concept name, or the formula itself written compactly (e.g. "v = u + at")
        - content: the one-to-two sentence explanation (concepts), or the symbol meanings and
          when it applies (formulas) — this is what shows on the back of the flashcard
        - type: "CONCEPT" or "FORMULA"
        Every concept and formula in contentMarkdown must have a corresponding card, and vice
        versa — the two are two views of the same material, not independent content.

        $formattingRules

        Output must match the provided JSON schema exactly, with no extra commentary.
    """.trimIndent()

    fun notesUserPrompt(subject: String, topic: String): String =
        "Subject: $subject\nTopic: $topic\n\nGenerate the reference sheet now."
}
