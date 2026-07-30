package dev.yeldos.echoprotocol.config;

import java.util.Locale;

public enum EchoIntensityPreset {
    SUBTLE,
    STANDARD,
    INTENSE,
    CUSTOM;

    public static EchoIntensityPreset parse(String value) {
        if (value == null) {
            return STANDARD;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return STANDARD;
        }
    }

    public String configName() {
        return name();
    }

    public String commandName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
