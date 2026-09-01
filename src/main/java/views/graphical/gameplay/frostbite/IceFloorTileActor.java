package views.graphical.gameplay.frostbite;

import pvz.libpvz.pam.PamPlayer;
import views.graphical.animation.PamAnimationActor;

public final class IceFloorTileActor extends PamAnimationActor {

    private enum Phase {
        START,
        IDLE,
        END
    }

    private static final int IDLE_LOOPS = 2;

    private final String startClip;
    private final String idleClip;
    private final String endClip;

    private final float startDuration;
    private final float idleDuration;
    private final float endDuration;

    private Phase phase = Phase.START;
    private float phaseTime;

    public IceFloorTileActor(
        PamPlayer pamPlayer,
        String pamPath,
        String startClip,
        String idleClip,
        String endClip,
        float startDuration,
        float idleClipDuration,
        float endDuration
    ) {
        super(pamPlayer, pamPath, startClip, false);
        this.startClip = startClip;
        this.idleClip = idleClip;
        this.endClip = endClip;
        this.startDuration = Math.max(0.05f, startDuration);
        this.idleDuration = Math.max(0.05f, idleClipDuration) * IDLE_LOOPS;
        this.endDuration = Math.max(0.05f, endDuration);
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        phaseTime += Math.max(0f, delta);

        switch (phase) {
            case START -> {
                if (phaseTime >= startDuration) {
                    phase = Phase.IDLE;
                    phaseTime = 0f;
                    play(idleClip, true);
                }
            }
            case IDLE -> {
                if (phaseTime >= idleDuration) {
                    phase = Phase.END;
                    phaseTime = 0f;
                    play(endClip, false);
                }
            }
            case END -> {
                if (phaseTime >= endDuration) {
                    phase = Phase.START;
                    phaseTime = 0f;
                    play(startClip, false);
                }
            }
        }
    }
}
