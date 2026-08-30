package network.client;

import network.protocol.news.NewsItemDto;
import network.protocol.news.NewsResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ClientNewsState {
    private static boolean loaded;
    private static List<NewsItemDto> news = List.of();
    private static int unreadCount;
    private static Set<String> discoveredZombieAliases = Set.of();

    private ClientNewsState() {
    }

    public static synchronized void apply(NewsResponse response) {
        if (response == null || !response.isSuccess()) {
            return;
        }

        news = List.copyOf(
                response.getNews() == null
                        ? List.of()
                        : response.getNews()
        );
        unreadCount = Math.max(0, response.getUnreadCount());
        discoveredZombieAliases = Set.copyOf(
                response.getDiscoveredZombieAliases() == null
                        ? Set.of()
                        : new LinkedHashSet<>(
                                response.getDiscoveredZombieAliases()
                        )
        );
        loaded = true;
    }

    public static synchronized boolean isLoaded() {
        return loaded;
    }

    public static synchronized List<NewsItemDto> news() {
        return new ArrayList<>(news);
    }

    public static synchronized int unreadCount() {
        return unreadCount;
    }

    public static synchronized Set<String> discoveredZombieAliases() {
        return new LinkedHashSet<>(discoveredZombieAliases);
    }

    public static synchronized boolean isZombieDiscovered(String alias) {
        if (alias == null) {
            return false;
        }
        for (String existing : discoveredZombieAliases) {
            if (existing.equalsIgnoreCase(alias)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized void clear() {
        loaded = false;
        news = List.of();
        unreadCount = 0;
        discoveredZombieAliases = Set.of();
    }
}
