package com.gngm.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
public class MapController {
    @GetMapping("/walls")
    public List<Map<String, Object>> getWalls() {
        // Layout: 8 L-shaped corners, 4 short horizontal walls, central rectangle, and 4 L-shaped walls farther from center (open toward center)
        return List.of(
            // Top left L
            Map.of("x", 0, "y", 0, "width", 40, "height", 200),
            Map.of("x", 0, "y", 0, "width", 200, "height", 40),
            // Top right L
            Map.of("x", 1560, "y", 0, "width", 40, "height", 200),
            Map.of("x", 1400, "y", 0, "width", 200, "height", 40),
            // Bottom left L
            Map.of("x", 0, "y", 1000, "width", 40, "height", 200),
            Map.of("x", 0, "y", 1160, "width", 200, "height", 40),
            // Bottom right L
            Map.of("x", 1560, "y", 1000, "width", 40, "height", 200),
            Map.of("x", 1400, "y", 1160, "width", 200, "height", 40),
            // Middle top
            Map.of("x", 700, "y", 0, "width", 200, "height", 40),
            // Middle bottom
            Map.of("x", 700, "y", 1160, "width", 200, "height", 40),
            // Middle left
            Map.of("x", 0, "y", 500, "width", 40, "height", 200),
            // Middle right
            Map.of("x", 1560, "y", 500, "width", 40, "height", 200),
            // Central rectangle (as rectangle for now)
            Map.of("x", 700, "y", 500, "width", 200, "height", 200),
            // New L-shaped walls (open toward center, not attached to central rectangle)
            // Top left L
            Map.of("x", 300, "y", 250, "width", 30, "height", 200), // vertical
            Map.of("x", 300, "y", 250, "width", 200, "height", 30), // horizontal
            // Bottom left L
            Map.of("x", 300, "y", 720, "width", 30, "height", 200), // vertical
            Map.of("x", 300, "y", 890, "width", 200, "height", 30), // horizontal
            // Top right L
            Map.of("x", 1270, "y", 250, "width", 30, "height", 200), // vertical
            Map.of("x", 1100, "y", 250, "width", 200, "height", 30), // horizontal
            // Bottom right L
            Map.of("x", 1270, "y", 720, "width", 30, "height", 200), // vertical
            Map.of("x", 1100, "y", 890, "width", 200, "height", 30)  // horizontal
        );
    }
} 