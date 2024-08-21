package ru.netology.nmedia.model

// флаги состояний
data class FeedModelState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val refreshing: Boolean = false,
)
