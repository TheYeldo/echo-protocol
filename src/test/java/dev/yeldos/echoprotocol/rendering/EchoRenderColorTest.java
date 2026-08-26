package dev.yeldos.echoprotocol.rendering;

import net.minecraft.util.math.ColorHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoRenderColorTest {
    @Test
    void appliesReadableGhostTintAndPacksOpacityIntoTheActualAlphaChannel() {
        int color = EchoRenderColor.withOpacity(0.95F);

        assertEquals(242, ColorHelper.getAlpha(color));
        assertEquals(220, ColorHelper.getRed(color));
        assertEquals(238, ColorHelper.getGreen(color));
        assertEquals(255, ColorHelper.getBlue(color));
        assertEquals(242 / 255.0F, ColorHelper.getAlphaFloat(color));
    }

    @Test
    void clampsOpacityToTheValidRenderRange() {
        assertEquals(0, ColorHelper.getAlpha(EchoRenderColor.withOpacity(-1.0F)));
        assertEquals(255, ColorHelper.getAlpha(EchoRenderColor.withOpacity(2.0F)));
    }
}
