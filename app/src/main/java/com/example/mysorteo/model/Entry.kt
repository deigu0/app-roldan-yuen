package com.example.mysorteo.model

data class Entry(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val drawId: String = "",
    val numbers: List<Int> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Calculates which numbers in this entry match the drawn numbers.
     */
    fun getMatchedNumbers(drawnNumbers: List<Int>): List<Int> {
        return numbers.filter { drawnNumbers.contains(it) }
    }

    /**
     * Returns match count out of total required numbers (6).
     */
    fun matchCount(drawnNumbers: List<Int>): Int {
        return getMatchedNumbers(drawnNumbers).size
    }

    /**
     * Returns progress as float between 0.0 and 1.0 (e.g., matchCount / 6.0).
     */
    fun progress(drawnNumbers: List<Int>): Float {
        return matchCount(drawnNumbers) / 6f
    }

    /**
     * Check if user won (matched all 6 numbers).
     */
    fun isWinner(drawnNumbers: List<Int>): Boolean {
        return numbers.isNotEmpty() && matchCount(drawnNumbers) == 6
    }
}
