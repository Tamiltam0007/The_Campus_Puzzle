package com.campus.scheduler.controller;

import com.campus.scheduler.algorithm.*;
import com.campus.scheduler.model.ScheduleResponse;
import com.campus.scheduler.model.ScheduleResponse.*;
import com.campus.scheduler.model.SchedulerData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class SchedulerController {

    private final ObjectMapper  mapper = new ObjectMapper();
    private SchedulerData       cachedData;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Campus Scheduler"));
    }

    @GetMapping("/schedule")
    public ResponseEntity<ScheduleResponse> runSchedule() throws Exception {
        SchedulerData data = loadData();

        long t0 = System.currentTimeMillis();
        StageResult greedy = new GreedySolver(data).solve();

        t0 = System.currentTimeMillis();
        GraphEngine ge      = new GraphEngine(data);
        Map<String, String> coloring = ge.colorGraph();
        StageResult graph   = ge.buildResult(coloring, greedy, System.currentTimeMillis() - t0);

        StageResult dp = new Optimizer(data).solve(coloring);


        StageResult bt = new Backtracker(data).solve(8_000);


        ScheduleResponse report = new ScheduleResponse();
        report.greedy       = greedy;
        report.graph        = graph;
        report.dp           = dp;
        report.backtracking = bt;


        report.finalSchedule = dp.assignments;


        report.manualIntervention = dp.assignments.stream()
            .filter(a -> "unscheduled".equals(a.status))
            .map(a -> a.classId + " (" + a.enrolled + " students): " + a.note)
            .collect(Collectors.toList());

        report.conflictGraph = graph.graphStats;

        return ResponseEntity.ok(report);
    }


    private SchedulerData loadData() throws Exception {
        if (cachedData != null) return cachedData;
        try (InputStream is = new ClassPathResource("constraints.json").getInputStream()) {
            cachedData = mapper.readValue(is, SchedulerData.class);
        }
        return cachedData;
    }
}
