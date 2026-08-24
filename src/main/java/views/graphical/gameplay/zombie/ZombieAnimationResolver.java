package views.graphical.gameplay.zombie;

import Data.loader.ZombieRegistry;
import com.badlogic.gdx.Gdx;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.EntityAnimationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ZombieAnimationResolver {

    private final PamPlayer pamPlayer;
    private final Map<String, ResolvedAnimations> cache = new HashMap<>();

    public ZombieAnimationResolver(PamPlayer pamPlayer) {
        this.pamPlayer = Objects.requireNonNull(pamPlayer, "pamPlayer");
    }

    public ResolvedAnimations resolve(String alias, String pamPath) {
        if (pamPath == null || pamPath.isBlank()) {
            throw new IllegalArgumentException("pamPath cannot be blank");
        }

        String cacheKey = String.valueOf(alias) + "\n" + pamPath;
        return cache.computeIfAbsent(
            cacheKey,
            ignored -> resolveUncached(alias, pamPath)
        );
    }

    public void clearCache() {
        cache.clear();
    }

    private ResolvedAnimations resolveUncached(
        String alias,
        String pamPath
    ) {
        pamPlayer.loadSync(pamPath);

        List<String> available = pamPlayer.clips(pamPath);
        if (available == null || available.isEmpty()) {
            throw new IllegalStateException(
                "PAM has no animation clips: " + pamPath
            );
        }

        List<String> clips = Collections.unmodifiableList(
            new ArrayList<>(available)
        );

        EnumMap<EntityAnimationState, String> resolved =
            new EnumMap<>(EntityAnimationState.class);
        if (alias.equals("IZombieSunProducer")) {
            alias = "ZombieDefault";
        }
        String idle = findClip(
            clips,
            ZombieRegistry.getIdleClip(alias),
            "idle",
            "idling",
            "stand",
            "standing"
        );
        if (idle == null) {
            idle = clips.get(0);
        }

        String walk = findClip(
            clips,
            ZombieRegistry.getWalkClip(alias),
            "walk",
            "walking",
            "shuffle",
            "move",
            "moving"
        );
        if (walk == null) {
            walk = idle;
        }

        String eat = findClip(
            clips,
            "eat",
            "eating",
            "chew",
            "chewing",
            "bite",
            "attack"
        );
        if (eat == null) {
            eat = idle;
        }

        String attack = findClip(
            clips,
            "attack",
            "attacking",
            "bite",
            "eat",
            "eating"
        );
        if (attack == null) {
            attack = eat;
        }

        String death = findClip(
            clips,
            "death",
            "die",
            "dying",
            "dead",
            "death1",
            "death2"
        );
        if (death == null) {
            death = idle;
        }

        String special = findClip(
            clips,
            "special",
            "ability",
            "action",
            "skill"
        );
        if (special == null) {
            special = idle;
        }

        resolved.put(EntityAnimationState.IDLE, idle);
        resolved.put(EntityAnimationState.WALK, walk);
        resolved.put(EntityAnimationState.EAT, eat);
        resolved.put(EntityAnimationState.ATTACK, attack);
        resolved.put(EntityAnimationState.SPECIAL, special);
        resolved.put(EntityAnimationState.DEATH, death);

        ResolvedAnimations result = new ResolvedAnimations(
            pamPath,
            clips,
            resolved
        );
        return result;
    }

    private static String findClip(
        List<String> clips,
        String... candidates
    ) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            for (String clip : clips) {
                if (clip.equalsIgnoreCase(candidate)) {
                    return clip;
                }
            }
        }

        for (String candidate : candidates) {
            String wanted = normalize(candidate);
            if (wanted.isEmpty()) {
                continue;
            }

            for (String clip : clips) {
                if (normalize(clip).equals(wanted)) {
                    return clip;
                }
            }
        }

        for (String candidate : candidates) {
            String wanted = normalize(candidate);
            if (wanted.isEmpty()) {
                continue;
            }

            for (String clip : clips) {
                String normalizedClip = normalize(clip);
                if (normalizedClip.contains(wanted)
                    || wanted.contains(normalizedClip)) {
                    return clip;
                }
            }
        }

        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
    }

    public static final class ResolvedAnimations {
        private final String pamPath;
        private final List<String> availableClips;
        private final EnumMap<EntityAnimationState, String> clipsByState;

        private ResolvedAnimations(
            String pamPath,
            List<String> availableClips,
            EnumMap<EntityAnimationState, String> clipsByState
        ) {
            this.pamPath = pamPath;
            this.availableClips = availableClips;
            this.clipsByState = new EnumMap<>(clipsByState);
        }

        public String getPamPath() {
            return pamPath;
        }

        public List<String> getAvailableClips() {
            return availableClips;
        }

        public String clip(EntityAnimationState state) {
            String clip = clipsByState.get(state);
            if (clip != null) {
                return clip;
            }
            return clipsByState.get(EntityAnimationState.IDLE);
        }

        public boolean hasDistinctClip(EntityAnimationState state) {
            String clip = clipsByState.get(state);
            String idle = clipsByState.get(EntityAnimationState.IDLE);
            return clip != null && !clip.equals(idle);
        }

        public String describe() {
            return "idle=" + clip(EntityAnimationState.IDLE)
                + ", walk=" + clip(EntityAnimationState.WALK)
                + ", eat=" + clip(EntityAnimationState.EAT)
                + ", attack=" + clip(EntityAnimationState.ATTACK)
                + ", special=" + clip(EntityAnimationState.SPECIAL)
                + ", death=" + clip(EntityAnimationState.DEATH)
                + ", available=" + availableClips;
        }
    }
}
