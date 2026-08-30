package network.client;

import models.App;
import models.User;
import models.quests.QuestType;
import network.protocol.quests.QuestEntryDto;
import network.protocol.quests.QuestResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientQuestState {
    private static boolean loaded;
    private static List<QuestEntryDto> entries = List.of();

    private ClientQuestState() {
    }

    public static synchronized void apply(
            QuestResponse response,
            boolean accountPlantStateMayHaveChanged
    ) {
        if (response == null) {
            return;
        }

        entries = response.getEntries() == null
                ? List.of()
                : List.copyOf(response.getEntries());
        loaded = true;

        User user = App.getInstance().getLoggedInUser();
        if (user != null) {
            user.setCoins(response.getCoins());
            user.setGems(response.getGems());
            user.setQuestDailyNum(response.getQuestDailyNum());
            user.setQuestNonDailyNum(response.getQuestNonDailyNum());
        }

        ClientPlantOwnershipState.replaceWith(
                response.getUnlockedPlantIds()
        );

        if (accountPlantStateMayHaveChanged) {
            ClientShopState.clear();
        }
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized List<QuestEntryDto> getEntries() {
        return new ArrayList<>(entries);
    }

    public static synchronized List<QuestEntryDto> getEntries(
            QuestType type
    ) {
        return entries.stream()
                .filter(entry -> entry != null && entry.getType() == type)
                .sorted(
                        Comparator.comparingInt(
                                (QuestEntryDto entry) ->
                                        entry.getPriority().ordinal()
                        ).thenComparingInt(
                                QuestEntryDto::getQuestId
                        )
                )
                .toList();
    }

    public static synchronized void clear() {
        loaded = false;
        entries = List.of();
    }
}
