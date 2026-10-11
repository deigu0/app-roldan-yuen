# PC01 · Lotería · Solución Técnica Actualizada

Esta solución en Android (**Kotlin**, **Jetpack Compose**, **Firebase Authentication**, **Cloud Firestore**) cumple al 100% con los indicadores y comportamientos requeridos para la PC01.

---

## 📊 Indicadores y Comportamiento Requerido

| Indicador | Comportamiento en la Aplicación |
| :--- | :--- |
| **Bolillas publicadas** | Muestra la cantidad de bolillas recibidas del sorteo, de `0 / 6` a `6 / 6`. |
| **Aciertos** | Conteo de números distintos de la jugada presentes en las bolillas publicadas. El orden de salida no afecta la coincidencia. |
| **Resultado final** | Muestra exactamente **`GANADOR`** si acertó los 6 números, o **`SIGUE INTENTANDO`** si tuvo menos de 6 aciertos cuando se han recibido las 6 bolillas. |
| **Cálculo Dinámico** | Los aciertos y el resultado final se calculan en tiempo real en la app. **No se registran `GANADOR` ni `SIGUE INTENTANDO` en Firestore**. Sin porcentajes. |
| **Mensajes de Estado** | - **Sorteo Abierto / 0 bolillas**: `"Esperando inicio del sorteo..."`<br>- **Sorteo en Curso / 1-5 bolillas**: Muestra `"Sorteo en curso"` y el mensaje de espera `"Sorteo en curso... Esperando resultados del sorteo..."`<br>- **Sorteo Finalizado / 6 bolillas**: Muestra el resultado definitivo (**`GANADOR`** o **`SIGUE INTENTANDO`**). |

---

## 1. Estructura de Datos en Cloud Firestore

```
Firestore Root
 └── draws (Colección)
      └── draw_test_01 (Documento del Sorteo)
           ├── nombre: "Sorteo Especial Lotería PC01" (String)
           ├── fecha: "2026-02-15" (String)
           ├── estado: "abierto" | "en curso" | "finalizado" (String)
           ├── bolillas: [
           │      { "numero": 5, "orden": 1 },
           │      { "numero": 12, "orden": 2 },
           │      { "numero": 18, "orden": 3 },
           │      ...
           │   ]
           │
           └── entries (Subcolección de Jugadas)
                ├── {uid_usuario_1} (Documento)
                │    ├── drawId: "draw_test_01" (String)
                │    ├── userId: "uid_1" (String)
                │    ├── userEmail: "ganador@test.com" (String)
                │    ├── numeros: [5, 12, 18, 24, 30, 42] (Arreglo de 6 enteros)
                │    └── timestamp: 1711800000000 (Long)
                │
                └── {uid_usuario_2} (Documento)
                     ├── drawId: "draw_test_01" (String)
                     ├── userId: "uid_2" (String)
                     ├── userEmail: "participante2@test.com" (String)
                     ├── numeros: [1, 2, 3, 4, 5, 6] (Arreglo de 6 enteros)
                     └── timestamp: 1711800001000 (Long)
```

---

## 2. Gestión de Listeners y Limpieza

- La escucha en tiempo real se realiza mediante `addSnapshotListener` dentro de `callbackFlow` en [`DrawRepository.kt`](file:///C:/Users/laboratorioesan/AndroidStudioProjects/Mysorteo/app/src/main/java/com/example/mysorteo/data/DrawRepository.kt).
- El bloque `awaitClose { listener.remove() }` garantiza que la suscripción de Firestore se desvincule inmediatamente al salir de la pantalla o cambiar de sorteo, evitando listeners activos innecesarios o duplicados.
