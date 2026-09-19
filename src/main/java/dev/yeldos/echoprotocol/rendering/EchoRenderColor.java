package dev.yeldos.echoprotocol.rendering;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

public final class EchoRenderColor {
    private static final int GHOST_RED = 220;
    private static final int GHOST_GREEN = 238;
    private static final int GHOST_BLUE = 255;

    private EchoRenderColor() {
    }

    public static int withOpacity(float opacity) {
        int alpha = ColorHelper.channelFromFloat(MathHelper.clamp(opacity, 0.0F, 1.0F));
        return alpha << 24 | GHOST_RED << 16 | GHOST_GREEN << 8 | GHOST_BLUE;
    }
}
