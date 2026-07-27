package dev.yeldos.echoprotocol.network;

import dev.yeldos.echoprotocol.EchoProtocol;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record ClearSkinCachePayload(UUID playerUuid) implements CustomPayload {
    public static final Id<ClearSkinCachePayload> ID = new Id<>(EchoProtocol.id("clear_skin_cache"));
    public static final PacketCodec<RegistryByteBuf, ClearSkinCachePayload> CODEC = CustomPayload.codecOf(
            (payload, buffer) -> buffer.writeUuid(payload.playerUuid()),
            buffer -> new ClearSkinCachePayload(buffer.readUuid()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
