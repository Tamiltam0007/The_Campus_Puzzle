package com.campus.scheduler.algorithm;

import com.campus.scheduler.model.SchedulerData;
import java.util.*;

public class ConflictGraph {

    private final Map<String, Set<String>> adj = new LinkedHashMap<>();

    public ConflictGraph(SchedulerData data) {
        data.courses.forEach(c -> adj.put(c.id, new HashSet<>()));
        addProfessorEdges(data);
        addGroupEdges(data);
    }


    private void addProfessorEdges(SchedulerData data) {
        Map<String, List<String>> byProf = new HashMap<>();
        data.courses.forEach(c ->
            byProf.computeIfAbsent(c.professorId, k -> new ArrayList<>()).add(c.id));
        byProf.values().forEach(this::addClique);
    }

    private void addGroupEdges(SchedulerData data) {
        data.studentGroups.values().forEach(g -> {
            List<String> valid = g.classes.stream().filter(adj::containsKey).toList();
            addClique(valid);
        });
    }

    private void addClique(List<String> ids) {
        for (int i = 0; i < ids.size(); i++)
            for (int j = i + 1; j < ids.size(); j++) {
                String a = ids.get(i), b = ids.get(j);
                if (adj.containsKey(a) && adj.containsKey(b)) {
                    adj.get(a).add(b);
                    adj.get(b).add(a);
                }
            }
    }

    public Set<String> neighbors(String id)  { return adj.getOrDefault(id, Set.of()); }
    public int         degree(String id)     { return adj.getOrDefault(id, Set.of()).size(); }
    public Set<String> nodes()               { return adj.keySet(); }

    public int edgeCount() {
        return adj.values().stream().mapToInt(Set::size).sum() / 2;
    }

    public int maxDegree() {
        return adj.values().stream().mapToInt(Set::size).max().orElse(0);
    }

    public double avgDegree() {
        if (adj.isEmpty()) return 0;
        return adj.values().stream().mapToInt(Set::size).average().orElse(0);
    }

    public Map<String, Integer> degreeMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        adj.forEach((k, v) -> m.put(k, v.size()));
        return m;
    }

    public Map<String, List<String>> adjacencyForJson() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        adj.forEach((k, v) -> m.put(k, new ArrayList<>(v)));
        return m;
    }
}
