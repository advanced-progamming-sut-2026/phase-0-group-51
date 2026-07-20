package models.projectile;

import models.Zombie.Zombie;
import models.Plant.Plant;
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
    },

    HYPNOTIZE {
        @Override
        public void onHit(Zombie target, GameState state) {
            if (target.isDead() || target.isHypnotized()) {
                return;
            }
            target.setHypnotized(true);
            if (target.getDirection() > 0) {
                target.reverseDirection();
            }
            System.out.printf(
                    "[DEBUG][CAULIPOWER] %s was hypnotized at row %d, x=%.2f.%n",
                    target.getAlias(),
                    target.getLane() + 1,
                    target.getX()
            );
        }
    };

    public abstract void onHit(Zombie target, GameState state);

    public void onHit(Zombie target, GameState state, int durationTicks) {
        onHit(target, state);
    }

    public void onHit(
            Zombie target, GameState state, int durationTicks, Plant sourcePlant
    ) {
        if (this == POISON) {
            float damagePerSecond = sourcePlant == null
                    ? 5f
                    : (float) sourcePlant.getPlantStat().poisonDamage();
            target.applyPoison(
                    state,
                    damagePerSecond,
                    6f,
                    sourcePlant
            );
            return;
        }
        onHit(target, state, durationTicks);
    }
}
