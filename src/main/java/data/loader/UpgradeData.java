package data.loader;

import java.util.List;

public record UpgradeData(int level, String description, List<StatModifierData> modifiers) {}
