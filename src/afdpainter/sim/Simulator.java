package afdpainter.sim;

import afdpainter.model.Automaton;
import afdpainter.model.State;
import afdpainter.model.Transition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Simulator {

    public enum Verdict { ACCEPTED, REJECTED, STUCK, NO_INITIAL }

    public static class Result {
        public final List<Set<State>> path = new ArrayList<>();
        public final List<Set<Transition>> edgesUsed = new ArrayList<>();
        public String input;
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
