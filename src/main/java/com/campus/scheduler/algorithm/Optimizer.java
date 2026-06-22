package com.campus.scheduler.algorithm;

import com.campus.scheduler.model.ScheduleResponse.*;
import com.campus.scheduler.model.SchedulerData;
import com.campus.scheduler.model.SchedulerData.Course;
import com.campus.scheduler.model.SchedulerData.Room;
import java.util.*;

public class Optimizer {

    private final SchedulerData data;

    public Optimizer(SchedulerData data) { this.data = data; }

    public StageResult solve(Map<String, String> coloring) {
        long start = System.currentTimeMillis();

        Map<String, Course> courseMap = new HashMap<>();
        data.courses.forEach(c -> courseMap.put(c.id, c));


        Map<String, List<String>> slotToCourses = new LinkedHashMap<>();
        List<String>              uncolored      = new ArrayList<>();

        coloring.forEach((classId, slot) -> {
            if (slot == null) uncolored.add(classId);
            else slotToCourses.computeIfAbsent(slot, k -> new ArrayList<>()).add(classId);
        });

        List<Assignment>       assignments  = new ArrayList<>();
        Map<String, Set<String>> bookedBySlot = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : slotToCourses.entrySet()) {
            String       slot   = entry.getKey();
            Set<String>  booked = bookedBySlot.computeIfAbsent(slot, k -> new HashSet<>());

            List<Course> courses = entry.getValue().stream()
                .map(courseMap::get).filter(Objects::nonNull).toList();

            Map<String, String> roomAssignment = dpAssignRooms(courses, booked);

            for (Course course : courses) {
                Assignment a = new Assignment(course.id, course.name, course.students);
                a.timeSlot = slot;
                String rid = roomAssignment.get(course.id);
                if (rid != null) {
                    Room room = data.rooms.stream().filter(r -> r.id.equals(rid)).findFirst().orElseThrow();
                    a.roomId      = rid;
                    a.wastedSeats = room.capacity - course.students;
                    a.status      = "scheduled";
                    a.note        = a.wastedSeats == 0 ? "Perfect Fit" : "Wasted " + a.wastedSeats + " seats";
                    booked.add(rid);
                } else {
                    a.status = "unscheduled";
                    a.note   = "No suitable room available in this time slot";
                }
                assignments.add(a);
            }
        }


        uncolored.forEach(id -> {
            Course course = courseMap.get(id);
            if (course == null) return;
            Assignment a = new Assignment(course.id, course.name, course.students);
            a.status = "unscheduled";
            a.note   = "Over-constrained in Stage 2 — no conflict-free time slot found";
            assignments.add(a);
        });

        int  scheduled  = (int) assignments.stream().filter(a -> "scheduled".equals(a.status)).count();
        int  totalWaste = assignments.stream().mapToInt(a -> a.wastedSeats).sum();

        StageResult r    = new StageResult();
        r.stage           = "Stage 3";
        r.algorithm       = "Dynamic Programming (Best-Fit DP)";
        r.complexity      = "O(T · Cₜ · R)";
        r.spaceComplexity = "O(Cₜ · R)";
        r.description     = "Time slots fixed by Stage 2. For each slot a DP table "
                          + "dp[i][j] = min waste assigning first i courses using first j rooms "
                          + "is computed. Recurrence: skip room j or assign it to course i. "
                          + "Replaces O(R!) brute-force with an O(Cₜ·R) table per slot.";
        r.assignments = assignments;
        r.scheduled   = scheduled;
        r.unscheduled = assignments.size() - scheduled;
        r.totalWaste  = totalWaste;
        r.timeMs      = System.currentTimeMillis() - start;
        return r;
    }

    private Map<String, String> dpAssignRooms(List<Course> courses, Set<String> bookedRooms) {
        /* Sort: largest course first, smallest free room first */
        List<Course> sc = courses.stream()
            .sorted(Comparator.comparingInt((Course c) -> c.students).reversed()).toList();
        List<Room>   fr = data.rooms.stream()
            .filter(r -> !bookedRooms.contains(r.id))
            .sorted(Comparator.comparingInt(r -> r.capacity)).toList();

        int n = sc.size(), m = fr.size();
        if (n == 0 || m == 0) return Collections.emptyMap();

        final int INF = Integer.MAX_VALUE / 2;
        int[][][] parent = new int[n + 1][m + 1][3];
        int[][]   dp     = new int[n + 1][m + 1];

        for (int[] row : dp) Arrays.fill(row, INF);
        for (int[][] plane : parent) for (int[] row : plane) Arrays.fill(row, -1);
        dp[0][0] = 0;

        for (int i = 1; i <= n; i++) {
            Course course = sc.get(i - 1);
            for (int j = i; j <= m; j++) {
                // Option A: skip room j-1 for course i
                if (dp[i][j - 1] < INF && dp[i][j - 1] < dp[i][j]) {
                    dp[i][j]     = dp[i][j - 1];
                    parent[i][j] = new int[]{i, j - 1, -1};
                }
                // Option B: assign room j-1 to course i
                Room room = fr.get(j - 1);
                if (room.capacity >= course.students && dp[i - 1][j - 1] < INF) {
                    int candidate = dp[i - 1][j - 1] + (room.capacity - course.students);
                    if (candidate < dp[i][j]) {
                        dp[i][j]     = candidate;
                        parent[i][j] = new int[]{i - 1, j - 1, j - 1};
                    }
                }
            }
        }

        int bestJ = n, bestWaste = INF;
        for (int j = n; j <= m; j++)
            if (dp[n][j] < bestWaste) { bestWaste = dp[n][j]; bestJ = j; }

        Map<String, String> result = new HashMap<>();
        if (bestWaste < INF) {
            int ci = n, ri = bestJ;
            while (ci > 0 && ri >= 0) {
                int[] p = parent[ci][ri];
                if (p[0] < 0) break;
                if (p[2] >= 0) result.put(sc.get(ci - 1).id, fr.get(p[2]).id);
                ci = p[0];
                ri = p[1];
            }
        }
        return result;
    }
}
