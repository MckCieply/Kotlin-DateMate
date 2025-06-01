package com.mckcieply.datemate

data class CalendarEventModel (
        val eventId: String,
        val summary: String,
        val startTime: String,
        val location: String?,
        val description: String?)
