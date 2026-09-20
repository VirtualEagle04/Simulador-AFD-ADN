package afdpainter.sim;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Motor de simulación (propio, sin librerías externas de autómatas).
 * Sirve tanto para AFD como para AFN: el "estado actual" en cada paso
 * es en realidad un CONJUNTO de estados (para un AFD ese conjunto
 * siempre tiene un único elemento, así que el comportamiento es el
 * mismo de siempre).
 */
public class Simulator {

    public enum Verdict { ACCEPTED, REJECTED, STUCK, NO_INITIAL }

    public static class Result {
        /** path.get(i) = conjunto de estados en que se está tras consumir los primeros i símbolos. */
        public final List<Set<State>> path = new ArrayList<>();
        /** edgesUsed.get(i) = transiciones usadas para llegar a path.get(i) (vacío para i = 0). */
        public final List<Set<Transition>> edgesUsed = new ArrayList<>();
        public String input;
        /** Índice del símbolo en el que no quedó ningún estado activo (-1 si no ocurrió). */
        public int stuckAtIndex = -1;
        public Verdict verdict;
    }

    public static Result simulate(Automaton automaton, String input) {
        Result r = new Result();
        r.input = input;
        State initial = automaton.getInitialState();
        if (initial == null) {
            r.verdict = Verdict.NO_INITIAL;
            return r;
        }

        Set<State> current = automaton.epsilonClosure(singleton(initial));
        r.path.add(current);
        r.edgesUsed.add(new LinkedHashSet<>());

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            Set<State> moved = new LinkedHashSet<>();
            Set<Transition> used = new LinkedHashSet<>();
            for (State s : current) {
                for (Transition t : automaton.getTransitions(s, c)) {
                    moved.add(t.getTo());
                    used.add(t);
                }
            }
            Set<State> closure = automaton.epsilonClosure(moved);
            if (closure.isEmpty()) {
                r.stuckAtIndex = i;
                r.verdict = Verdict.STUCK;
                return r;
            }
            current = closure;
            r.path.add(current);
            r.edgesUsed.add(used);
        }

        boolean anyFinal = false;
        for (State s : current) {
            if (s.isFinalState()) { anyFinal = true; break; }
        }
        r.verdict = anyFinal ? Verdict.ACCEPTED : Verdict.REJECTED;
        return r;
    }

    private static Set<State> singleton(State s) {
        Set<State> set = new LinkedHashSet<>();
        set.add(s);
        return set;
    }
}
