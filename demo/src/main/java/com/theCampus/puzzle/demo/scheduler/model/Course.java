package com.theCampus.puzzle.demo.scheduler.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Course {
    public String id;
    public String name;
    public int students;

    @JsonProperty("professor_id")
    public String professorId;

    @JsonProperty("duration_hours")
    public int durationHours;
}
