package dev.yeldos.echoprotocol.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoRenderColorTest {
    @Test
    void appliesReadableGhostTintAndPacksOpacityIntoTheActualAlphaChannel() {
        int color = EchoRenderColor.withOpacity(0.95F);

        assertEquals(242, color >>> 24);
        assertEquals(220, color >>> 16 & 0xFF);
        assertEquals(238, color >>> 8 & 0xFF);
        assertEquals(255, color & 0xFF);
    }

    @Test
    void clampsOpacityToTheValidRenderRange() {
        assertEquals(0, EchoRenderColor.withOpacity(-1.0F) >>> 24);
        assertEquals(255, EchoRenderColor.withOpacity(2.0F) >>> 24);
    }
}
