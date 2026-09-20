package afdpainter.sim;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Construcción de subconjuntos ("subset construction"): convierte un AFN
 * (con o sin transiciones épsilon) en un AFD equivalente.
 *
 * Los estados del AFD resultante se nombran k0, k1, k2, ... siguiendo la
 * convención "de clase": primero los subconjuntos que coinciden con un
 * único estado del AFN original, en el mismo orden en que esos estados
 * fueron dibujados (q0, q1, q2, ...), y solo al final los subconjuntos
 * combinados (los que de verdad son producto del no-determinismo), en el
 * orden en que se descubrieron. Así k0..k(m-1) corresponden uno a uno con
 * q0..q(m-1) siempre que sean alcanzables, igual que en la construcción
 * hecha a mano en papel.
 *
 * El algoritmo se hace en tres fases: (1) se descubren todos los
 * subconjuntos alcanzables junto con sus transiciones, usando índices de
 * descubrimiento; (2) se renumeran esos índices según la convención
 * anterior; (3) ya con el total y el orden final conocidos, se ubican los
 * estados en un círculo alrededor del centro del AFN original y se crean
 * las transiciones reales. Ubicarlos en círculo evita que los nodos queden
 * amontonados, y seguir un orden con sentido (en vez de uno arbitrario)
 * ayuda a que los estados relacionados queden cerca, reduciendo el cruce
 * de flechas.
 *
 * Implementación propia, sin librerías externas de autómatas.
 */
public class NfaToDfaConverter {

    /**
     * Resultado completo de la conversión: el AFD ya renombrado (k0, k1, ...),
     * más la información cruda de la construcción de subconjuntos (para poder
     * mostrar también la tabla "clásica" en notación {q0,q1}), ya en el mismo
     * orden final que el AFD renombrado.
     */
    public static class ConversionResult {
        public final Automaton dfa;
        /** subsetOrder.get(i) = conjunto de estados del AFN que representa el estado ki del AFD. */
        public final List<Set<State>> subsetOrder;
        /** deltaByIndex.get(i).get(symbol) = índice (en subsetOrder) al que se llega desde i con symbol. */
        public final List<Map<Character, Integer>> deltaByIndex;

        ConversionResult(Automaton dfa, List<Set<State>> subsetOrder, List<Map<Character, Integer>> deltaByIndex) {
            this.dfa = dfa;
            this.subsetOrder = subsetOrder;
            this.deltaByIndex = deltaByIndex;
        }
    }

    public static ConversionResult convert(Automaton nfa) {
        Automaton dfa = new Automaton();
        dfa.setKind(Automaton.Kind.DFA);
        dfa.setAlphabet(new LinkedHashSet<>(nfa.getAlphabet()));

        State nfaInitial = nfa.getInitialState();
        if (nfaInitial == null) return new ConversionResult(dfa, new ArrayList<>(), new ArrayList<>());

        // ---------- Fase 1: descubrir subconjuntos y transiciones (por índice de descubrimiento) ----------
        List<Set<State>> order = new ArrayList<>();
        Map<Set<State>, Integer> indexOf = new LinkedHashMap<>();
        List<Map<Character, Integer>> deltaByIndex = new ArrayList<>();

        Set<State> startSet = nfa.epsilonClosure(singleton(nfaInitial));
        order.add(startSet);
        indexOf.put(startSet, 0);
        deltaByIndex.add(null);

        Deque<Integer> pending = new ArrayDeque<>();
        pending.add(0);

        while (!pending.isEmpty()) {
            int curIdx = pending.poll();
            Set<State> curSet = order.get(curIdx);
            Map<Character, Integer> deltaRow = new LinkedHashMap<>();

            for (char symbol : nfa.getAlphabet()) {
                Set<State> moved = new LinkedHashSet<>();
                for (State s : curSet) {
                    for (Transition t : nfa.getTransitions(s, symbol)) {
                        moved.add(t.getTo());
                    }
                }
                Set<State> closure = nfa.epsilonClosure(moved);
                if (closure.isEmpty()) continue; // sin transición: no se crea "estado trampa"

                Integer targetIdx = indexOf.get(closure);
                if (targetIdx == null) {
                    targetIdx = order.size();
                    order.add(closure);
                    indexOf.put(closure, targetIdx);
                    deltaByIndex.add(null);
                    pending.add(targetIdx);
                }
                deltaRow.put(symbol, targetIdx);
            }
            deltaByIndex.set(curIdx, deltaRow);
        }

        int n = order.size();

        // ---------- Fase 2: renumerar según la convención "de papel" ----------
        // Primero, cada subconjunto que sea EXACTAMENTE {q_i} para algún estado
        // original q_i, en el mismo orden en que q_i fue dibujado. Luego, el
        // resto de subconjuntos (los combinados) en el orden en que se
        // descubrieron.
        int[] oldToNew = new int[n];
        Arrays.fill(oldToNew, -1);
        List<Integer> newToOld = new ArrayList<>();

        for (State s : nfa.getStates()) {
            Integer oldIdx = indexOf.get(singleton(s));
            if (oldIdx != null && oldToNew[oldIdx] == -1) {
                oldToNew[oldIdx] = newToOld.size();
                newToOld.add(oldIdx);
            }
        }
        for (int oldIdx = 0; oldIdx < n; oldIdx++) {
            if (oldToNew[oldIdx] == -1) {
                oldToNew[oldIdx] = newToOld.size();
                newToOld.add(oldIdx);
            }
        }

        List<Set<State>> renumberedOrder = new ArrayList<>(Collections.nCopies(n, null));
        List<Map<Character, Integer>> renumberedDelta = new ArrayList<>(Collections.nCopies(n, null));
        for (int oldIdx = 0; oldIdx < n; oldIdx++) {
            int newIdx = oldToNew[oldIdx];
            renumberedOrder.set(newIdx, order.get(oldIdx));

            Map<Character, Integer> oldRow = deltaByIndex.get(oldIdx);
            Map<Character, Integer> newRow = new LinkedHashMap<>();
            if (oldRow != null) {
                for (Map.Entry<Character, Integer> e : oldRow.entrySet()) {
                    newRow.put(e.getKey(), oldToNew[e.getValue()]);
                }
            }
            renumberedDelta.set(newIdx, newRow);
        }
        int initialIdx = oldToNew[0];

        // ---------- Fase 3: ubicar los estados y construir el AFD real ----------
        double[] center = centroidOf(nfa);
        double centerX = center[0], centerY = center[1];

        double spacing = 130; // separación deseada entre estados vecinos en el círculo
        double radius = (n <= 1) ? 0 : Math.max(110, (spacing * n) / (2 * Math.PI));

        State[] dfaStates = new State[n];
        for (int i = 0; i < n; i++) {
            double angle = (n <= 1) ? 0 : (2 * Math.PI * i / n) - Math.PI / 2;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            State s = new State("k" + i, x, y);
            boolean anyFinal = false;
            for (State orig : renumberedOrder.get(i)) {
                if (orig.isFinalState()) { anyFinal = true; break; }
            }
            s.setFinalState(anyFinal);
            dfa.addState(s);
            dfaStates[i] = s;
        }
        dfaStates[initialIdx].setInitial(true);

        for (int i = 0; i < n; i++) {
            Map<Character, Integer> row = renumberedDelta.get(i);
            if (row == null) continue;
            for (Map.Entry<Character, Integer> e : row.entrySet()) {
                char symbol = e.getKey();
                State from = dfaStates[i];
                State to = dfaStates[e.getValue()];

                Transition existing = dfa.getTransitionBetween(from, to);
                if (existing != null) {
                    existing.addSymbol(symbol);
                } else {
                    Transition t = new Transition(from, to);
                    t.addSymbol(symbol);
                    dfa.getTransitions().add(t);
                }
            }
        }

        return new ConversionResult(dfa, renumberedOrder, renumberedDelta);
    }

    /** Centro (promedio) de las posiciones de los estados del AFN original, para ubicar el AFD cerca. */
    private static double[] centroidOf(Automaton nfa) {
        if (nfa.getStates().isEmpty()) return new double[]{320, 280};
        double sx = 0, sy = 0;
        for (State s : nfa.getStates()) { sx += s.getX(); sy += s.getY(); }
        int n = nfa.getStates().size();
        return new double[]{sx / n, sy / n};
    }

    private static Set<State> singleton(State s) {
        Set<State> set = new LinkedHashSet<>();
        set.add(s);
        return set;
    }
}
