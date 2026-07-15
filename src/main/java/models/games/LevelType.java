package models.games;

public enum LevelType {
    NORMAL{
        @Override
        public void initialize(GameState state) { }
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
    },
    CONVEYOR_BELT{
        @Override
        public void initialize(GameState state) {
            // توقف  خورشید و تنظیم اولیه نوار گیاه
        }
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
    },

    SAVE_OUR_SEEDS{
        @Override
        public void initialize(GameState state) {
            // TODO: Add protected starting plants and fail the level when one is destroyed.
            return;
        }
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
    },

    NIGHT_OPS{
        @Override
        public void initialize(GameState state) { }
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
    },

    PLANT_WHAT_YOU_GET{
        @Override
        // دادن خورشید اولیه  و توقف  خورشید و کاشت هرچی گیاه که میخوایم
        public void initialize(GameState state) { }
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
    },
    BOSS{
        @Override
        public void initialize(GameState state) {
            // TODO: Spawn the chapter boss and add the boss-specific win condition.
            return;
        }
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
    };

    public abstract void initialize(GameState state);
    public abstract boolean isFinished(GameState state);
}
