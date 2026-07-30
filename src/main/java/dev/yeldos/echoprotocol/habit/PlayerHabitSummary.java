package dev.yeldos.echoprotocol.habit;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dev.yeldos.echoprotocol.memory.PersistentHabit;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class PlayerHabitSummary {
    public static final class Habit {
        private final HabitType type;
        private final String dimension;
        private final BlockPos position;
        private ItemStack visualItem;
        private int observations;
        private long lastSeenTick;

        Habit(HabitType type, String dimension, BlockPos position, ItemStack visualItem, long tick) {
            this.type = type;
            this.dimension = dimension;
            this.position = position.toImmutable();
            this.visualItem = visualItem.copyWithCount(Math.min(1, visualItem.getCount()));
            this.observations = 1;
            this.lastSeenTick = tick;
        }

        void observe(ItemStack item, long tick) {
            observations++;
            lastSeenTick = Math.max(lastSeenTick, tick);
            if (!item.isEmpty()) {
                visualItem = item.copyWithCount(1);
            }
        }

        public HabitType type() { return type; }
        public String dimension() { return dimension; }
        public BlockPos position() { return position; }
        public ItemStack visualItem() { return visualItem.copy(); }
        public int observations() { return observations; }
        public long lastSeenTick() { return lastSeenTick; }

        boolean matches(HabitType otherType, String otherDimension, BlockPos otherPosition) {
            return type == otherType && dimension.equals(otherDimension)
                    && position.getSquaredDistance(otherPosition) <= 25.0D;
        }
    }

    private final List<Habit> habits = new ArrayList<>();

    public void observe(HabitType type, String dimension, BlockPos position, ItemStack item, long tick, int maximum) {
        for (Habit habit : habits) {
            if (habit.matches(type, dimension, position)) {
                habit.observe(item, tick);
                return;
            }
        }
        habits.add(new Habit(type, dimension, position, item, tick));
        trim(maximum);
    }

    public List<Habit> entries() {
        return habits.stream()
                .sorted(Comparator.comparingInt(Habit::observations).thenComparingLong(Habit::lastSeenTick).reversed())
                .toList();
    }

    public void trim(int maximum) {
        habits.sort(Comparator.comparingInt(Habit::observations).thenComparingLong(Habit::lastSeenTick).reversed());
        while (habits.size() > Math.max(1, maximum)) {
            habits.remove(habits.size() - 1);
        }
    }

    public int clear() {
        int count = habits.size();
        habits.clear();
        return count;
    }

    public void restore(PersistentHabit persistent) {
        ItemStack item = ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(persistent.visualItemId());
        if (id != null && Registries.ITEM.containsId(id)) {
            item = new ItemStack(Registries.ITEM.get(id));
        }
        Habit habit = new Habit(persistent.type(), persistent.dimension(), persistent.position(), item,
                persistent.lastSeenTick());
        habit.observations = persistent.observations();
        habits.add(habit);
    }
}
