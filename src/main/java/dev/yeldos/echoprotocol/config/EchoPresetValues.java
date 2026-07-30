package dev.yeldos.echoprotocol.config;

public record EchoPresetValues(
        float eventIntervalMultiplier,
        float strongEventWeightMultiplier,
        float threadActivityMultiplier,
        float contradictionWeightMultiplier,
        float contaminationGrowthMultiplier,
        float originalFinaleWeightMultiplier,
        float strongSilenceMultiplier,
        float directConfrontationMultiplier
) {
    public EchoPresetValues {
        eventIntervalMultiplier = clamp(eventIntervalMultiplier, 0.70F, 1.75F);
        strongEventWeightMultiplier = clamp(strongEventWeightMultiplier, 0.40F, 1.50F);
        threadActivityMultiplier = clamp(threadActivityMultiplier, 0.40F, 1.60F);
        contradictionWeightMultiplier = clamp(contradictionWeightMultiplier, 0.30F, 1.75F);
        contaminationGrowthMultiplier = clamp(contaminationGrowthMultiplier, 0.40F, 1.50F);
        originalFinaleWeightMultiplier = clamp(originalFinaleWeightMultiplier, 0.40F, 1.60F);
        strongSilenceMultiplier = clamp(strongSilenceMultiplier, 0.75F, 1.75F);
        directConfrontationMultiplier = clamp(directConfrontationMultiplier, 0.35F, 1.30F);
    }

    public static EchoPresetValues forPreset(EchoIntensityPreset preset) {
        return switch (preset) {
            case SUBTLE -> new EchoPresetValues(1.45F, 0.60F, 0.60F, 0.50F, 0.60F, 0.60F, 1.50F, 0.55F);
            case STANDARD, CUSTOM -> new EchoPresetValues(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            case INTENSE -> new EchoPresetValues(0.78F, 1.25F, 1.35F, 1.35F, 1.25F, 1.30F, 0.82F, 1.15F);
        };
    }

    private static float clamp(float value, float minimum, float maximum) {
        if (!Float.isFinite(value)) {
            return minimum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
