package Data.loader;

public record PlantAnimationData(
        String clip,
        boolean loop,
        float duration
) {
    public PlantAnimationData {
        if (clip == null || clip.isBlank()) {
            throw new IllegalArgumentException(
                    "Animation clip cannot be blank."
            );
        }

        if (duration < 0f) {
            throw new IllegalArgumentException(
                    "Animation duration cannot be negative."
            );
        }
    }
}