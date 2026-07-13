package dev.yeldos.echoprotocol.recording;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerRecording {
    private RecordedFrame[] frames = new RecordedFrame[20];
    private int start;
    private int size;
    private final List<SoundMarker> soundMarkers = new ArrayList<>();
    private final List<String> recentChat = new ArrayList<>();

    public void resize(int capacity) {
        capacity = Math.max(20, capacity);
        if (frames.length == capacity) {
            return;
        }
        List<RecordedFrame> ordered = frames();
        if (ordered.size() > capacity) {
            ordered = ordered.subList(ordered.size() - capacity, ordered.size());
        }
        frames = new RecordedFrame[capacity];
        start = 0;
        size = 0;
        for (RecordedFrame frame : ordered) {
            add(frame);
        }
    }

    public void add(RecordedFrame frame) {
        int index = (start + size) % frames.length;
        if (size == frames.length) {
            frames[start] = frame;
            start = (start + 1) % frames.length;
        } else {
            frames[index] = frame;
            size++;
        }
    }

    public RecordedFrame latest() {
        if (size == 0) {
            return null;
        }
        return frames[(start + size - 1) % frames.length];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return frames.length;
    }

    public List<RecordedFrame> randomSegment(int minFrames, int maxFrames) {
        if (size < minFrames) {
            return List.of();
        }
        int length = Math.min(size, ThreadLocalRandom.current().nextInt(minFrames, Math.max(minFrames, maxFrames) + 1));
        int offset = ThreadLocalRandom.current().nextInt(0, size - length + 1);
        List<RecordedFrame> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(frames[(start + offset + i) % frames.length]);
        }
        return result;
    }

    public List<RecordedFrame> frames() {
        List<RecordedFrame> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(frames[(start + i) % frames.length]);
        }
        return result;
    }

    public void addSound(SoundMarker marker) {
        soundMarkers.add(marker);
        while (soundMarkers.size() > 128) {
            soundMarkers.remove(0);
        }
    }

    public List<SoundMarker> soundMarkers() {
        return List.copyOf(soundMarkers);
    }

    public void addChat(String message) {
        String sanitized = message.replaceAll("§.", "").replace('\n', ' ').replace('\r', ' ').trim();
        if (!sanitized.startsWith("/") && !sanitized.isBlank()) {
            recentChat.add(sanitized.substring(0, Math.min(96, sanitized.length())));
            while (recentChat.size() > 12) {
                recentChat.remove(0);
            }
        }
    }

    public String randomChat() {
        if (recentChat.isEmpty()) {
            return "";
        }
        return recentChat.get(ThreadLocalRandom.current().nextInt(recentChat.size()));
    }
}
