package com.example.mysorteo.model

data class DrawnBall(
    val numero: Int,
    val orden: Int
)

data class Draw(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val status: String = STATUS_OPEN, // "abierto", "en curso", "finalizado" / "OPEN", "IN_PROGRESS", "CLOSED"
    val drawnBalls: List<DrawnBall> = emptyList()
) {
    val drawnNumbers: List<Int>
        get() = drawnBalls.sortedBy { it.orden }.map { it.numero }

    val isOpen: Boolean
        get() = (status.equals(STATUS_OPEN, ignoreCase = true) || status.equals("abierto", ignoreCase = true)) && drawnBalls.isEmpty()

    val isCompleted: Boolean
        get() = status.equals(STATUS_CLOSED, ignoreCase = true) || status.equals("finalizado", ignoreCase = true) || drawnBalls.size >= 6

    val statusDisplayName: String
        get() = when {
            isOpen -> "Abierto"
            isCompleted -> "Finalizado"
            else -> "En curso"
        }

    companion object {
        const val STATUS_OPEN = "OPEN"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_CLOSED = "CLOSED"
    }
}
