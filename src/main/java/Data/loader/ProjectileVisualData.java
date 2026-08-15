package Data.loader;

import java.util.List;

public record ProjectileVisualData(
        String pamPath,
        String clip,
        float scale,
        List<ProjectileReleaseData> releases
) {
    public ProjectileReleaseData release(int id) {
        return releases.stream()
                .filter(release -> release.id() == id)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Projectile release id not found: " + id
                        )
                );
    }
}