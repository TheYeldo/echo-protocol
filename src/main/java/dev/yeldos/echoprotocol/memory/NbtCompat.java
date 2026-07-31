package dev.yeldos.echoprotocol.memory;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Uuids;

import java.util.UUID;

final class NbtCompat {
    private NbtCompat() {
    }

    static boolean containsType(NbtCompound nbt, String key, byte type) {
        NbtElement element = nbt.get(key);
        return element != null && element.getType() == type;
    }

    static int getInt(NbtCompound nbt, String key) {
        return nbt.getInt(key).orElse(0);
    }

    static long getLong(NbtCompound nbt, String key) {
        return nbt.getLong(key).orElse(0L);
    }

    static float getFloat(NbtCompound nbt, String key) {
        return nbt.getFloat(key).orElse(0.0F);
    }

    static double getDouble(NbtCompound nbt, String key) {
        return nbt.getDouble(key).orElse(0.0D);
    }

    static boolean getBoolean(NbtCompound nbt, String key) {
        return nbt.getBoolean(key).orElse(false);
    }

    static String getString(NbtCompound nbt, String key) {
        return nbt.getString(key).orElse("");
    }

    static long[] getLongArray(NbtCompound nbt, String key) {
        return nbt.getLongArray(key).orElseGet(() -> new long[0]);
    }

    static NbtCompound getCompound(NbtCompound nbt, String key) {
        return nbt.getCompound(key).orElseGet(NbtCompound::new);
    }

    static NbtCompound getCompound(NbtList list, int index) {
        return list.getCompound(index).orElseGet(NbtCompound::new);
    }

    static NbtList getList(NbtCompound nbt, String key) {
        return nbt.getList(key).orElseGet(NbtList::new);
    }

    static UUID getUuid(NbtCompound nbt, String key) {
        int[] value = nbt.getIntArray(key).orElseThrow(() -> new IllegalArgumentException("missing " + key));
        if (value.length != 4) {
            throw new IllegalArgumentException("invalid " + key);
        }
        return Uuids.toUuid(value);
    }

    static void putUuid(NbtCompound nbt, String key, UUID value) {
        nbt.putIntArray(key, Uuids.toIntArray(value));
    }
}
