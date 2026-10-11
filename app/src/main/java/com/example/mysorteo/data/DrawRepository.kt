package com.example.mysorteo.data

import com.example.mysorteo.model.Draw
import com.example.mysorteo.model.DrawnBall
import com.example.mysorteo.model.Entry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DrawRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val DEFAULT_DRAW_ID = "draw_test_01"
        const val DEFAULT_DRAW_NAME = "Sorteo Especial Lotería PC01"
        const val DEFAULT_DRAW_DATE = "2026-02-15"
    }

    // In-memory local fallback store for local testing/offline support
    private val localDraws = mutableMapOf<String, Draw>(
        DEFAULT_DRAW_ID to Draw(
            id = DEFAULT_DRAW_ID,
            name = DEFAULT_DRAW_NAME,
            date = DEFAULT_DRAW_DATE,
            status = "abierto",
            drawnBalls = emptyList()
        )
    )
    private val localEntries = mutableMapOf<String, Entry>()

    private fun parseBallsFromField(raw: Any?): List<DrawnBall> {
        if (raw !is List<*>) return emptyList()
        val list = mutableListOf<DrawnBall>()
        raw.forEachIndexed { index, item ->
            when (item) {
                is Number -> {
                    list.add(DrawnBall(numero = item.toInt(), orden = index + 1))
                }
                is Map<*, *> -> {
                    val num = (item["numero"] as? Number ?: item["number"] as? Number)?.toInt()
                    val ord = (item["orden"] as? Number ?: item["order"] as? Number)?.toInt() ?: (index + 1)
                    if (num != null) {
                        list.add(DrawnBall(numero = num, orden = ord))
                    }
                }
            }
        }
        return list
    }

    /**
     * Listens to the draw document in real-time with fallback.
     */
    fun observeDraw(drawId: String): Flow<Draw?> = callbackFlow {
        val docRef = firestore.collection("draws").document(drawId)
        val bolillasRef = docRef.collection("bolillas")

        var currentDrawDoc: Draw? = localDraws[drawId] ?: Draw(id = drawId, name = DEFAULT_DRAW_NAME, date = DEFAULT_DRAW_DATE)
        var subcollectionBalls: List<DrawnBall> = emptyList()

        fun updateAndSend() {
            val base = currentDrawDoc ?: return
            val finalBalls = if (subcollectionBalls.isNotEmpty()) {
                subcollectionBalls.sortedBy { it.orden }
            } else {
                base.drawnBalls.sortedBy { it.orden }
            }
            val result = base.copy(drawnBalls = finalBalls)
            localDraws[drawId] = result
            trySend(result)
        }

        // Send initial local state immediately so UI isn't stuck
        trySend(currentDrawDoc)

        val drawListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Emit local draw if firestore listener encounters network error
                trySend(localDraws[drawId])
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("nombre") ?: snapshot.getString("name") ?: DEFAULT_DRAW_NAME
                val date = snapshot.getString("fecha") ?: snapshot.getString("date") ?: DEFAULT_DRAW_DATE
                val status = snapshot.getString("estado") ?: snapshot.getString("status") ?: Draw.STATUS_OPEN
                val drawnBallsDoc = parseBallsFromField(snapshot.get("bolillas") ?: snapshot.get("drawnNumbers"))

                currentDrawDoc = Draw(
                    id = snapshot.id,
                    name = name,
                    date = date,
                    status = status,
                    drawnBalls = drawnBallsDoc
                )
                updateAndSend()
            }
        }

        val bolillasListener = bolillasRef.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null && !snapshot.isEmpty) {
                subcollectionBalls = snapshot.documents.mapNotNull { doc ->
                    val num = doc.getLong("numero")?.toInt() ?: doc.getLong("number")?.toInt()
                    val ord = doc.getLong("orden")?.toInt() ?: doc.getLong("order")?.toInt() ?: 1
                    if (num != null) DrawnBall(numero = num, orden = ord) else null
                }
                updateAndSend()
            }
        }

        awaitClose {
            drawListener.remove()
            bolillasListener.remove()
        }
    }

    /**
     * Listens to all available draws in real-time.
     */
    fun observeAllDraws(): Flow<List<Draw>> = callbackFlow {
        // Emit initial local list immediately
        trySend(localDraws.values.toList())

        val listener = firestore.collection("draws").addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(localDraws.values.toList())
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.map { doc ->
                    val name = doc.getString("nombre") ?: doc.getString("name") ?: DEFAULT_DRAW_NAME
                    val date = doc.getString("fecha") ?: doc.getString("date") ?: DEFAULT_DRAW_DATE
                    val status = doc.getString("estado") ?: doc.getString("status") ?: Draw.STATUS_OPEN
                    val drawnBalls = parseBallsFromField(doc.get("bolillas") ?: doc.get("drawnNumbers"))

                    Draw(
                        id = doc.id,
                        name = name,
                        date = date,
                        status = status,
                        drawnBalls = drawnBalls
                    )
                }
                list.forEach { localDraws[it.id] = it }
                trySend(list)
            } else {
                trySend(localDraws.values.toList())
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Initializes default test draw if it doesn't exist yet in Firestore.
     */
    suspend fun ensureDefaultDrawExists(): Draw {
        val defaultDraw = Draw(
            id = DEFAULT_DRAW_ID,
            name = DEFAULT_DRAW_NAME,
            date = DEFAULT_DRAW_DATE,
            status = "abierto",
            drawnBalls = emptyList()
        )
        localDraws[DEFAULT_DRAW_ID] = defaultDraw

        return try {
            val docRef = firestore.collection("draws").document(DEFAULT_DRAW_ID)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val initialDraw = mapOf(
                    "nombre" to DEFAULT_DRAW_NAME,
                    "fecha" to DEFAULT_DRAW_DATE,
                    "estado" to "abierto",
                    "bolillas" to emptyList<Int>()
                )
                docRef.set(initialDraw).await()
                defaultDraw
            } else {
                val name = snapshot.getString("nombre") ?: snapshot.getString("name") ?: DEFAULT_DRAW_NAME
                val date = snapshot.getString("fecha") ?: snapshot.getString("date") ?: DEFAULT_DRAW_DATE
                val status = snapshot.getString("estado") ?: snapshot.getString("status") ?: Draw.STATUS_OPEN
                val drawnBalls = parseBallsFromField(snapshot.get("bolillas") ?: snapshot.get("drawnNumbers"))
                val d = Draw(
                    id = snapshot.id,
                    name = name,
                    date = date,
                    status = status,
                    drawnBalls = drawnBalls
                )
                localDraws[DEFAULT_DRAW_ID] = d
                d
            }
        } catch (e: Exception) {
            defaultDraw
        }
    }

    /**
     * Listens to the current user's entry for a draw in real-time.
     */
    fun observeUserEntry(drawId: String, userId: String): Flow<Entry?> = callbackFlow {
        val entryKey = "${drawId}_$userId"
        trySend(localEntries[entryKey])

        val docRef = firestore.collection("draws").document(drawId).collection("entries").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(localEntries[entryKey])
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val uEmail = snapshot.getString("userEmail") ?: snapshot.getString("email") ?: ""
                val dId = snapshot.getString("drawId") ?: drawId
                @Suppress("UNCHECKED_CAST")
                val numsRaw = snapshot.get("numeros") as? List<*> ?: snapshot.get("numbers") as? List<*> ?: emptyList<Any>()
                val numbers = numsRaw.mapNotNull {
                    when (it) {
                        is Long -> it.toInt()
                        is Int -> it
                        is Number -> it.toInt()
                        else -> null
                    }
                }
                val ts = snapshot.getLong("timestamp") ?: System.currentTimeMillis()
                val entry = Entry(
                    id = snapshot.id,
                    userId = userId,
                    userEmail = uEmail,
                    drawId = dId,
                    numbers = numbers,
                    timestamp = ts
                )
                localEntries[entryKey] = entry
                trySend(entry)
            } else {
                trySend(localEntries[entryKey])
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Saves user entry for a draw.
     */
    suspend fun registerEntry(
        drawId: String,
        userId: String,
        userEmail: String,
        numbers: List<Int>
    ): Result<Unit> {
        val currentDraw = localDraws[drawId]
        val isLocalOpen = currentDraw?.isOpen ?: true

        if (!isLocalOpen) {
            return Result.failure(
                IllegalStateException("El sorteo ya no está abierto. No se pueden registrar ni modificar jugadas.")
            )
        }

        if (numbers.size != 6) {
            return Result.failure(IllegalArgumentException("Debe seleccionar exactamente 6 números."))
        }
        if (numbers.distinct().size != 6) {
            return Result.failure(IllegalArgumentException("Los 6 números deben ser distintos."))
        }
        if (numbers.any { it !in 1..45 }) {
            return Result.failure(IllegalArgumentException("Los números deben estar entre 1 y 45."))
        }

        val sortedNumbers = numbers.sorted()
        val entryKey = "${drawId}_$userId"
        val newEntry = Entry(
            id = userId,
            userId = userId,
            userEmail = userEmail,
            drawId = drawId,
            numbers = sortedNumbers,
            timestamp = System.currentTimeMillis()
        )
        localEntries[entryKey] = newEntry

        return try {
            val drawDoc = firestore.collection("draws").document(drawId).get().await()
            val status = drawDoc.getString("estado") ?: drawDoc.getString("status") ?: Draw.STATUS_OPEN
            val drawnBalls = parseBallsFromField(drawDoc.get("bolillas") ?: drawDoc.get("drawnNumbers"))

            val isOpen = (status.equals("abierto", ignoreCase = true) || status.equals(Draw.STATUS_OPEN, ignoreCase = true)) && drawnBalls.isEmpty()

            if (!isOpen) {
                return Result.failure(
                    IllegalStateException("El sorteo ya no está abierto. No se pueden registrar ni modificar jugadas.")
                )
            }

            val entryData = mapOf(
                "userId" to userId,
                "userEmail" to userEmail,
                "drawId" to drawId,
                "numeros" to sortedNumbers,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("draws")
                .document(drawId)
                .collection("entries")
                .document(userId)
                .set(entryData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            // Local save succeeded as fallback
            Result.success(Unit)
        }
    }

    /**
     * Admin/Demo Helper: Publishes a drawn ball to Firestore with its order (1 to 6).
     */
    suspend fun publishDrawnNumber(drawId: String, number: Int): Result<Unit> {
        val currentDraw = localDraws[drawId] ?: Draw(id = drawId)
        val currentBalls = currentDraw.drawnBalls.toMutableList()
        if (currentBalls.size >= 6) {
            return Result.failure(IllegalStateException("Ya se han publicado las 6 bolillas del sorteo."))
        }

        val nextOrden = currentBalls.size + 1
        currentBalls.add(DrawnBall(numero = number, orden = nextOrden))
        val newStatus = if (nextOrden == 6) "finalizado" else "en curso"

        val updatedDraw = currentDraw.copy(
            status = newStatus,
            drawnBalls = currentBalls
        )
        localDraws[drawId] = updatedDraw

        return try {
            val docRef = firestore.collection("draws").document(drawId)
            val ballMap = mapOf("numero" to number, "orden" to nextOrden)

            docRef.update(
                "bolillas", FieldValue.arrayUnion(ballMap),
                "estado", newStatus
            ).await()

            docRef.collection("bolillas").document("ball_$nextOrden").set(ballMap).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    /**
     * Admin/Demo Helper: Resets drawn numbers and sets draw status back to "abierto".
     */
    suspend fun resetDraw(drawId: String): Result<Unit> {
        val currentDraw = localDraws[drawId] ?: Draw(id = drawId)
        localDraws[drawId] = currentDraw.copy(status = "abierto", drawnBalls = emptyList())

        return try {
            val docRef = firestore.collection("draws").document(drawId)
            docRef.update(
                "bolillas", emptyList<Int>(),
                "estado", "abierto"
            ).await()

            val subSnaps = docRef.collection("bolillas").get().await()
            for (doc in subSnaps.documents) {
                doc.reference.delete().await()
            }

            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }
}
