package models.leaderBoard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


    public record LeaderBoard(String username,
                              String lastCompleted, int completedChapter,
                              int completedLevel, int minigamesCompleted,
                              int dailyQuestsCompleted, int nonDailyQuestsCompleted,
                              int highestScore) {
        private static final Pattern LEVEL_THEN_CHAPTER = Pattern.compile(
                "(?i).*level\\s+(\\d+).*chapter\\s+(\\d+).*"
        );
        private static final Pattern CHAPTER_THEN_LEVEL = Pattern.compile(
                "(?i).*chapter\\s+(\\d+).*level\\s+(\\d+).*"
        );

        public static LeaderBoard fromDatabase(
                String username,
                String lastCompleted,
                int minigamesCompleted,
                int dailyQuestsCompleted,
                int nonDailyQuestsCompleted,
                int highestScore
        ) {
            String lastCompletedBoth = lastCompleted == null || lastCompleted.isBlank()
                    ? "None"
                    : lastCompleted.trim();

            int[] progress = parseProgress(lastCompletedBoth);

            return new LeaderBoard(
                    username,
                    lastCompletedBoth,
                    progress[0],
                    progress[1],
                    Math.max(0, minigamesCompleted),
                    Math.max(0, dailyQuestsCompleted),
                    Math.max(0, nonDailyQuestsCompleted),
                    Math.max(0, highestScore)
            );
        }

        public int progressRank() {
            return completedChapter * 100 + completedLevel;
        }

        private static int[] parseProgress(String value) {
            Matcher levelThenChapter = LEVEL_THEN_CHAPTER.matcher(value);
            if (levelThenChapter.matches()) {
                return new int[]{
                        Integer.parseInt(levelThenChapter.group(2)),
                        Integer.parseInt(levelThenChapter.group(1))
                };
            }

            Matcher chapterThenLevel = CHAPTER_THEN_LEVEL.matcher(value);
            if (chapterThenLevel.matches()) {
                return new int[]{
                        Integer.parseInt(chapterThenLevel.group(1)),
                        Integer.parseInt(chapterThenLevel.group(2))
                };
            }

            return new int[]{0, 0};
        }
}
