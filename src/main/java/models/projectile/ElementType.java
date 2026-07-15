package models.projectile;

import models.Zombie.Zombie;
import models.games.ChapterTheme;
import models.games.GameState;

public enum ElementType {

    NORMAL {
        @Override
        public void onHit(Zombie target, GameState state) {
            return;
        }
    },

    FIRE {
        @Override
        public void onHit(Zombie target, GameState state) {
            target.clearColdEffects();
        }
    },

    ICE {
        private static final int DEFAULT_CHILL_TICKS = 60;

        @Override
        public void onHit(Zombie target, GameState state) {
            if (state.getChapterTheme() != ChapterTheme.FROSTBITE_CAVES) {
                target.applyChill(DEFAULT_CHILL_TICKS);
            }
        }

        @Override
        public void onHit(Zombie target, GameState state, int durationTicks) {
            int appliedDuration = durationTicks > 0
                    ? durationTicks
                    : DEFAULT_CHILL_TICKS;
            if (state.getChapterTheme() != ChapterTheme.FROSTBITE_CAVES) {
                target.applyChill(appliedDuration);
            }
        }
    },

    POISON {
        @Override
        public void onHit(Zombie target, GameState state) {
            target.applyPoison(state, 5f, 6f);
        }
    },

    BUTTER {
        @Override
        public void onHit(Zombie target, GameState state) {
            target.applyButter(40);
        }
    };

    public abstract void onHit(Zombie target, GameState state);

    public void onHit(Zombie target, GameState state, int durationTicks) {
        onHit(target, state);
    }
}
