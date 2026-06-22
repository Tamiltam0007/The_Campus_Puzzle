package com.campus.scheduler.model;

import java.util.*;


public class ScheduleResponse {

    public StageResult greedy;
    public StageResult graph;
    public StageResult dp;
    public StageResult backtracking;
    public List<Assignment> finalSchedule = new ArrayList<>();
    public List<String> manualIntervention = new ArrayList<>();
    public GraphInfo conflictGraph;


    public static class Assignment {
        public String classId;
        public String className;
        public int enrolled;
        public String timeSlot;
        public String roomId;
        public int wastedSeats;
        public String status;
        public String note;

        public Assignment(String classId, String className, int enrolled) {
            this.classId   = classId;
            this.className = className;
            this.enrolled  = enrolled;
        }
    }

    public static class StageResult {
        public String stage;
        public String algorithm;
        public String complexity;
        public String spaceComplexity;
        public String description;
        public List<Assignment> assignments = new ArrayList<>();
        public int scheduled;
        public int unscheduled;
        public int totalWaste;
        public long timeMs;
        public GraphInfo graphStats;
    }

    public static class GraphInfo {
        public int nodes;
        public int edges;
        public int maxDegree;
        public double avgDegree;
        public Map<String, Integer> degreeMap = new LinkedHashMap<>();
        public Map<String, List<String>> adjacency = new LinkedHashMap<>();
    }
}
