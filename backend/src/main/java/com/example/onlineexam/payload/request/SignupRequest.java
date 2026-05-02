package com.example.onlineexam.payload.request;

import lombok.Data;
import java.util.Set;

@Data
public class SignupRequest {
    private String username;
    private String password;
    private String email;
    private String department;
    private Set<String> role;
}
