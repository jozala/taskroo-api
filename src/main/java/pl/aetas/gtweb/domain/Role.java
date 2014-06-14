package pl.aetas.gtweb.domain;

public enum Role {
    ADMIN(0), USER(1);

    private final int intValue;

    private Role(int intValue) {
        this.intValue = intValue;
    }

    public int intValue() {
        return intValue;
    }

    public static Role getByInt(int value) {
        for (Role role : Role.values()) {
            if (role.intValue() == value) {
                return role;
            }
        }
        throw new IllegalArgumentException("Value: " + value + " does not map to any user role value");
    }
}