# Simulador de AFD y AFN

Aplicación de escritorio desarrollada en Java Swing para dibujar, editar y simular autómatas finitos deterministas (AFD) y no deterministas (AFN). La interfaz permite trabajar directamente sobre un lienzo, definir alfabetos, crear estados y transiciones, ejecutar una cadena paso a paso y convertir un AFN en su AFD equivalente mediante construcción de subconjuntos.

La lógica de automatización y la conversión AFN → AFD no dependen de librerías externas: están implementadas dentro del proyecto.

![Ejemplo del editor y la simulación de un autómata](.img/sc1.jpg)

## Estado del proyecto

- Lenguaje: Java
- Interfaz: Swing
- Arquitectura: modelo + vista + simulación
- Gestión de dependencias: ninguna externa (sin Maven ni Gradle)
- Validado con JDK 25 en este repositorio

## Características

- Dibujo libre de estados, transiciones y auto-transiciones.
- Estados iniciales y finales, renombrado y movimiento de nodos.
- Cambio de color del trazo, deshacer y limpieza del autómata.
- Modo AFD con validación de determinismo.
- Modo AFN con múltiples destinos por símbolo y transiciones ε.
- Tabla de transiciones sincronizada con el autómata.
- Simulación manual o automática con reproductor y velocidad ajustable.
- Resaltado del estado activo, transiciones recorridas y resultado final.
- Conversión de AFN a AFD mediante cierre epsilon y construcción de subconjuntos.

## Requisitos

- JDK 17 o superior.
- `javac` y `java` disponibles en el `PATH`.

> El proyecto se ejecuta correctamente con JDK 25, pero también es compatible con versiones recientes de Java.

## Ejecutar el proyecto

Desde la raíz del repositorio:

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out afdpainter.Main
```

## Flujo rápido de uso

1. Elige el modo **AFD** o **AFN** desde la barra superior.
2. Define el alfabeto Σ en el campo correspondiente y guarda el valor.
3. Usa **Dibujar Estado** para crear nodos. El primer estado creado es el inicial.
4. Usa **Dibujar Transición** para conectar estados. En AFN puedes marcar transiciones vacías (`ε`).
5. Marca estados iniciales y finales según corresponda.
6. Escribe una cadena y pulsa **Verificar / Cargar Cadena**.
7. Usa el panel inferior para ejecutar la simulación paso a paso.

Durante la ejecución, el estado actual se resalta y la cadena termina en verde si es aceptada o en rojo si no lo es.

![Simulación de AFN paso a paso](.img/rec2.gif)

## AFD y AFN

### AFD

En un AFD, para cada estado y símbolo existe como máximo un destino. La aplicación valida y evita crear transiciones conflictivas.

### AFN

En un AFN, un mismo símbolo puede conducir a varios estados. El editor admite múltiples destinos y transiciones vacías `ε`.

![AFN con múltiples caminos y tabla de transiciones](.img/rec1.gif)

### Conversión AFN → AFD

1. Selecciona el modo **AFN**.
2. Dibuja el autómata y define el alfabeto.
3. Asegúrate de que exista un estado inicial.
4. Pulsa **Convertir AFN → AFD**.

Se abre una nueva ventana con el AFD equivalente generado por construcción de subconjuntos, junto con la tabla de transición del resultado.

![Conversión de un AFN a un AFD](.img/rec3.gif)

## Controles principales

| Control | Función |
| --- | --- |
| **Dibujar Estado** | Crea un estado en el lienzo. |
| **Dibujar Transición** | Conecta dos estados y solicita los símbolos. |
| **Marcar Inicial** | Establece el estado inicial. |
| **Marcar Final** | Activa o desactiva el estado final. |
| **Mover** | Reubica un estado y ajusta sus transiciones. |
| **Borrar** | Elimina un estado o transición. |
| **Color** | Cambia el color de los trazos. |
| **Deshacer** | Revierte la última acción de edición. |
| **Limpiar Todo** | Borra el autómata actual tras confirmar. |
| **Ayuda** | Abre la guía de uso del programa. |

También es posible hacer doble clic sobre un estado para cambiar su nombre.

## Estructura del proyecto

```text
src/
└── afdpainter/
    ├── Main.java                     # Punto de entrada
    ├── model/
    │   ├── Automaton.java            # Estados, transiciones, alfabeto y modo
    │   ├── State.java                # Nodo del autómata
    │   └── Transition.java           # Transiciones y arcos
    ├── sim/
    │   ├── Simulator.java            # Simulación de AFD y AFN
    │   └── NfaToDfaConverter.java    # Conversión AFN → AFD por subconjuntos
    └── gui/
        ├── MainFrame.java           # Ventana principal
        ├── DrawingPanel.java        # Lienzo y edición con mouse
        ├── ToolBar.java             # Modos de edición y acciones
        ├── PlaybackBar.java         # Controles de reproducción
        ├── TransitionTablePanel.java # Tabla δ de transiciones
        ├── SubsetTablePanel.java    # Tabla de subconjuntos
        ├── Labels.java              # Cadenas y formato visual
        └── Palette.java             # Paleta de colores
```

## Notas de uso

- Cada símbolo del alfabeto debe tener exactamente un carácter.
- Antes de validar una cadena, debe existir un alfabeto guardado y un estado inicial.
- Cambiar entre AFD y AFN borra el autómata actual y pide confirmación.
- El botón **Detener** finaliza la simulación activa; **Reiniciar** conserva la cadena y vuelve al primer paso.
- La aplicación usa el sistema operativo como look and feel cuando es posible.

## Licencia

No se ha incluido un archivo de licencia en este repositorio.
