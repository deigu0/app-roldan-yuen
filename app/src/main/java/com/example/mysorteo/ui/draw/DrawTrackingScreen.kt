package com.example.mysorteo.ui.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysorteo.model.Draw
import com.example.mysorteo.model.DrawnBall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawTrackingScreen(
    drawId: String,
    userId: String,
    userEmail: String,
    drawViewModel: DrawViewModel,
    onNavigateBack: () -> Unit
) {
    val state by drawViewModel.uiState.collectAsState()
    val draw = state.currentDraw
    val userEntry = state.userEntry

    LaunchedEffect(drawId, userId) {
        drawViewModel.selectDrawAndUser(drawId, userId)
    }

    val drawnBalls = draw?.drawnBalls ?: emptyList()
    val drawnNumbers = draw?.drawnNumbers ?: emptyList()
    val userNumbers = userEntry?.numbers ?: emptyList()
    val matchedNumbers = userNumbers.filter { drawnNumbers.contains(it) }
    
    val bolillasCount = drawnBalls.size
    val matchCount = matchedNumbers.size
    val isStatusFinalized = draw?.status?.equals("finalizado", ignoreCase = true) == true || draw?.status?.equals(
        Draw.STATUS_CLOSED, ignoreCase = true) == true
    val isDrawFinalized = isStatusFinalized && bolillasCount == 6

    val finalResultText: String? = when {
        isDrawFinalized -> if (matchCount == 6) "GANADOR" else "SIGUE INTENTANDO"
        else -> null
    }

    val drawStatusText: String = when {
        isDrawFinalized -> "Sorteo Finalizado"
        draw?.status?.equals("en curso", ignoreCase = true) == true || bolillasCount > 0 -> "En Curso"
        else -> "Abierto"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento en Vivo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Draw & User Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = draw?.name ?: "Sorteo Especial",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Status Badge
                        val (statusBg, statusFg) = when {
                            isDrawFinalized -> Pair(Color(0xFFE0E0E0), Color(0xFF424242))
                            bolillasCount > 0 -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
                            else -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                        }

                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = drawStatusText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusFg
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fecha: ${draw?.date ?: "2026-02-15"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }



            // Status, Bolillas publicadas, Aciertos & Resultado Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (finalResultText) {
                        "GANADOR" -> Color(0xFFE8F5E9)
                        "SIGUE INTENTANDO" -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Row for Bolillas publicadas & Aciertos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Bolillas publicadas",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$bolillasCount / 6",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Aciertos",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$matchCount",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (matchCount == 6) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Final Result or Waiting Message
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDrawFinalized) {
                            Surface(
                                color = if (finalResultText == "GANADOR") Color(0xFF2E7D32) else Color(0xFFC62828),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (finalResultText == "GANADOR") Icons.Default.EmojiEvents else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = finalResultText ?: "",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        } else {
                            // Wait message when draw is not finalized
                            val message = if (bolillasCount == 0 && (draw?.status?.equals("abierto", ignoreCase = true) == true || draw?.isOpen == true)) {
                                "Esperando inicio del sorteo..."
                            } else {
                                "Sorteo en curso... Esperando resultados del sorteo..."
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Registered Entry Card
            Text(
                text = "Tu Jugada Registrada (6 Números):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (userEntry == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No has registrado ninguna jugada para este sorteo.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Regrese a la pantalla de registro para seleccionar sus 6 números.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            userNumbers.forEach { num ->
                                val isMatched = drawnNumbers.contains(num)
                                MatchedNumberBall(
                                    number = num,
                                    isMatched = isMatched
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Drawn Balls Section (Real-time stream with exit order 1..6)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bolillas Publicadas ($bolillasCount/6):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (drawnBalls.isNotEmpty()) {
                    Text(
                        text = "Transmisión en vivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (drawnBalls.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Esperando que se publiquen las bolillas en Firestore...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            drawnBalls.sortedBy { it.orden }.forEach { ball ->
                                val matchesUser = userNumbers.contains(ball.numero)
                                DrawnBallWithOrder(
                                    ball = ball,
                                    matchesUser = matchesUser
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Real-time matched detail breakdown
            if (drawnBalls.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Detalle de Coincidencias:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (matchedNumbers.isEmpty()) {
                            Text(
                                text = "Aún no hay coincidencias entre las bolillas publicadas y tu jugada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Números acertados: ${matchedNumbers.sorted().joinToString(", ")}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchedNumberBall(
    number: Int,
    isMatched: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isMatched) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isMatched) 2.dp else 1.dp,
                    color = if (isMatched) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = if (isMatched) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isMatched) "✓ Acierto" else "",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
fun DrawnBallWithOrder(
    ball: DrawnBall,
    matchesUser: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Nº ${ball.orden}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (matchesUser) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
                .border(
                    width = 2.dp,
                    color = if (matchesUser) Color(0xFFFFD700) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ball.numero.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
