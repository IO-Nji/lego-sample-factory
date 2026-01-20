package io.life.user_service.dto.user;

import io.life.user_service.entity.UserRole;

public class UserDto {

    private Long id;
    private String username;
    private UserRole role;
    private WorkstationDto workstation;

    public UserDto(Long id, String username, UserRole role, WorkstationDto workstation) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.workstation = workstation;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public WorkstationDto getWorkstation() {
        return workstation;
    }

}
