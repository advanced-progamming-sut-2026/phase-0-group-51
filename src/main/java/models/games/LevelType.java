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
            state.setSun(0);
        }
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
        @Override
        public boolean usesPlantSelection() {
            return false;
        }
    },

    SAVE_OUR_SEEDS{
        @Override
        public void initialize(GameState state) {
            state.configureSaveOurSeeds(
                    state.getCurrentLevel().saveOurSeedsConfig()
            );
        }
        @Override
        public boolean isFinished(GameState state) {
            return !state.hasLostProtectedPlant()
                    && state.getZombieWaveManager() != null
                    && state.getZombieWaveManager().isLevelCleared();
        }
    },

    NIGHT_OPS{
        @Override
        public void initialize(GameState state) {
            state.logEvent("Night Ops started.\n");
        }
        @Override
        public boolean isFinished(GameState state) {
           return state.getZombieWaveManager() != null && state.getZombieWaveManager().isLevelCleared();
        }
    },
    LOVE_YOUR_PLANTS{
        @Override
        public void initialize(GameState state) {}
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager() != null
                    && state.getZombieWaveManager().isLevelCleared();
        }
    },
    LOCKED_PLANTS{
        @Override
        public void initialize(GameState state) {}
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager() != null
                    && state.getZombieWaveManager().isLevelCleared();
        }
    },
    TIMED_BATTLE{
        @Override
        public void initialize(GameState state) {}
        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager() != null
                    && state.getZombieWaveManager().isLevelCleared();
        }
    },
    DEAD_LINE {
        @Override
        public void initialize(GameState state) {
            state.configureDeadline(state.getCurrentLevel().deadlineColumn());
        }

        @Override
        public boolean isFinished(GameState state) {
            return state.getZombieWaveManager().isLevelCleared();
        }
    },

    PLANT_WHAT_YOU_GET{
        @Override
        public void initialize(GameState state) {
            state.logEvent(
                    "Plant What You Get started with " + state.getSun()
                            + " sun.sun-producing plants are forbidden, and the lawn is dry. Plant freely "
                            + "without recharge, then use 'start zombie waves'.\n"
            );
        }
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
    public boolean usesPlantSelection() {return true;}
    public abstract void initialize(GameState state);
    public abstract boolean isFinished(GameState state);
}
