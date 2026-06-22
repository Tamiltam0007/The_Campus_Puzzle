package com.campus.scheduler.algorithm;

import com.campus.scheduler.model.ScheduleResponse.*;
import com.campus.scheduler.model.SchedulerData;
import com.campus.scheduler.model.SchedulerData.Course;
import java.util.*;


public class GraphEngine {

    private final SchedulerData data;
    private ConflictGraph       graph;   // retained so callers can reuse the graph

    public GraphEngine(SchedulerData data) {
        this.data = data;
    }


    public Map<String, String> colorGraph() {
        graph = new ConflictGraph(data);

        List<String> nodes = new ArrayList<>(graph.nodes());
        nodes.sort(Comparator.comparingInt(graph::degree).reversed());

        Map<String, String> coloring = new LinkedHashMap<>();
        for (String node : nodes) {
            /* Colors already claimed by colored neighbors */
            Set<String> usedColors = new HashSet<>();
            for (String nb : graph.neighbors(node))
                if (coloring.containsKey(nb) && coloring.get(nb) != null)
                    usedColors.add(coloring.get(nb));

            String assigned = null;
            for (String slot : data.timeSlots) {
                if (!usedColors.contains(slot)) { assigned = slot; break; }
            }
            coloring.put(node, assigned);
        }
        return coloring;
    }

    public ConflictGraph getGraph() { return graph; }


    public StageResult buildResult(Map<String, String> coloring, StageResult greedyResult, long ms) {
        Map<String, Course> courseMap = new HashMap<>();
        data.courses.forEach(c -> courseMap.put(c.id, c));

        List<Assignment> assignments = new ArrayList<>();
        coloring.forEach((classId, slot) -> {
            Course course = courseMap.get(classId);
            if (course == null) return;
            Assignment a = new Assignment(course.id, course.name, course.students);
            if (slot != null) {
                a.timeSlot = slot;
                a.status   = "scheduled";
                a.note     = "Conflict-free slot assigned via Welsh–Powell coloring";
            } else {
                a.status = "unscheduled";
                a.note   = "Over-constrained: all time slots blocked by conflicting classes";
            }
            assignments.add(a);
        });

        int scheduled   = (int) assignments.stream().filter(a -> "scheduled".equals(a.status)).count();
        int improvement = greedyResult != null ? scheduled - greedyResult.scheduled : 0;


        GraphInfo gi  = new GraphInfo();
        gi.nodes      = graph.nodes().size();
        gi.edges      = graph.edgeCount();
        gi.maxDegree  = graph.maxDegree();
        gi.avgDegree  = Math.round(graph.avgDegree() * 10.0) / 10.0;
        gi.degreeMap  = graph.degreeMap();
        gi.adjacency  = graph.adjacencyForJson();

        StageResult r    = new StageResult();
        r.stage           = "Stage 2";
        r.algorithm       = "Welsh–Powell Graph Coloring";
        r.complexity      = "O(C² + C·T)";
        r.spaceComplexity = "O(C²)";
        r.description     = "Build a conflict graph: nodes = classes, edges = shared professor or "
                          + "student group. Welsh–Powell sorts nodes by degree descending and assigns "
                          + "the lowest-index time slot not already used by any neighbor. "
                          + "Improvement over greedy: " + improvement + " more classes conflict-free.";
        r.assignments = assignments;
        r.scheduled   = scheduled;
        r.unscheduled = assignments.size() - scheduled;
        r.totalWaste  = 0;
        r.timeMs      = ms;
        r.graphStats  = gi;
        return r;
    }
}
