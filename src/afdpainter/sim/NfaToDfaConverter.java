package afdpainter.sim;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Construcción de subconjuntos ("subset construction"): convierte un AFN
 * (con o sin transiciones épsilon) en un AFD equivalente. Cada estado del
 * AFD resultante representa un conjunto de estados del AFN original, y se
 * nombra como "{q0,q1}" para dejar visible de dónde viene.
 *
 * Implementación propia, sin librerías externas de autómatas.
 */
public class NfaToDfaConverter {

    public static Automaton convert(Automaton nfa) {
        Automaton dfa = new Automaton();
        dfa.setKind(Automaton.Kind.DFA);
        dfa.setAlphabet(new LinkedHashSet<>(nfa.getAlphabet()));

        State nfaInitial = nfa.getInitialState();
        if (nfaInitial == null) return dfa;

        Map<Set<State>, State> mapping = new LinkedHashMap<>();
        Deque<Set<State>> pending = new ArrayDeque<>();

        Set<State> startSet = nfa.epsilonClosure(singleton(nfaInitial));
        State startDfaState = newDfaState(dfa, startSet, 0);
        startDfaState.setInitial(true);
        mapping.put(startSet, startDfaState);
        pending.add(startSet);

        int count = 1;
        while (!pending.isEmpty()) {
            Set<State> curSet = pending.poll();
            State curDfaState = mapping.get(curSet);

            for (char symbol : nfa.getAlphabet()) {
                Set<State> moved = new LinkedHashSet<>();
                for (State s : curSet) {
                    for (Transition t : nfa.getTransitions(s, symbol)) {
                        moved.add(t.getTo());
                    }
                }
                Set<State> closure = nfa.epsilonClosure(moved);
                if (closure.isEmpty()) continue; // sin transición: no se crea "estado trampa"

                State targetDfaState = mapping.get(closure);
                if (targetDfaState == null) {
                    targetDfaState = newDfaState(dfa, closure, count++);
                    mapping.put(closure, targetDfaState);
                    pending.add(closure);
                }

                Transition existing = dfa.getTransitionBetween(curDfaState, targetDfaState);
                if (existing != null) {
                    existing.addSymbol(symbol);
                } else {
                    Transition t = new Transition(curDfaState, targetDfaState);
                    t.addSymbol(symbol);
                    dfa.getTransitions().add(t);
                }
            }
        }
        return dfa;
    }

    /** Crea un estado del AFD para un subconjunto, con una posición razonable en el lienzo. */
    private static State newDfaState(Automaton dfa, Set<State> subset, int index) {
        double angle = index * 0.9;
        double radius = 70 + index * 42;
        double x = 320 + radius * Math.cos(angle);
        double y = 280 + radius * Math.sin(angle);

        // Se consume el contador interno para mantenerlo coherente, aunque el
        // nombre final del estado sea la etiqueta del subconjunto.
        dfa.nextStateName();

        State s = new State(subsetLabel(subset), x, y);
        boolean anyFinal = false;
        for (State orig : subset) {
            if (orig.isFinalState()) { anyFinal = true; break; }
        }
        s.setFinalState(anyFinal);
        dfa.addState(s);
        return s;
    }

    private static String subsetLabel(Set<State> subset) {
        StringBuilder label = new StringBuilder("{");
        boolean first = true;
        for (State orig : subset) {
            if (!first) label.append(',');
            label.append(orig.getName());
            first = false;
        }
        label.append('}');
        return label.toString();
    }

    private static Set<State> singleton(State s) {
        Set<State> set = new LinkedHashSet<>();
        set.add(s);
        return set;
    }
}
