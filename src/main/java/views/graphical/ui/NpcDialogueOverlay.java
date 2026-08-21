package views.graphical.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import views.graphical.animation.PamAnimationActor;
import views.graphical.dialogue.NpcDialogueSequence;

import java.util.List;

public final class NpcDialogueOverlay extends Table {
    private static final String SPEECH_BUBBLE_ASSET =
            "IMAGE_STORE_SPEECHBUBBLE2";
    private static final float TYPE_SECONDS_PER_CHARACTER = 0.035f;
    private static final float ENTER_FALLBACK_DURATION = 1.4f;
    private static final float LEAVE_FALLBACK_DURATION = 1.4f;
    private static final float NPC_SCALE = 0.6f;
    private static final float BACKGROUND_DIM_ALPHA = 0.52f;
    private static final float BUBBLE_X_RATIO = 0.5f;
    private static final float BUBBLE_TOP_MARGIN = 200f;

    private enum State {
        ENTERING,
        TYPING,
        WAITING_FOR_ENTER,
        LEAVING,
        FINISHED
    }

    private final PvzGame game;
    private final NpcDialogueSequence sequence;
    private final Runnable onFinished;
    private final PamAnimationActor npcActor;
    private final Image speechBubble;
    private final Label dialogueLabel;
    private final Label continueLabel;

    private State state = State.ENTERING;
    private int lineIndex = -1;
    private int visibleCharacters;
    private float phaseTime;
    private float typingTime;
    private float phaseDuration;
    private boolean finishCallbackRun;

    public NpcDialogueOverlay(
            PvzGame game,
            NpcDialogueSequence sequence,
            Runnable onFinished
    ) {
        this.game = game;
        this.sequence = sequence;
        this.onFinished = onFinished;

        setFillParent(true);
        setTouchable(Touchable.enabled);
        setBackground(
                game.getSkin().newDrawable(
                        "white_pixel",
                        new Color(0f, 0f, 0f, BACKGROUND_DIM_ALPHA)
                )
        );

        validatePamClips();
        speechBubble = createSpeechBubble();
        dialogueLabel = createDialogueLabel();
        continueLabel = createContinueLabel();
        npcActor = createNpcActor();

        addActor(speechBubble);
        addActor(dialogueLabel);
        addActor(continueLabel);
        addActor(npcActor);

        installInputListener();
        beginEnterAnimation();
    }

    private void validatePamClips() {
        game.getPamPlayer().loadSync(sequence.pamPath());
        List<String> clips = game.getPamPlayer().clips(sequence.pamPath());

        requireClip(clips, sequence.enterClip());
        requireClip(clips, sequence.idleClip());
        requireClip(clips, sequence.leaveClip());
        for (String talkClip : sequence.talkClips()) {
            requireClip(clips, talkClip);
        }
    }

    private void requireClip(List<String> clips, String requiredClip) {
        if (clips != null) {
            for (String clip : clips) {
                if (clip.equalsIgnoreCase(requiredClip)) {
                    return;
                }
            }
        }

        throw new IllegalStateException(
                "Missing Crazy Dave animation clip '"
                        + requiredClip
                        + "' in "
                        + sequence.pamPath()
        );
    }

    private Image createSpeechBubble() {
        TextureRegion region = game.getTextureBank().region(SPEECH_BUBBLE_ASSET);
        if (region == null) {
            throw new IllegalStateException(
                    "Missing speech bubble asset: " + SPEECH_BUBBLE_ASSET
            );
        }

        Image image = new Image(new TextureRegionDrawable(region));
        image.setScaling(Scaling.none);
        image.setTouchable(Touchable.disabled);
        image.setVisible(false);
        return image;
    }

    private Label createDialogueLabel() {
        Label label = new Label(
                "",
                game.getSkin().get(
                        "medium",
                        Label.LabelStyle.class
                )
        );
        label.setColor(Color.valueOf("3A2416"));
        label.setAlignment(Align.topLeft);
        label.setWrap(true);
        label.setTouchable(Touchable.disabled);
        label.setVisible(false);
        return label;
    }

    private Label createContinueLabel() {
        Label label = new Label(
                "tap enter to continue",
                game.getSkin().get(
                        "medium",
                        Label.LabelStyle.class
                )
        );
        label.setColor(Color.valueOf("6F3E1C"));
        label.setAlignment(Align.center);
        label.setFontScale(0.72f);
        label.setTouchable(Touchable.disabled);
        label.setVisible(false);
        return label;
    }

    private PamAnimationActor createNpcActor() {
        PamAnimationActor actor = game.createPamActor(
                sequence.pamPath(),
                sequence.enterClip(),
                0f,
                0f,
                false
        );
        actor.setScale(NPC_SCALE);
        actor.setTouchable(Touchable.disabled);
        return actor;
    }

    private void installInputListener() {
        addListener(
                new InputListener() {
                    @Override
                    public boolean touchDown(
                            InputEvent event,
                            float x,
                            float y,
                            int pointer,
                            int button
                    ) {
                        handleAdvanceInput();
                        return true;
                    }

                    @Override
                    public boolean keyDown(
                            InputEvent event,
                            int keycode
                    ) {
                        if (keycode != Input.Keys.ENTER) {
                            return false;
                        }

                        handleAdvanceInput();
                        return true;
                    }
                }
        );
    }

    private void beginEnterAnimation() {
        state = State.ENTERING;
        phaseTime = 0f;
        phaseDuration = clipDuration(
                sequence.enterClip(),
                ENTER_FALLBACK_DURATION
        );
        npcActor.play(sequence.enterClip(), false);
        npcActor.restart();
    }

    private void beginLine(int nextLineIndex) {
        lineIndex = nextLineIndex;
        visibleCharacters = 0;
        typingTime = 0f;
        state = State.TYPING;

        dialogueLabel.setText("");
        dialogueLabel.setVisible(true);
        speechBubble.setVisible(true);
        continueLabel.setVisible(false);

        String talkClip = sequence.talkClips().get(
                lineIndex % sequence.talkClips().size()
        );
        npcActor.play(talkClip, true);
        npcActor.restart();
    }

    private void finishTyping() {
        String line = sequence.lines().get(lineIndex);
        dialogueLabel.setText(line);
        visibleCharacters = line.length();
        state = State.WAITING_FOR_ENTER;
        continueLabel.setVisible(true);
        npcActor.play(sequence.idleClip(), true);
        npcActor.restart();
    }

    private void handleAdvanceInput() {
        if (state == State.TYPING) {
            finishTyping();
            return;
        }

        if (state == State.WAITING_FOR_ENTER) {
            advanceDialogue();
        }
    }

    private void advanceDialogue() {
        if (state != State.WAITING_FOR_ENTER) {
            return;
        }

        int nextLine = lineIndex + 1;
        if (nextLine < sequence.lines().size()) {
            beginLine(nextLine);
            return;
        }

        beginLeaveAnimation();
    }

    private void beginLeaveAnimation() {
        state = State.LEAVING;
        phaseTime = 0f;
        phaseDuration = clipDuration(
                sequence.leaveClip(),
                LEAVE_FALLBACK_DURATION
        );

        speechBubble.setVisible(false);
        dialogueLabel.setVisible(false);
        continueLabel.setVisible(false);

        npcActor.play(sequence.leaveClip(), false);
        npcActor.restart();
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        switch (state) {
            case ENTERING -> updateEntering(delta);
            case TYPING -> updateTyping(delta);
            case LEAVING -> updateLeaving(delta);
            case WAITING_FOR_ENTER, FINISHED -> {
            }
        }
    }

    private void updateEntering(float delta) {
        phaseTime += Math.max(0f, delta);
        if (phaseTime >= phaseDuration) {
            beginLine(0);
        }
    }

    private void updateTyping(float delta) {
        String line = sequence.lines().get(lineIndex);
        typingTime += Math.max(0f, delta);

        int characters = Math.min(
                line.length(),
                (int) (typingTime / TYPE_SECONDS_PER_CHARACTER)
        );

        if (characters != visibleCharacters) {
            visibleCharacters = characters;
            dialogueLabel.setText(line.substring(0, visibleCharacters));
        }

        if (visibleCharacters >= line.length()) {
            finishTyping();
        }
    }

    private void updateLeaving(float delta) {
        phaseTime += Math.max(0f, delta);
        if (phaseTime < phaseDuration) {
            return;
        }

        state = State.FINISHED;
        remove();
        runFinishCallback();
    }

    private void runFinishCallback() {
        if (finishCallbackRun) {
            return;
        }
        finishCallbackRun = true;
        if (onFinished != null) {
            onFinished.run();
        }
    }

    private float clipDuration(String clip, float fallback) {
        try {
            return Math.max(
                    0.05f,
                    game.getPamPlayer().clipDurationSeconds(
                            sequence.pamPath(),
                            clip
                    )
            );
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    @Override
    public void layout() {
        super.layout();
        layoutSpeechBubble();
        layoutNpc();
    }

    private void layoutSpeechBubble() {
        float bubbleWidth = speechBubble.getDrawable().getMinWidth();
        float bubbleHeight = speechBubble.getDrawable().getMinHeight();
        float bubbleX = Math.max(28f, getWidth() * BUBBLE_X_RATIO);
        float bubbleY = getHeight() - bubbleHeight - BUBBLE_TOP_MARGIN;

        speechBubble.setBounds(
                bubbleX,
                bubbleY,
                bubbleWidth,
                bubbleHeight
        );

        float scaleX = bubbleWidth / 620f;
        float scaleY = bubbleHeight / 220f;

        dialogueLabel.setBounds(
                bubbleX + 58f * scaleX,
                bubbleY + 62f * scaleY,
                bubbleWidth - 116f * scaleX,
                bubbleHeight - 104f * scaleY
        );

        continueLabel.setBounds(
                bubbleX + 70f * scaleX,
                bubbleY + 24f * scaleY,
                bubbleWidth - 140f * scaleX,
                28f * scaleY
        );
    }

    private void layoutNpc() {
        float targetX = getWidth() * 0.40f;
        float targetY = getHeight() * 0.5f;

        npcActor.setPosition(targetX, targetY);
    }
}
