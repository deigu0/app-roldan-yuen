package com.example.mysorteo.model

data class Draw(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val status: String = STATUS_OPEN, // "OPEN", "IN_PROGRESS", "CLOSED"
    val drawnNumbers: List<Int> = emptyList()
) {
    val isOpen: Boolean
        get() = status == STATUS_OPEN && drawnNumbers.isEmpty()

    companion object {
        const val STATUS_OPEN = "OPEN"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_CLOSED = "CLOSED"
    }
}
