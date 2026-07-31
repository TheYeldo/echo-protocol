package dev.yeldos.echoprotocol.entity;

import dev.yeldos.echoprotocol.EchoProtocol;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class EchoEntities {
    private static final Identifier ECHO_ID = EchoProtocol.id("echo");
    private static final RegistryKey<EntityType<?>> ECHO_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, ECHO_ID);

    public static final EntityType<EchoEntity> ECHO = Registry.register(
            Registries.ENTITY_TYPE,
            ECHO_ID,
            EntityType.Builder.create(EchoEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6F, 1.8F)
                    .disableSaving()
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build(ECHO_KEY)
    );

    private EchoEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(ECHO, EchoEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 1.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0D));
    }
}
