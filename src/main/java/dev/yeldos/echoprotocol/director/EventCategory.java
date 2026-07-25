package dev.yeldos.echoprotocol.director;

public enum EventCategory {
    MEMORY(EventIntensity.SUBTLE),
    CORRUPTED(EventIntensity.MODERATE),
    MIMIC(EventIntensity.STRONG),
    ORIGINAL(EventIntensity.STRONG),
    FALSE_MEMORY(EventIntensity.MODERATE),
    MAJOR_FALSE_MEMORY(EventIntensity.STRONG),
    PANIC_IMPRINT(EventIntensity.STRONG),
    PERIPHERAL(EventIntensity.SUBTLE),
    AUDIO_RESIDUE(EventIntensity.SUBTLE);

    private final EventIntensity intensity;

    EventCategory(EventIntensity intensity) {
        this.intensity = intensity;
    }

    public EventIntensity intensity() {
        return intensity;
    }
}
