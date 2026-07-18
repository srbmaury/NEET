package com.neet.app.ui.question

import com.neet.app.data.model.Question

sealed interface QuestionUiState {
    data object Loading : QuestionUiState
    data class Error(val message: String) : QuestionUiState
    data class Ready(
        val question: Question,
        val selectedOptionKey: String? = null,
        val isAnswered: Boolean = false,
    ) : QuestionUiState
}
