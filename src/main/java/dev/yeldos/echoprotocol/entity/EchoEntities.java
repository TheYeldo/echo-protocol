package dev.yeldos.echoprotocol.entity;

import dev.yeldos.echoprotocol.EchoProtocol;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class EchoEntities {
    public static final EntityType<EchoEntity> ECHO = Registry.register(
            Registries.ENTITY_TYPE,
            EchoProtocol.id("echo"),
            EntityType.Builder.create(EchoEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6F, 1.8F)
                    .disableSaving()
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build("echo")
    );

    private EchoEntities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(ECHO, EchoEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D));
    }
}
