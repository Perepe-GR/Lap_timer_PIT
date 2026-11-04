# Contrato de Diseño e Interfaz de Usuario (`ui_contract.md`)

Este documento constituye el **Contrato Oficial de Diseño e Interfaz de Usuario** de la aplicación Android `LapTimer`. Todas las modificaciones visuales, pantallas, vistas personalizadas y componentes actuales y futuros deben cumplir estrictamente las directrices y reglas aquí expuestas.

---

## 1. Tokens de Diseño Dinámicos Obligatorios

Está **estrictamente prohibido** utilizar colores *hardcodeados* (como `#000000`, `Color.BLACK` o valores literales dispersos) en las vistas o componentes UI. Toda la interfaz debe consumir dinámicamente el tema activo (`ThemeManager.getTheme(context)` / `PageActivity` tokens):

| Token de Diseño | Uso Obligatorio | Valor por Defecto (F1 Lime Theme) |
|---|---|---|
| **`ColorBackground`** | Fondo principal de la actividad o pantalla completa. | `#0B0D10` |
| **`ColorSurface`** | Fondo de tarjetas (`cards`), contenedores de grupo y botones secundarios. | `#12161B` / `#1E242B` |
| **`ColorPrimary`** | Botones de acción principal, estado activo y acentos destacados (Verde Lima). | `#C6F432` |
| **`ColorTextMain`** | Títulos de pantalla, texto primario de lectura y valores activos. | `#FFFFFF` / `#F2F4F7` |
| **`ColorTextSecondary`** | Subtítulos, etiquetas de campos (*labels*), unidades y texto descriptivo (*muted*). | `#9AA3AE` |
| **`ColorTelemetryFaster`** | Delta negativo (mejora de tiempo), mejor sector y mejor vuelta personal. | `#C6F432` / `#16A34A` |
| **`ColorTelemetrySlower`** | Delta positivo (pérdida de tiempo) y alertas de penalización/retraso. | `#EF4444` / `#FF5A5F` |
| **`ColorAlert`** | Alertas de batería crítica, avisos destructivos o acciones de emergencia. | `#EF4444` / `#C2410C` |

---

## 2. Invariantes Estructurales de Navegación y Disposición

1. **Header Superior Integrado:**
   - Todas las pantallas secundarias, formularios, configuraciones y submenús (incluyendo los de Endurance y Editor) deben contar con un botón fijo de retorno **`‹  BACK`** situado en la esquina superior izquierda.
   - El texto del botón debe utilizar **`ColorPrimary`** (`#C6F432`) en negrita para asegurar visibilidad inmediata.

2. **Acciones y Barras Inferiores (Footers):**
   - **Acción Principal / Destacada:** Se ubicará a la derecha de la barra inferior o ocupará el ancho completo si es única, utilizando **`ColorPrimary`** con texto oscuro en negrita.
   - **Acciones Secundarias o Cancelación:** Se ubicarán a la izquierda utilizando **`ColorSurface`** con texto claro.

---

## 3. Geometría, Estética y Tipografía

1. **Geometría y Radio de Bordes:**
   - Todas las tarjetas, contenedores de entrada (*inputs*) y botones deben mantener esquinas redondeadas uniformes de entre **`12dp` y `16dp`** (`roundBg(color, radiusDp)`).
   - Estilo visual **Flat Design** plano, sin sombras pesadas (*elevations* recargadas) que entorpezcan la legibilidad bajo luz solar directa.

2. **Tipografía de Telemetría:**
   - **Obligatorio para Telemetría:** Toda la información numérica de alta frecuencia y tiempos de precisión (tiempos de vuelta, deltas de tiempo, velocidad en km/h, sectores, frecuencia Hz) debe utilizar estrictamente fuentes numéricas de ancho constante (**Monospace** o **TabularFigures** / `Typeface.MONOSPACE` / `Typeface.DEFAULT_BOLD`).
   - Evita saltos de anchura o parpadeos de alineación durante las actualizaciones a 25Hz.

---

## 🚨 REGLA DE ORO

Antes de realizar cualquier modificación en el código fuente, la IA o el desarrollador debe **leer y cumplir de manera cruzada** todos los archivos Markdown situados en la raíz del proyecto (`PROJECT_MAP.md`, `README.md`, `CONTRATO-PANEL.md`, `ui_contract.md`, `CHANGELOG.md`) para garantizar la coherencia arquitectónica, de datos y visual.
