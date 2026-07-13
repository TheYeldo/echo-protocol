package dev.yeldos.echoprotocol.entity;

import dev.yeldos.echoprotocol.config.EchoConfig;
import dev.yeldos.echoprotocol.echo.EchoBehaviorController;
import dev.yeldos.echoprotocol.echo.EchoEventContext;
import dev.yeldos.echoprotocol.echo.EchoState;
import dev.yeldos.echoprotocol.echo.EchoType;
import dev.yeldos.echoprotocol.recording.RecordedFrame;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity.RemovalReason;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
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
    private int sampleIntervalTicks = 2;
    private float baseOpacity = 0.45F;
    private boolean grantLookAdvancement;
    private EchoEventContext context;
    private EchoBehaviorController behavior;
    private EchoType echoType = EchoType.MEMORY;
    private EchoState echoState = EchoState.REPLAYING;
    private boolean eventFinished;

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

    public void configure(UUID targetUuid, boolean shared, List<RecordedFrame> frames, int sampleIntervalTicks,
                          EchoConfig config, EchoEventContext context, EchoBehaviorController behavior) {
        this.dataTracker.set(TARGET, Optional.of(targetUuid));
        this.dataTracker.set(SHARED, shared);
        this.context = context;
        this.behavior = behavior;
        this.echoType = behavior.type();
        this.echoState = behavior.state();
        this.replay.clear();
        this.replay.addAll(frames);
        this.sampleIntervalTicks = Math.max(1, sampleIntervalTicks);
        this.baseOpacity = Math.max(0.05F, config.echoOpacity());
        this.dataTracker.set(OPACITY, 0.0F);
        if (!frames.isEmpty()) {
            RecordedFrame first = frames.get(0);
            this.applyFrame(first);
        }
        behavior.onStarted(this);
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.setNoGravity(true);
        if (getWorld().isClient()) {
            return;
        }
        if (behavior == null) {
            finishAndDiscard();
            return;
        }
        if (getTargetPlayer() == null) {
            behavior.onTargetUnavailable(this);
            return;
        }
        behavior.tick(this);
    }

    public boolean applyReplayFrame(int replayAge, boolean forceLookAtTarget) {
        if (replay.isEmpty()) {
            return false;
        }
        int lastTick = replayDurationTicks();
        if (replayAge > lastTick + 20) {
            return false;
        }
        float progress = Math.max(0, replayAge) / (float) sampleIntervalTicks;
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

        if (forceLookAtTarget && replayAge > sampleIntervalTicks * 10) {
            lookAtTarget(0.12F);
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
        return true;
    }

    public void applyFrame(RecordedFrame frame) {
        refreshPositionAndAngles(frame.x(), frame.y(), frame.z(), frame.bodyYaw(), frame.pitch());
        bodyYaw = frame.bodyYaw();
        setHeadYaw(frame.headYaw());
        dataTracker.set(REPLAY_SNEAKING, frame.sneaking());
        dataTracker.set(REPLAY_SPRINTING, frame.sprinting());
        dataTracker.set(REPLAY_SWIMMING, frame.swimming());
        setHeldItemVisual(frame.heldItemVisual());
    }

    public void setHeldItemVisual(ItemStack stack) {
        ItemStack visual = stack.copyWithCount(Math.min(1, stack.getCount()));
        dataTracker.set(HELD_ITEM, visual);
        setStackInHand(Hand.MAIN_HAND, visual);
    }

    public void setReplayOpacity(float opacity) {
        dataTracker.set(OPACITY, MathHelper.clamp(opacity, 0.0F, 1.0F));
    }

    public void setFadeOpacity(int age, int duration) {
        int fadeTicks = 20;
        float fadeIn = MathHelper.clamp(age / (float) fadeTicks, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((duration + fadeTicks - age) / (float) fadeTicks, 0.0F, 1.0F);
        setReplayOpacity(baseOpacity * Math.min(fadeIn, fadeOut));
    }

    public void lookAtTarget(float lerp) {
        ServerPlayerEntity player = getTargetPlayer();
        if (player == null) {
            return;
        }
        Vec3d delta = player.getEyePos().subtract(getEyePos());
        float targetYaw = (float) (MathHelper.atan2(delta.z, delta.x) * 57.2957763671875D) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * 57.2957763671875D));
        setHeadYaw(MathHelper.lerpAngleDegrees(lerp, getHeadYaw(), targetYaw));
        setPitch(MathHelper.lerp(lerp, getPitch(), targetPitch));
        grantLookAdvancement = true;
    }

    public boolean moveToward(Vec3d target, double maxStep) {
        Vec3d delta = target.subtract(getPos());
        double distance = delta.length();
        if (distance < 0.05D) {
            return false;
        }
        Vec3d step = delta.normalize().multiply(Math.min(maxStep, distance));
        Vec3d destination = getPos().add(step);
        if (!isSafeEchoPosition(destination)) {
            return false;
        }
        float yaw = (float) (MathHelper.atan2(step.z, step.x) * 57.2957763671875D) - 90.0F;
        refreshPositionAndAngles(destination.x, destination.y, destination.z, yaw, getPitch());
        bodyYaw = yaw;
        setHeadYaw(yaw);
        return true;
    }

    public boolean isSafeEchoPosition(Vec3d pos) {
        if (!(getWorld() instanceof ServerWorld world)) {
            return false;
        }
        Vec3d offset = pos.subtract(getPos());
        BlockPos blockPos = BlockPos.ofFloored(pos);
        return world.isSpaceEmpty(this, getBoundingBox().offset(offset))
                && !world.getBlockState(blockPos).isOf(Blocks.LAVA)
                && !world.getBlockState(blockPos).isOf(Blocks.FIRE)
                && !world.getBlockState(blockPos).isOf(Blocks.SOUL_FIRE);
    }

    public void finishAndDiscard() {
        if (!eventFinished && context != null) {
            context.stageManager().state(context.targetUuid()).setActiveEvent(false);
            eventFinished = true;
        }
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!eventFinished && context != null) {
            context.stageManager().state(context.targetUuid()).setActiveEvent(false);
            eventFinished = true;
        }
        super.remove(reason);
    }

    public int replayDurationTicks() {
        return Math.max(1, (replay.size() - 1) * sampleIntervalTicks);
    }

    public int sampleIntervalTicks() {
        return sampleIntervalTicks;
    }

    public List<RecordedFrame> replayFrames() {
        return List.copyOf(replay);
    }

    public RecordedFrame frameAt(int index) {
        return replay.get(MathHelper.clamp(index, 0, replay.size() - 1));
    }

    public ServerPlayerEntity getTargetPlayer() {
        if (!(getWorld() instanceof ServerWorld world)) {
            return null;
        }
        return getTargetUuid().map(uuid -> world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player ? player : null).orElse(null);
    }

    public EchoType echoType() {
        return echoType;
    }

    public EchoState echoState() {
        return echoState;
    }

    public EchoBehaviorController behavior() {
        return behavior;
    }

    public void setEchoState(EchoState echoState) {
        this.echoState = echoState;
    }

    public boolean visibleTo(UUID viewerUuid) {
        return dataTracker.get(SHARED) || dataTracker.get(TARGET).map(viewerUuid::equals).orElse(false);
    }

    @Override
    public boolean canBeSpectated(ServerPlayerEntity spectator) {
        return visibleTo(spectator.getUuid());
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
        return null;
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
