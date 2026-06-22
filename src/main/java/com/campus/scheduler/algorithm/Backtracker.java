package com.campus.scheduler.algorithm;

import com.campus.scheduler.model.ScheduleResponse.*;
import com.campus.scheduler.model.SchedulerData;
import com.campus.scheduler.model.SchedulerData.Course;
import com.campus.scheduler.model.SchedulerData.Room;
import java.util.*;

public class Backtracker {

    private static final int LOOKAHEAD = 3;

    private final SchedulerData data;
    private final ConflictGraph graph;
    private final List<Course>  orderedCourses;
    private final Map<String, Room> roomById = new HashMap<>();


    private Map<String, String[]> bestState = new LinkedHashMap<>();
    private int  bestCount      = 0;
    private int  nodesExplored  = 0;
    private long deadline;

    public Backtracker(SchedulerData data) {
        this.data  = data;
        this.graph = new ConflictGraph(data);
        data.rooms.forEach(r -> roomById.put(r.id, r));

        this.orderedCourses = new ArrayList<>(data.courses);
        this.orderedCourses.sort(
            Comparator.comparingInt((Course c) -> graph.degree(c.id)).reversed()
                      .thenComparingInt((Course c) -> c.students).reversed()
        );
    }

    public StageResult solve(long timeLimitMs) {
        long start = System.currentTimeMillis();
        deadline = start + timeLimitMs;

        backtrack(new LinkedHashMap<>(), 0);

        Map<String, Course> courseMap = new HashMap<>();
        data.courses.forEach(c -> courseMap.put(c.id, c));

        Set<String>        scheduledIds = bestState.keySet();
        List<Assignment>   assignments  = new ArrayList<>();

        bestState.forEach((classId, sa) -> {
            Course course = courseMap.get(classId);
            if (course == null) return;
            Room   room   = roomById.get(sa[1]);
            Assignment a  = new Assignment(course.id, course.name, course.students);
            a.timeSlot    = sa[0];
            a.roomId      = sa[1];
            a.wastedSeats = room.capacity - course.students;
            a.status      = "scheduled";
            a.note        = a.wastedSeats == 0 ? "Perfect Fit" : "Wasted " + a.wastedSeats + " seats";
            assignments.add(a);
        });

        orderedCourses.forEach(course -> {
            if (!scheduledIds.contains(course.id)) {
                Assignment a = new Assignment(course.id, course.name, course.students);
                a.status = "unscheduled";
                a.note   = "Backtracking exhausted all valid assignments — flagged for manual intervention";
                assignments.add(a);
            }
        });

        int  scheduled  = (int) assignments.stream().filter(a -> "scheduled".equals(a.status)).count();
        int  totalWaste = assignments.stream().mapToInt(a -> a.wastedSeats).sum();

        StageResult r    = new StageResult();
        r.stage           = "Stage 4";
        r.algorithm       = "Backtracking (MRV + Forward Checking)";
        r.complexity      = "O(bᵈ) pruned";
        r.spaceComplexity = "O(C · d)";
        r.description     = "CSP solver. MRV orders classes by conflict degree so the hardest "
                          + "variable is assigned first. Forward checking (lookahead = " + LOOKAHEAD
                          + ") prunes branches that would leave a future class with no valid option. "
                          + "Explored " + nodesExplored + " nodes. "
                          + "Best-effort: unplaceable classes are flagged for manual intervention.";
        r.assignments = assignments;
        r.scheduled   = scheduled;
        r.unscheduled = assignments.size() - scheduled;
        r.totalWaste  = totalWaste;
        r.timeMs      = System.currentTimeMillis() - start;
        return r;
    }


    private boolean backtrack(Map<String, String[]> assignments, int depth) {
        nodesExplored++;


        if (assignments.size() > bestCount) {
            bestCount = assignments.size();
            bestState = new LinkedHashMap<>(assignments);
        }

        if (depth == orderedCourses.size()) return true;           // complete solution
        if (System.currentTimeMillis() > deadline)  return false;  // time limit

        Course       course = orderedCourses.get(depth);
        List<String[]> domain = buildDomain(course, assignments);


        if (domain.isEmpty()) return backtrack(assignments, depth + 1);

        for (String[] option : domain) {
            assignments.put(course.id, option);

            if (forwardCheck(assignments, depth + 1) && backtrack(assignments, depth + 1))
                return true;

            assignments.remove(course.id);
            if (System.currentTimeMillis() > deadline) return false;
        }


        return backtrack(assignments, depth + 1);
    }


    private List<String[]> buildDomain(Course course, Map<String, String[]> assignments) {
        Set<String>         blockedSlots = new HashSet<>();
        Map<String, Set<String>> roomBookings = new HashMap<>();

        assignments.forEach((otherId, sa) -> {
            if (graph.neighbors(course.id).contains(otherId)) blockedSlots.add(sa[0]);
            roomBookings.computeIfAbsent(sa[1], k -> new HashSet<>()).add(sa[0]);
        });

        List<Room> sortedRooms = data.rooms.stream()
            .sorted(Comparator.comparingInt(r -> r.capacity)).toList();

        List<String[]> domain = new ArrayList<>();
        for (String slot : data.timeSlots) {
            if (blockedSlots.contains(slot)) continue;
            for (Room room : sortedRooms) {
                if (room.capacity < course.students)                               continue;
                if (roomBookings.getOrDefault(room.id, Set.of()).contains(slot))   continue;
                domain.add(new String[]{slot, room.id});
                break;
            }
        }
        return domain;
    }

    private boolean forwardCheck(Map<String, String[]> assignments, int from) {
        int limit = Math.min(from + LOOKAHEAD, orderedCourses.size());
        for (int i = from; i < limit; i++) {
            Course c = orderedCourses.get(i);
            if (!assignments.containsKey(c.id) && buildDomain(c, assignments).isEmpty())
                return false;
        }
        return true;
    }
}
