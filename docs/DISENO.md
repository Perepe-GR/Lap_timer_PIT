# Diseño de la pantalla del piloto (horizontal)

## Colores (aprobados, no cambiar)
- Fondo `#0B0D10`; tarjetas `#12161B` con esquinas de 16 dp
- Verde acento `#C6F432` (más rápido, mejor vuelta); rojo `#FF5A5F` (más lento)
- Texto secundario `#9AA3AE`
- Sectores tipo F1: morado `#A855F7`, verde `#C6F432`, amarillo `#FACC15`, neutro `#1E242B`
- Mensajes del pit: amarillo `#FACC15`, verde `#22C55E`, rojo `#EF4444`, azul `#2563EB`, blanco `#F2F4F7`,
  rayas = amarillo con 4 rayas rojas verticales (9 franjas iguales) y el texto sobre una etiqueta oscura

## Vista completa
1. **Delta** (protagonista): tarjeta grande a la izquierda, refresco a 5 Hz. Barra de ±2 % de la mejor vuelta.
2. **Tiempo de vuelta** con las cajitas de sector S1..Sn (colores F1).
3. **Mejor / Última / Óptima** en tres tarjetas pequeñas.
4. **Stint y velocidad**: franja compacta de 40 dp con "STINT n · PILOTO · tiempo/máximo · V hechas/planificadas"
   (gris; verde al llegar al mínimo; ámbar a 5 min del máximo o con las vueltas planificadas cumplidas; rojo al
   superar el máximo) y la velocidad pequeña y gris a la derecha.

## Vista simplificada
Delta gigante con su barra y, debajo, los sectores F1 en grande. Sin nada más.

## Avisos visuales
Franja de 30 dp encima de las vistas, solo mientras dura el problema. Roja: sin antena, sin GPS (más de 3 s sin datos),
límite del stint superado, batería crítica. Ámbar: stint cerca del máximo o vueltas cumplidas, batería baja.
No avisa de batería si el móvil está cargando. Sin sonido ni vibración.

## Elementos ocultos
- **Barra superior** (estado, INICIO, red, GPS): oculta. Se muestra deslizando hacia abajo desde la parte alta
  (no desde el borde, que lo usa Android). Se esconde sola a los 5 s o deslizando hacia arriba.
- **Banda inferior**: solo visible con un mensaje del pit, una petición de cambio de piloto (azul, con botón
  CONFIRMAR CAMBIO, activo solo en boxes) o durante el reconocimiento de circuito.

## Gestos
- Cambio de vista: deslizar a izquierda o derecha empezando por el centro (no choca con el gesto "atrás").

## Reglas
- Sin sonido, sin voz y sin vibración en ningún momento.
- Por encima del límite de bloqueo (10 km/h por defecto, configurable) se bloquean INICIO, "atrás", los botones de
  marcado y CONFIRMAR CAMBIO (no en modo demo).
- Salir de la sesión (atrás o INICIO) pide doble confirmación.

## Pantallas superpuestas (0.8)
- **Boxes**: fondo que parpadea entre #FFE23A y #CA8A04 (texto #0B0D10); al cumplir el tiempo mínimo, entre #4ADE80 y #16A34A.
  Cuenta atrás en la tipografía condensada, a tamaño máximo.
- **Resumen de vuelta**: fondo #0B0D10 casi opaco, tiempo grande en #F2F4F7, delta en verde (#C6F432) o rojo (#FF5A5F),
  colores de sector tipo F1 y, en resistencia, la línea de stint.

## Menús a pantalla completa (0.9)
Fondo #0B0D10, tarjetas #12161B con radio 16dp, campos #1E242B con radio 10dp, texto #F2F4F7, secundario #9AA3AE y
acento verde lima #C6F432 (botón principal con texto oscuro; la opción seleccionada lleva borde verde lima de 2dp).
Colores del stint: rojo #FF5A5F, verde #C6F432, lila #A855F7. Aviso de última vuelta: ámbar #F5A524.

## Layout completo (0.10)
BEST / LAST / OPTIMAL en una sola tarjeta con tres filas (etiqueta a la izquierda, tiempo grande alineado a la derecha).
Cabecera de la tarjeta del tiempo: "LAP n" (blanco, condensada) y, en sprint, "LAP n / total" con barra fina verde lima.
Pantalla amarilla de PADDOCK con el mismo parpadeo que la de PIT. Zona PADDOCK en el editor: rosa #E879F9.
