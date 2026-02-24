package com.billtracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupRequest {
    @NotBlank
    private String name;
}
