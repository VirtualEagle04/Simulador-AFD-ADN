# Editor y Simulador de AFD (Java Swing)

Aplicación de escritorio en Java puro (Swing, sin librerías externas de
autómatas) para **dibujar un AFD a mano alzada** (como con un lápiz),
definir el alfabeto Σ, escribir una cadena, y **reproducir paso a paso**
su evaluación sobre el autómata dibujado, resaltando estados y
transiciones.

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

El lienzo es de **dibujo libre, a mano alzada**: para crear estados y
transiciones, **mantén el botón del mouse presionado mientras lo
mueves** trazando la figura; el trazo real que dibujes es lo que queda
en el autómata (no se colocan figuras preestablecidas de una librería).

La interfaz está organizada así:
- **Arriba**: barra de herramientas con los modos de edición.
- **Centro**: el lienzo de dibujo.
- **Derecha (sidebar)**: Alfabeto Σ, Cadena a validar, y el estado de la
  simulación en curso (cadena, consumido, restante, estado actual y
  resultado).
- **Abajo**: controles multimedia de reproducción (Reiniciar, Atrás,
  Reproducir/Pausar, Siguiente, Detener, Velocidad).

1. **Barra superior** (modos de edición, uno a la vez):
   - **Dibujar Estado**: presiona el botón y, sin soltarlo, traza un
     círculo/blob a mano alzada; al soltar, ese trazo se convierte en el
     nuevo estado (con la forma exacta que dibujaste). Se numeran
     automáticamente S0, S1, S2... y **el primer estado creado se marca
     como inicial automáticamente**.
   - **Dibujar Transición**: presiona sobre un estado origen y, sin
     soltar, dibuja la curva a mano hasta el estado destino (o de vuelta
     al mismo estado para un self-loop); al soltar sobre el destino se
     pide el/los símbolo(s) (ej. `a,b`). La curva se recalcula sola si
     luego mueves los estados, y dos transiciones opuestas (A→B y B→A)
     siempre quedan en lados opuestos del arco (nunca se sobreponen).
   - **Marcar Inicial / Marcar Final**: clic sobre un estado (son
     banderas, no figuras, por lo que se activan con un clic).
   - **Mover**: arrastra un estado ya creado; sus transiciones lo siguen.
   - **Borrar**: clic sobre un estado o una transición para eliminarla.
   - **Deshacer**: revierte la última creación de estado/transición.
   - **Limpiar Todo** / **Ayuda**: reinician el lienzo o muestran las
     instrucciones dentro de la app.
   - Doble clic sobre un estado para renombrarlo.

2. **Alfabeto Σ** (sidebar): escribe los símbolos separados por coma o
   espacio (ej. `0,1` o `a b c`) y pulsa **Guardar Alfabeto**.

3. **Determinismo**: al definir una transición, el sistema impide usar
   un símbolo que ya sale de ese mismo estado hacia otro destino distinto.

4. **Cadena** (sidebar): escribe la cadena y pulsa
   **Verificar / Cargar Cadena**. Se valida que pertenezca a Σ y que
   exista un estado inicial.

5. **Reproducción** (panel inferior): Reiniciar (vuelve al paso 0),
   Atrás / Siguiente (paso a paso), Reproducir/Pausar (automático según
   la velocidad), Detener (cierra la simulación por completo). Durante
   la reproducción, el estado activo se resalta en **naranja**; al
   terminar, el autómata se pinta **verde** (aceptada) o **rojo**
   (rechazada, o sin transición para algún símbolo).

## Estructura del proyecto

```
src/afdpainter/
  Main.java                 -> punto de entrada
  model/State.java          -> nodo del AFD (trazo dibujado a mano, radio, inicial/final)
  model/Transition.java     -> arista con símbolo(s) y geometría (bow / ángulo y tamaño de lazo)
  model/Automaton.java      -> estados + transiciones + alfabeto (lógica propia, sin librerías)
  sim/Simulator.java        -> motor de simulación (propio, símbolo a símbolo)
  gui/DrawingPanel.java     -> lienzo de dibujo a mano alzada + geometría de arcos/lazos + resaltado de simulación
  gui/ToolBar.java          -> barra de modos de edición (dibujar, marcar, mover, borrar, deshacer, ayuda)
  gui/PlaybackBar.java      -> controles "multimedia" de reproducción
  gui/MainFrame.java        -> ventana principal: conecta lienzo, sidebar y reproducción
  gui/Palette.java          -> colores (sin azul ni café, según lo solicitado)
```

Toda la lógica de autómatas (determinismo, búsqueda de transiciones,
simulación) está implementada desde cero en `model/` y `sim/`; no se usa
ninguna librería externa de AFD/JFLAP/automatalib, etc.
