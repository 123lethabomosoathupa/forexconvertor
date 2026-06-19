package com.forex.forexapp.controller;

import com.forex.forexapp.service.ConversionService;
import com.forex.forexapp.service.SparklineService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class SparklineController {

    private final ConversionService conversionService;
    private final SparklineService sparklineService;

    public SparklineController(ConversionService conversionService,
            SparklineService sparklineService) {
        this.conversionService = conversionService;
        this.sparklineService = sparklineService;
    }

    /**
     * GET /sparkline?from=USD&to=ZAR Returns a PNG image of the rate history
     * sparkline for this pair. Called as
     * <img th:src="@{/sparkline(from=${from},to=${to})}">
     */
    @GetMapping(value = "/sparkline", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> sparkline(@RequestParam String from,
            @RequestParam String to) {
        try {
            List<Double> rates = conversionService.getRatesForSparkline(
                    from.toUpperCase(), to.toUpperCase());
            byte[] png;
            if (rates.isEmpty()) {
                png = sparklineService.emptyImage();
            } else {
                png = sparklineService.generateSparkline(
                        from.toUpperCase(), to.toUpperCase(), rates);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(png);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
