package dev.yeldos.echoprotocol.interaction;

import dev.yeldos.echoprotocol.entity.EchoEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Produces short-lived, target-only world reactions for an Echo. The real world is never changed: doors and
 * placed blocks are sent as client-side block updates and are restored from the authoritative server state.
 */
public final class EchoWorldInteraction {
    private static final int PASSAGE_HOLD_TICKS = 28;
    private static final int GHOST_BLOCK_HOLD_TICKS = 34;
    private static final double MOVEMENT_EPSILON_SQUARED = 0.0004D;

    private final Map<BlockPos, Integer> visualBlocks = new HashMap<>();
    private Vec3d previousPosition;
    private int age;
    private int lastGestureAge = -40;

    public void tick(EchoEntity echo) {
        age++;
        if (!(echo.getWorld() instanceof ServerWorld world)) {
            return;
        }
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target == null) {
            visualBlocks.clear();
            return;
        }

        restoreExpired(world, target);
        Vec3d current = echo.getPos();
        boolean moving = previousPosition != null
                && previousPosition.squaredDistanceTo(current) >= MOVEMENT_EPSILON_SQUARED;
        previousPosition = current;
        if (moving && age % 2 == 0) {
            revealNearbyPassages(echo, world, target);
        }
    }

    public boolean performPlacement(EchoEntity echo, Vec3d preferredTarget) {
        if (!(echo.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        ServerPlayerEntity target = echo.getTargetPlayer();
        ItemStack held = echo.getHeldItemVisual();
        if (target == null || !(held.getItem() instanceof BlockItem blockItem) || age - lastGestureAge < 8) {
            return false;
        }
        BlockPos placement = findPlacement(world, echo, target, preferredTarget);
        if (placement == null) {
            return false;
        }

        BlockState visual = blockItem.getBlock().getDefaultState();
        sendVisual(target, placement, visual, age + GHOST_BLOCK_HOLD_TICKS);
        world.spawnParticles(target, new BlockStateParticleEffect(ParticleTypes.BLOCK, visual), true,
                placement.getX() + 0.5D, placement.getY() + 0.55D, placement.getZ() + 0.5D,
                10, 0.28D, 0.32D, 0.28D, 0.035D);
        playTargetOnly(target, visual.getSoundGroup().getPlaceSound(), placement.toCenterPos(),
                0.45F, 0.82F);
        echo.swingHand(Hand.MAIN_HAND, true);
        lastGestureAge = age;
        return true;
    }

    public void clear(EchoEntity echo) {
        if (visualBlocks.isEmpty() || !(echo.getWorld() instanceof ServerWorld world)) {
            visualBlocks.clear();
            return;
        }
        ServerPlayerEntity target = echo.getTargetPlayer();
        if (target != null) {
            for (BlockPos pos : visualBlocks.keySet()) {
                target.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, world.getBlockState(pos)));
            }
        }
        visualBlocks.clear();
    }

    public static boolean isPassage(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof TrapdoorBlock
                || state.getBlock() instanceof FenceGateBlock;
    }

    public static boolean collisionContainsOnlyPassages(ServerWorld world, Box box) {
        int minimumX = (int) Math.floor(box.minX + 1.0E-7D);
        int minimumY = (int) Math.floor(box.minY + 1.0E-7D);
        int minimumZ = (int) Math.floor(box.minZ + 1.0E-7D);
        int maximumX = (int) Math.floor(box.maxX - 1.0E-7D);
        int maximumY = (int) Math.floor(box.maxY - 1.0E-7D);
        int maximumZ = (int) Math.floor(box.maxZ - 1.0E-7D);
        for (BlockPos pos : BlockPos.iterate(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ)) {
            BlockState state = world.getBlockState(pos);
            if (state.getCollisionShape(world, pos).isEmpty()) {
                continue;
            }
            boolean intersects = state.getCollisionShape(world, pos).getBoundingBoxes().stream()
                    .map(local -> local.offset(pos))
                    .anyMatch(box::intersects);
            if (intersects && !isPassage(state)) {
                return false;
            }
        }
        return true;
    }

    private void revealNearbyPassages(EchoEntity echo, ServerWorld world, ServerPlayerEntity target) {
        BlockPos center = echo.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(center.add(-1, 0, -1), center.add(1, 1, 1))) {
            BlockState state = world.getBlockState(pos);
            if (!isPassage(state) || !state.contains(Properties.OPEN) || state.get(Properties.OPEN)) {
                continue;
            }
            boolean firstReveal = !visualBlocks.containsKey(pos.toImmutable());
            revealPassagePart(target, pos, state);
            if (state.getBlock() instanceof DoorBlock && state.contains(Properties.DOUBLE_BLOCK_HALF)) {
                BlockPos other = state.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                        ? pos.up() : pos.down();
                BlockState otherState = world.getBlockState(other);
                if (otherState.getBlock() instanceof DoorBlock && otherState.contains(Properties.OPEN)) {
                    revealPassagePart(target, other, otherState);
                }
            }
            if (firstReveal && age - lastGestureAge >= 7) {
                echo.swingHand(Hand.MAIN_HAND, true);
                playTargetOnly(target, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, pos.toCenterPos(), 0.55F, 0.82F);
                lastGestureAge = age;
            }
        }
    }

    private void revealPassagePart(ServerPlayerEntity target, BlockPos pos, BlockState state) {
        sendVisual(target, pos, state.with(Properties.OPEN, true), age + PASSAGE_HOLD_TICKS);
    }

    private void sendVisual(ServerPlayerEntity target, BlockPos pos, BlockState state, int expiresAt) {
        BlockPos immutable = pos.toImmutable();
        target.networkHandler.sendPacket(new BlockUpdateS2CPacket(immutable, state));
        visualBlocks.merge(immutable, expiresAt, Math::max);
    }

    private void restoreExpired(ServerWorld world, ServerPlayerEntity target) {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = visualBlocks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            if (entry.getValue() > age) {
                continue;
            }
            target.networkHandler.sendPacket(new BlockUpdateS2CPacket(entry.getKey(),
                    world.getBlockState(entry.getKey())));
            iterator.remove();
        }
    }

    private static BlockPos findPlacement(ServerWorld world, EchoEntity echo, ServerPlayerEntity target,
                                          Vec3d preferredTarget) {
        Vec3d direction = preferredTarget == null ? facingDirection(echo)
                : preferredTarget.subtract(echo.getPos()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSquared() < 0.01D) {
            direction = facingDirection(echo);
        }
        direction = direction.normalize();
        BlockPos primary = BlockPos.ofFloored(echo.getPos().add(direction.multiply(1.25D)));
        Set<BlockPos> candidates = new LinkedHashSet<>();
        candidates.add(primary);
        candidates.add(primary.up());
        candidates.add(primary.offset(horizontalLeft(direction)));
        candidates.add(primary.offset(horizontalRight(direction)));
        BlockPos feet = echo.getBlockPos();
        for (net.minecraft.util.math.Direction horizontal : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            candidates.add(feet.offset(horizontal));
        }
        for (BlockPos candidate : candidates) {
            if (!world.isChunkLoaded(candidate) || !world.getBlockState(candidate).isReplaceable()
                    || world.getBlockState(candidate.down()).getCollisionShape(world, candidate.down()).isEmpty()) {
                continue;
            }
            Box blockBox = new Box(candidate);
            if (blockBox.intersects(target.getBoundingBox()) || blockBox.intersects(echo.getBoundingBox())) {
                continue;
            }
            return candidate.toImmutable();
        }
        return null;
    }

    private static Vec3d facingDirection(EchoEntity echo) {
        double radians = Math.toRadians(echo.getYaw());
        return new Vec3d(-Math.sin(radians), 0.0D, Math.cos(radians));
    }

    private static net.minecraft.util.math.Direction horizontalLeft(Vec3d direction) {
        return net.minecraft.util.math.Direction.getFacing(direction.z, 0.0D, -direction.x);
    }

    private static net.minecraft.util.math.Direction horizontalRight(Vec3d direction) {
        return net.minecraft.util.math.Direction.getFacing(-direction.z, 0.0D, direction.x);
    }

    private static void playTargetOnly(ServerPlayerEntity target, SoundEvent sound, Vec3d pos,
                                       float volume, float pitch) {
        target.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket(
                Registries.SOUND_EVENT.getEntry(sound), SoundCategory.BLOCKS,
                pos.x, pos.y, pos.z, volume, pitch, target.getRandom().nextLong()));
    }
}
