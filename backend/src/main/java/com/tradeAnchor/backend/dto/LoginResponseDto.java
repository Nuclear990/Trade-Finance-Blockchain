package com.tradeAnchor.backend.dto;

import com.tradeAnchor.backend.model.UserType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginResponseDto {
    String accessToken;
    UserType userType;
    String username;
}
