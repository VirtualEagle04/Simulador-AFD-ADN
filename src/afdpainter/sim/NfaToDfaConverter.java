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

public class NfaToDfaConverter {

    public static class ConversionResult {
        public final Automaton dfa;
        public final List<Set<State>> subsetOrder;
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
                if (closure.isEmpty()) continue;

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

        double[] center = centroidOf(nfa);
        double centerX = center[0], centerY = center[1];

        double spacing = 130;
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
