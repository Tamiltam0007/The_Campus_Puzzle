package com.campus.scheduler.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;


public class SchedulerData {

    @JsonProperty("classes")
    public List<Course> courses = new ArrayList<>();

    public List<Room> rooms = new ArrayList<>();

    @JsonProperty("time_slots")
    public List<String> timeSlots = new ArrayList<>();

    @JsonProperty("student_groups")
    public Map<String, StudentGroup> studentGroups = new LinkedHashMap<>();


    public static class Course {
        public String id;
        public String name;
        public int students;

        @JsonProperty("professor_id")
        public String professorId;

        @JsonProperty("duration_hours")
        public int durationHours;
    }

    public static class Room {
        public String id;
        public String name;
        public int capacity;
    }

    public static class StudentGroup {
        public String name;
        public List<String> classes = new ArrayList<>();
    }
}
