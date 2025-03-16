package com.example.calandy.api

import com.example.calandy.model.Event

object EventsApi {
    private val nearbyEvents = listOf(
        Event(
            id = "1",
            title = "Summer Music Festival",
            date = "2025-06-15",
            venue = "Central Park",
            imageUrl = "https://picsum.photos/300/200?random=1",
            distance = 0.5f
        ),
        Event(
            id = "2",
            title = "Food & Wine Expo",
            date = "2025-04-20",
            venue = "Convention Center",
            imageUrl = "https://picsum.photos/300/200?random=2",
            distance = 1.2f
        ),
        Event(
            id = "3",
            title = "Tech Conference 2025",
            date = "2025-05-10",
            venue = "Innovation Hub",
            imageUrl = "https://picsum.photos/300/200?random=3",
            distance = 2.1f
        )
    )

    private val attendingEvents = listOf(
        Event(
            id = "4",
            title = "Art Gallery Opening",
            date = "2025-03-30",
            venue = "Modern Art Museum",
            imageUrl = "https://picsum.photos/300/200?random=4",
            isAttending = true
        ),
        Event(
            id = "5",
            title = "Comedy Night",
            date = "2025-04-05",
            venue = "Laugh Factory",
            imageUrl = "https://picsum.photos/300/200?random=5",
            isAttending = true
        )
    )

    fun getNearbyEvents(): List<Event> = nearbyEvents
    fun getAttendingEvents(): List<Event> = attendingEvents
}