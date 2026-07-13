package dev.yeldos.echoprotocol.rendering;

import com.mojang.authlib.GameProfile;
import dev.yeldos.echoprotocol.EchoProtocol;
import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class EchoSkinResolver {
    private static final int MAX_CACHE_ENTRIES = 64;
    private static final Map<UUID, SkinTextures> CACHE = new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, SkinTextures> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private EchoSkinResolver() {
    }

    public static SkinTextures resolve(EchoEntity echo) {
        UUID uuid = echo.getTargetUuid().orElse(null);
        if (uuid == null || !EchoProtocol.config().realPlayerSkins()) {
            return DefaultSkinHelper.getSkinTextures(uuid == null ? UUID.nameUUIDFromBytes("echo".getBytes(java.nio.charset.StandardCharsets.UTF_8)) : uuid);
        }
        if (EchoProtocol.config().skinCacheEnabled()) {
            SkinTextures cached = CACHE.get(uuid);
            if (cached != null) {
                return cached;
            }
        }

        MinecraftClient client = MinecraftClient.getInstance();
        SkinTextures textures = null;
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry != null) {
                textures = entry.getSkinTextures();
            }
        }
        if (textures == null) {
            textures = client.getSkinProvider().getSkinTextures(new GameProfile(uuid, "Echo"));
        }
        if (textures == null) {
            textures = DefaultSkinHelper.getSkinTextures(uuid);
        }
        if (EchoProtocol.config().skinCacheEnabled()) {
            CACHE.put(uuid, textures);
        }
        return textures;
    }

    public static boolean isCached(UUID uuid) {
        return CACHE.containsKey(uuid);
    }

    public static void clear(UUID uuid) {
        CACHE.remove(uuid);
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
