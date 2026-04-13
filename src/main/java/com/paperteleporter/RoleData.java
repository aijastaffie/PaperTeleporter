package com.paperteleporter;

public class RoleData {
    private String playerUuid;
    private UserRole role;

    public RoleData(String playerUuid, UserRole role) {
        this.playerUuid = playerUuid;
        this.role = role;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
