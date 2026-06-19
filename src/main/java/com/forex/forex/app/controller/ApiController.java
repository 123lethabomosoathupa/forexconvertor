package com.forex.forexapp.controller;

import com.forex.forexapp.model.ConversionLog;
import com.forex.forexapp.service.ConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Forex API", description = "Currency conversion and history endpoints")
public class ApiController {

    private final ConversionService conversionService;

    public ApiController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/convert")
    @Operation(summary = "Convert currency",
               description = "Converts an amount and saves a log entry")
    public ResponseEntity<?> convert(@RequestParam String from,
                                      @RequestParam String to,
                                      @RequestParam double amount) {
        try {
            return ResponseEntity.ok(conversionService.convert(
                from.toUpperCase(), to.toUpperCase(), amount));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    @Operation(summary = "Get recent history",
               description = "Returns last 10 conversions for the logged-in user")
    public ResponseEntity<List<ConversionLog>> history() {
        return ResponseEntity.ok(conversionService.getRecentHistory());
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}