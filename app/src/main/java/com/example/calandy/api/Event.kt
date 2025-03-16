package com.example.calandy.model

data class Event(
    val id: String,
    val title: String,
    val date: String,
    val venue: String,
    val imageUrl: String,
    val isAttending: Boolean = false,
    val distance: Float? = null
)