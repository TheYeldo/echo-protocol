package dev.yeldos.echoprotocol.stage;

public enum EchoStage {
    OBSERVATION(0),
    DEJA_VU(1),
    CORRUPTED_MEMORY(2),
    THE_ORIGINAL(3);

    private final int id;

    EchoStage(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static EchoStage fromId(int id) {
        if (id <= 0) {
            return OBSERVATION;
        }
        if (id == 1) {
            return DEJA_VU;
        }
        if (id == 2) {
            return CORRUPTED_MEMORY;
        }
        return THE_ORIGINAL;
    }
}
