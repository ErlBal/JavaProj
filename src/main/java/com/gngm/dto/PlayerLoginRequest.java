package com.gngm.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PlayerLoginRequest {

    private String username;

    private String password;
} 