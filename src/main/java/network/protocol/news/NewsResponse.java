package network.protocol.news;

import java.util.ArrayList;
import java.util.List;

public class NewsResponse {
    private boolean success;
    private String message;
    private List<NewsItemDto> news;
    private int unreadCount;
    private List<String> discoveredZombieAliases;

    public NewsResponse() {
        this.news = new ArrayList<>();
        this.discoveredZombieAliases = new ArrayList<>();
    }

    public NewsResponse(
            boolean success,
            String message,
            List<NewsItemDto> news,
            int unreadCount,
            List<String> discoveredZombieAliases
    ) {
        this.success = success;
        this.message = message;
        this.news = news == null ? new ArrayList<>() : new ArrayList<>(news);
        this.unreadCount = Math.max(0, unreadCount);
        this.discoveredZombieAliases = discoveredZombieAliases == null
                ? new ArrayList<>()
                : new ArrayList<>(discoveredZombieAliases);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<NewsItemDto> getNews() {
        return news;
    }

    public void setNews(List<NewsItemDto> news) {
        this.news = news == null ? new ArrayList<>() : new ArrayList<>(news);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = Math.max(0, unreadCount);
    }

    public List<String> getDiscoveredZombieAliases() {
        return discoveredZombieAliases;
    }

    public void setDiscoveredZombieAliases(List<String> discoveredZombieAliases) {
        this.discoveredZombieAliases = discoveredZombieAliases == null
                ? new ArrayList<>()
                : new ArrayList<>(discoveredZombieAliases);
    }
}
