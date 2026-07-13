package dev.yeldos.echoprotocol.entity;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EchoEntity extends MobEntity {
    private static final TrackedData<Float> OPACITY = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> SHARED = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Optional<UUID>> TARGET = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> REPLAY_SNEAKING = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_SPRINTING = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> REPLAY_SWIMMING = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<ItemStack> HELD_ITEM = DataTracker.registerData(EchoEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

    private final List<RecordedFrame> replay = new ArrayList<>();
    private int replayAge;
    private int sampleIntervalTicks = 2;
    private float baseOpacity = 0.45F;
    private boolean lookAtTarget;
    private boolean grantLookAdvancement;

    public EchoEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setSilent(false);
        this.setCanPickUpLoot(false);
        this.experiencePoints = 0;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new Goal() {
            @Override
            public boolean canStart() {
                return false;
            }
        });
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OPACITY, 0.0F);
        builder.add(SHARED, false);
        builder.add(TARGET, Optional.empty());
        builder.add(REPLAY_SNEAKING, false);
        builder.add(REPLAY_SPRINTING, false);
        builder.add(REPLAY_SWIMMING, false);
        builder.add(HELD_ITEM, ItemStack.EMPTY);
    }

    public void configure(UUID targetUuid, boolean shared, List<RecordedFrame> frames, int sampleIntervalTicks, EchoConfig config, boolean lookAtTarget) {
        this.dataTracker.set(TARGET, Optional.of(targetUuid));
        this.dataTracker.set(SHARED, shared);
        this.replay.clear();
        this.replay.addAll(frames);
        this.sampleIntervalTicks = Math.max(1, sampleIntervalTicks);
        this.baseOpacity = Math.max(0.05F, config.echoOpacity());
        this.lookAtTarget = lookAtTarget;
        this.grantLookAdvancement = lookAtTarget;
        this.dataTracker.set(OPACITY, 0.0F);
        if (!frames.isEmpty()) {
            RecordedFrame first = frames.get(0);
            this.refreshPositionAndAngles(first.x(), first.y(), first.z(), first.bodyYaw(), first.pitch());
            this.setHeadYaw(first.headYaw());
            this.dataTracker.set(HELD_ITEM, first.heldItemVisual());
            this.setStackInHand(Hand.MAIN_HAND, first.heldItemVisual());
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (getWorld().isClient()) {
            return;
        }
        if (replay.isEmpty()) {
            discard();
            return;
        }
        int lastTick = (replay.size() - 1) * sampleIntervalTicks;
        if (replayAge > lastTick + 20) {
            discard();
            return;
        }
        applyReplayFrame();
        replayAge++;
    }

    private void applyReplayFrame() {
        float progress = replayAge / (float) sampleIntervalTicks;
        int index = MathHelper.clamp((int) progress, 0, replay.size() - 1);
        int nextIndex = MathHelper.clamp(index + 1, 0, replay.size() - 1);
        float tickDelta = MathHelper.clamp(progress - index, 0.0F, 1.0F);
        RecordedFrame frame = replay.get(index);
        RecordedFrame next = replay.get(nextIndex);

        double x = MathHelper.lerp(tickDelta, frame.x(), next.x());
        double y = MathHelper.lerp(tickDelta, frame.y(), next.y());
        double z = MathHelper.lerp(tickDelta, frame.z(), next.z());
        float yaw = MathHelper.lerpAngleDegrees(tickDelta, frame.bodyYaw(), next.bodyYaw());
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, frame.headYaw(), next.headYaw());
        float pitch = MathHelper.lerp(tickDelta, frame.pitch(), next.pitch());

        if (lookAtTarget && getWorld() instanceof ServerWorld serverWorld && replayAge > sampleIntervalTicks * 10) {
            getTargetUuid().flatMap(uuid -> Optional.ofNullable(serverWorld.getPlayerByUuid(uuid))).ifPresent(player -> {
                Vec3d delta = player.getEyePos().subtract(getEyePos());
                float targetYaw = (float) (MathHelper.atan2(delta.z, delta.x) * 57.2957763671875D) - 90.0F;
                this.setHeadYaw(MathHelper.lerpAngleDegrees(0.12F, this.getHeadYaw(), targetYaw));
            });
        } else {
            setHeadYaw(headYaw);
        }

        refreshPositionAndAngles(x, y, z, yaw, pitch);
        bodyYaw = yaw;
        dataTracker.set(REPLAY_SNEAKING, frame.sneaking());
        dataTracker.set(REPLAY_SPRINTING, frame.sprinting());
        dataTracker.set(REPLAY_SWIMMING, frame.swimming());
        dataTracker.set(HELD_ITEM, frame.heldItemVisual());
        setStackInHand(Hand.MAIN_HAND, frame.heldItemVisual());

        int fadeTicks = 20;
        int duration = Math.max(1, (replay.size() - 1) * sampleIntervalTicks);
        float fadeIn = MathHelper.clamp(replayAge / (float) fadeTicks, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((duration + fadeTicks - replayAge) / (float) fadeTicks, 0.0F, 1.0F);
        dataTracker.set(OPACITY, baseOpacity * Math.min(fadeIn, fadeOut));
    }

    public boolean visibleTo(UUID viewerUuid) {
        return dataTracker.get(SHARED) || dataTracker.get(TARGET).map(viewerUuid::equals).orElse(false);
    }

    public Optional<UUID> getTargetUuid() {
        return dataTracker.get(TARGET);
    }

    public float getReplayOpacity() {
        return dataTracker.get(OPACITY);
    }

    public boolean replaySneaking() {
        return dataTracker.get(REPLAY_SNEAKING);
    }

    public boolean replaySprinting() {
        return dataTracker.get(REPLAY_SPRINTING);
    }

    public boolean replaySwimming() {
        return dataTracker.get(REPLAY_SWIMMING);
    }

    public ItemStack getHeldItemVisual() {
        return dataTracker.get(HELD_ITEM);
    }

    public boolean consumeLookAdvancementFlag() {
        boolean result = grantLookAdvancement;
        grantLookAdvancement = false;
        return result;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public void pushAwayFrom(net.minecraft.entity.Entity entity) {
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        super.move(MovementType.SELF, movement);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMBIENT_CAVE.value();
    }

    @Override
    protected float getSoundVolume() {
        return 0.08F;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
    }
}
