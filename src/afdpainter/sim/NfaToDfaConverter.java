package afdpainter.sim;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
 * Los estados del AFD resultante se nombran de forma simple y coherente
 * (k0, k1, k2, ...) en el mismo orden en que se descubren (recorrido en
 * anchura desde el estado inicial), en vez de mostrar el subconjunto
 * completo. Se hace en dos fases: primero se descubren todos los
 * subconjuntos y sus transiciones (sobre índices), y solo al final,
 * conociendo el total, se ubican los estados en un círculo alrededor
 * del centro del AFN original y se crean las transiciones reales. Así
 * se evita que los nodos queden amontonados y, al seguir el orden de
 * descubrimiento (BFS) alrededor del círculo, los estados conectados
 * quedan cerca entre sí, reduciendo el cruce de flechas.
 *
 * Implementación propia, sin librerías externas de autómatas.
 */
public class NfaToDfaConverter {

    /**
     * Resultado completo de la conversión: el AFD ya renombrado (k0, k1, ...),
     * más la información cruda de la construcción de subconjuntos (para poder
     * mostrar también la tabla "clásica" en notación {q0,q1}).
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

        // ---------- Fase 1: descubrir subconjuntos y transiciones (por índice) ----------
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

        // ---------- Fase 2: ubicar los estados y construir el AFD real ----------
        int n = order.size();
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
            for (State orig : order.get(i)) {
                if (orig.isFinalState()) { anyFinal = true; break; }
            }
            s.setFinalState(anyFinal);
            dfa.addState(s);
            dfaStates[i] = s;
        }
        dfaStates[0].setInitial(true);

        for (int i = 0; i < n; i++) {
            Map<Character, Integer> row = deltaByIndex.get(i);
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

        return new ConversionResult(dfa, order, deltaByIndex);
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
