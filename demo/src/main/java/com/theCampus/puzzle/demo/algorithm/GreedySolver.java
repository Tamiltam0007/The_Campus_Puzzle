package com.theCampus.puzzle.demo.algorithm;

import com.theCampus.puzzle.demo.scheduler.model.Course;
import com.theCampus.puzzle.demo.scheduler.model.Room;
import com.theCampus.puzzle.demo.scheduler.model.SchedulerData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GreedySolver {

    // Json data schduler one
    private final SchedulerData data;
//    private final ConflictGraph graph;

    public GreedySolver(SchedulerData data) {
        this.data = data;
//        this.graph = new ConflictGraph(data);
    }

    public StageResult solve() {
        long start = System.currentTimeMillis();

        //Sort classes: largest enrollment first
        List<Course> courseListSorted = new ArrayList<>(data.courses);
        courseListSorted.sort(Comparator.comparingInt((Course c) -> c.students)); //bigger class capacity

        //Sort rooms: smallest capacity first (best-fit)
        List<Room> roomsListSorted = new ArrayList<>(data.rooms);
        roomsListSorted.sort(Comparator.comparingInt(r -> r.capacity));// room capcity


        /* Booking state */
        Map<String, Set<String>> roomBookedAt = new HashMap<>();   // roomId  → {slots}
        Map<String, Set<String>> slotOccupants = new HashMap<>();   // slot    → {classIds}
        data.rooms.forEach(r -> roomBookedAt.put(r.id, new HashSet<>()));
        data.timeSlots.forEach(ts -> slotOccupants.put(ts, new HashSet<>()));

        List<Assignment> assignments = new ArrayList<>();

        for (Course course : sorted) {
            Assignment a = new Assignment(course.id, course.name, course.students);
            boolean placed = false;

            outer:
            for (String slot : data.timeSlots) {
                /* Skip slot if any conflicting class is already there */
                Set<String> conflictsInSlot = new HashSet<>(graph.neighbors(course.id));
                conflictsInSlot.retainAll(slotOccupants.get(slot));
                if (!conflictsInSlot.isEmpty()) continue;

                for (Room room : sortedRooms) {
                    if (room.capacity < course.students) continue;
                    if (roomBookedAt.get(room.id).contains(slot)) continue;

                    a.timeSlot = slot;
                    a.roomId = room.id;
                    a.wastedSeats = room.capacity - course.students;
                    a.status = "scheduled";
                    a.note = a.wastedSeats == 0
                            ? "Perfect Fit"
                            : "Wasted " + a.wastedSeats + " seats";

                    roomBookedAt.get(room.id).add(slot);
                    slotOccupants.get(slot).add(course.id);
                    placed = true;
                    break outer;
                }
            }

            if (!placed) {
                a.status = "unscheduled";
                a.note = "No valid slot / room combination found";
            }
            assignments.add(a);
        }

        return buildResult(assignments, System.currentTimeMillis() - start);
    }

    private StageResult buildResult(List<Assignment> assignments, long ms) {
        StageResult r = new StageResult();
        r.stage = "Stage 1";
        r.algorithm = "Greedy (Sort by Enrollment)";
        r.complexity = "O(C·T·R)";
        r.spaceComplexity = "O(C + T·R)";
        r.description = "Sort classes by enrollment descending — large classes are hardest to "
                + "fit so they are placed first. Assign each to the first valid (slot, room) "
                + "pair that satisfies all hard constraints. Room is chosen by best-fit "
                + "(smallest sufficient capacity). Fastest algorithm; good baseline.";
        r.assignments = assignments;
        r.scheduled = (int) assignments.stream().filter(a -> "scheduled".equals(a.status)).count();
        r.unscheduled = assignments.size() - r.scheduled;
        r.totalWaste = assignments.stream().mapToInt(a -> a.wastedSeats).sum();
        r.timeMs = ms;
        return r;
    }
}
