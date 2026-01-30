package com.tradeAnchor.backend.dto;

import com.tradeAnchor.backend.model.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshDto {
    private String username;
    private UserType UserType;
}
