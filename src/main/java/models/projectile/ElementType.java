package models.projectile;

import models.Zombie.Zombie;
import models.games.GameState;

public enum ElementType {

    NORMAL {
        @Override
        public void onHit(Zombie target, GameState state) {
            // no extra effect
        }
    },

    FIRE {
        @Override
        public void onHit(Zombie target, GameState state) {
            target.clearColdEffects();
        }
    },

    ICE {
        @Override
        public void onHit(Zombie target, GameState state) {
            // Slows the zombie for a while (Snow Pea, Winter Melon).
            target.applyChill(60); // 50% speed for 6s
        }
    },

    POISON {
        @Override
        public void onHit(Zombie target, GameState state) {
            target.applyPoison(state, 5f, 6f); // 5 dmg/s for 6s
        }
    },

    BUTTER {
        @Override
        public void onHit(Zombie target, GameState state) {
            target.applyButter(40); // fully stopped for 4s
        }
    };

    public abstract void onHit(Zombie target, GameState state);
}
