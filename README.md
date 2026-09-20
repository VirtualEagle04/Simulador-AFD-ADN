# Editor y Simulador de AFD (Java Swing)

Aplicación de escritorio en Java puro (Swing, sin librerías externas de
autómatas) para **dibujar un AFD a mano** (estilo Paint), definir el
alfabeto Σ, escribir una cadena, y **reproducir paso a paso** su evaluación
sobre el autómata dibujado, resaltando estados y transiciones.

## Requisitos
- JDK 11 o superior (probado con JDK 21).

## Compilar

```bash
cd AFDPainter
javac -d out $(find src -name "*.java")
```

## Ejecutar

```bash
java -cp out afdpainter.Main
```

(En Windows: `javac -d out src\afdpainter\*.java src\afdpainter\model\*.java src\afdpainter\sim\*.java src\afdpainter\gui\*.java` y luego `java -cp out afdpainter.Main`)

## Cómo usarlo

El lienzo es de **dibujo libre, a mano alzada** (como un lápiz): para crear
estados y transiciones, **mantén el botón del mouse presionado mientras lo
mueves** trazando la figura; el trazo real que dibujes es lo que queda en
el autómata (no se colocan figuras preestablecidas de una librería).

1. **Barra superior** (modos de edición, uno a la vez):
   - **Mover**: arrastra los estados ya creados para acomodarlos.
   - **+ Estado**: presiona el botón y, sin soltarlo, traza un círculo/óvalo
     a mano alzada en el lienzo vacío; al soltar, ese trazo se convierte en
     el nuevo estado (con la forma que dibujaste). Si solo haces clic sin
     trazar nada, se crea un círculo estándar en ese punto. Los estados se
     numeran automáticamente S0, S1, S2... y el primero creado se marca
     como inicial automáticamente.
   - **+ Transición**: presiona sobre (o muy cerca de) un estado origen y,
     sin soltar, dibuja a mano alzada la curva hasta el estado destino
     (puede terminar en el mismo estado, para un self-loop); al soltar
     sobre el destino, se pide el/los símbolo(s) (ej: `0` o `0,1`). La
     curva dibujada se reajusta automáticamente si luego mueves los
     estados. Un trazo casi sin movimiento (un clic rápido) genera una
     curva automática suave como respaldo.
   - **Marcar Inicial**: clic sobre un estado para convertirlo en el
     estado inicial (dibuja la flecha de entrada). Es una bandera, no
     una figura, por lo que se activa con un clic.
   - **Alternar Final**: clic sobre un estado para activar/desactivar
     el círculo doble de estado de aceptación (también una bandera).
   - **Eliminar**: clic sobre un estado o una transición para borrarla.
   - **Limpiar Todo**: borra el autómata completo.

2. **Alfabeto Σ**: escribe los símbolos (ej. `0,1`) y pulsa
   **Aplicar Alfabeto**. Si defines una transición con un símbolo que
   no está en Σ, el sistema te preguntará si deseas agregarlo.

3. **Determinismo**: el sistema impide que un mismo estado tenga dos
   transiciones salientes con el mismo símbolo (regla de un AFD).

4. **Cadena**: escribe la cadena a evaluar y pulsa
   **Validar / Ejecutar**. Se valida que todos sus símbolos pertenezcan
   a Σ y que exista un estado inicial.

5. **Panel de reproducción** (parte inferior):
   - `|◀◀` Ir al inicio / `■` Detener (reinicia la reproducción)
   - `◀` Paso atrás
   - `▶` / `⏸` Reproducir / Pausar (automático)
   - `▶` Paso adelante
   - Control deslizante de velocidad
   - Etiqueta con el símbolo consumido, el restante y el estado actual

   Durante la reproducción, el estado activo se resalta en **naranja**;
   al finalizar, el autómata muestra **verde** si la cadena fue
   **aceptada**, o **rojo** si fue **rechazada** (por terminar en un
   estado no final, o por no existir transición para algún símbolo).

## Estructura del proyecto

```
src/afdpainter/
  Main.java                 -> punto de entrada
  model/State.java          -> nodo del AFD
  model/Transition.java     -> arista con símbolo(s)
  model/Automaton.java      -> estados + transiciones + alfabeto (lógica propia, sin librerías)
  sim/Simulator.java        -> motor de simulación (propio, símbolo a símbolo)
  gui/DrawingPanel.java     -> lienzo de dibujo estilo Paint + resaltado de simulación
  gui/ToolBar.java          -> barra de modos de edición
  gui/PlaybackBar.java      -> controles "multimedia" de reproducción
  gui/MainFrame.java        -> ventana principal, conecta todo
  gui/Palette.java          -> colores (sin azul ni café, según lo solicitado)
```

Toda la lógica de autómatas (determinismo, búsqueda de transiciones,
simulación) está implementada desde cero en `model/` y `sim/`; no se usa
ninguna librería externa de AFD/JFLAP/automatalib, etc.
