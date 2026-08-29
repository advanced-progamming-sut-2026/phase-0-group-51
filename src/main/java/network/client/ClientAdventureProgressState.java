package network.client;

public final class ClientAdventureProgressState {
    private static boolean loaded;
    private static int currentChapter = 1;
    private static int currentLevel = 1;

    private ClientAdventureProgressState() {
    }

    public static synchronized void replaceWith(
            int chapter,
            int level
    ) {
        currentChapter = Math.max(1, chapter);
        currentLevel = Math.max(1, level);
        loaded = true;
    }

    public static synchronized void clear() {
        loaded = false;
        currentChapter = 1;
        currentLevel = 1;
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized int getCurrentChapter() {
        return currentChapter;
    }

    public static synchronized int getCurrentLevel() {
        return currentLevel;
    }
}
