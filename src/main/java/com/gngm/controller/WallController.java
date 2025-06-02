// package com.gngm.controller;
//
// import org.springframework.web.bind.annotation.*;
// import java.util.*;
// import com.gngm.service.GameEngineService;
//
// @RestController
// @RequestMapping("/api/walls")
// public class WallController {
//     @GetMapping("/{mapName}")
//     public List<GameEngineService.Wall> getWalls(@PathVariable String mapName) {
//         return GameEngineService.getWallConfigurations().getOrDefault(mapName, List.of());
//     }
// } 