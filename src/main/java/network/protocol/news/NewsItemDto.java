package network.protocol.news;

import models.items.News;

public class NewsItemDto {
    private int id;
    private String message;
    private String createdAt;
    private boolean read;

    public NewsItemDto() {
    }

    public NewsItemDto(
            int id,
            String message,
            String createdAt,
            boolean read
    ) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public static NewsItemDto fromNews(News news) {
        return new NewsItemDto(
                news.getId(),
                news.getMessage(),
                news.getCreatedAt(),
                news.isRead()
        );
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
