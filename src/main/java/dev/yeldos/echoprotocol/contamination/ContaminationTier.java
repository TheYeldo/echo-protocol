package dev.yeldos.echoprotocol.contamination;

public enum ContaminationTier {
    CLEAN,
    UNSTABLE,
    DISTORTED,
    OVERRIDDEN;

    public static ContaminationTier fromValue(float value) {
        if (value >= 0.70F) {
            return OVERRIDDEN;
        }
        if (value >= 0.45F) {
            return DISTORTED;
        }
        if (value >= 0.20F) {
            return UNSTABLE;
        }
        return CLEAN;
    }
}
