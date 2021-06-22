package fireflies.entity;

public enum FireflyAbdomenAnimation {
    OFF, // constantly off
    ON, // constantly on
    DEFAULT, // random flashing
    CALM, // slow random flashing
    CALM_SYNCHRONIZED, // slow synchronized flashing
    STARRY_NIGHT, // fast random flashing
    STARRY_NIGHT_SYNCHRONIZED, // fast synchronized flashing
    FRANTIC, // very fast random flashing
    SLOW, // very slow random flashing
    QUICK_BLINKS, // two fast flashes followed by a short period of nothing
    ILLUMINATED // pulsing like a heartbeat, never turns off fully.
}
