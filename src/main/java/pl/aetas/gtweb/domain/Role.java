package pl.aetas.gtweb.domain;

public enum Role {
    ROLE_ADMIN(0), ROLE_USER(1);

    private final int intValue;

    private Role(int intValue) {
        this.intValue = intValue;
    }

    public int intValue() {
        return intValue;
    }
}