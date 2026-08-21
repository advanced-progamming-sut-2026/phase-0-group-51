package models.effects;

public record GameplayNoticeEvent(Type type) {
    public enum Type {
        NECROMANCY,
        LOW_TIDE_ZOMBIES
    }

    public static GameplayNoticeEvent necromancy() {
        return new GameplayNoticeEvent(Type.NECROMANCY);
    }

    public static GameplayNoticeEvent lowTideZombies() {
        return new GameplayNoticeEvent(Type.LOW_TIDE_ZOMBIES);
    }
}
