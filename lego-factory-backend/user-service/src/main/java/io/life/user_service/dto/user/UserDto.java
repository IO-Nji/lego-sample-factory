package io.life.user_service.dto.user;

import io.life.user_service.entity.UserRole;

public class UserDto {

    private Long id;
    private String username;
    private UserRole role;
    private Long workstationId;  // Flat field for easy frontend access
    private WorkstationDto workstation;

    public UserDto(Long id, String username, UserRole role, Long workstationId, WorkstationDto workstation) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.workstationId = workstationId;
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

    public Long getWorkstationId() {
        return workstationId;
    }

    public WorkstationDto getWorkstation() {
        return workstation;
    }

}
