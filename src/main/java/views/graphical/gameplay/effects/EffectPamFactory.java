package views.graphical.gameplay.effects;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import graphics.PvzGame;
import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;

import java.util.List;

public final class EffectPamFactory {

    private EffectPamFactory() {
    }

    public record OneShot(
            PamAnimationActor actor,
            float duration
    ) {
    }

    public static OneShot create(
            PvzGame game,
            String pamPath,
            float scale,
            float fallbackDuration,
            String... clipCandidates
    ) {
        PamPlayer pamPlayer = game.getPamPlayer();
        pamPlayer.loadSync(pamPath);

        List<String> clips = pamPlayer.clips(pamPath);
        if (clips == null || clips.isEmpty()) {
            throw new IllegalStateException(
                    "Effect PAM has no clips: " + pamPath
            );
        }

        String clip = findClip(clips, clipCandidates);
        if (clip == null) {
            clip = clips.getFirst();
        }

        PamAnimationActor actor = game.createPamActor(
                pamPath,
                clip,
                0f,
                0f,
                false
        );

        actor.setScale(scale);
        actor.setTouchable(Touchable.disabled);

        float duration = fallbackDuration;
        try {
            duration = Math.max(
                    0.05f,
                    pamPlayer.clipDurationSeconds(
                            pamPath,
                            clip
                    )
            );
        } catch (RuntimeException ignored) {
        }

        return new OneShot(actor, duration);
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

        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase()
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

}
