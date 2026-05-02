package com.theCampus.puzzle.demo.scheduler.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

}
