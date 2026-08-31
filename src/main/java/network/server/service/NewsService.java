package network.server.service;

import Data.database.NewsRepository;
import Data.loader.ZombieRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.items.News;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.news.NewsItemDto;
import network.protocol.news.NewsResponse;
import network.protocol.news.ZombieDiscoverRequest;
import network.server.ClientConnection;

import java.util.ArrayList;
import java.util.List;

public class NewsService {
    private final NewsRepository newsRepository = new NewsRepository();
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        NewsResponse response = userId == null
                ? failure("You must log in first.")
                : snapshot(userId, true, "News loaded.");

        return encode(
                message.getRequestId(),
                MessageType.NEWS_GET_RESPONSE,
                response
        );
    }

    public NetworkMessage handleMarkAllRead(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return encode(
                    message.getRequestId(),
                    MessageType.NEWS_MARK_ALL_READ_RESPONSE,
                    failure("You must log in first.")
            );
        }

        if (!newsRepository.markAllAsRead(userId)) {
            return encode(
                    message.getRequestId(),
                    MessageType.NEWS_MARK_ALL_READ_RESPONSE,
                    failure("News could not be marked as read.")
            );
        }

        return encode(
                message.getRequestId(),
                MessageType.NEWS_MARK_ALL_READ_RESPONSE,
                snapshot(userId, true, "News marked as read.")
        );
    }

    public NetworkMessage handleZombieDiscover(
            ClientConnection connection,
            NetworkMessage message
    ) {
        Integer userId = authenticatedUserId(connection);
        if (userId == null) {
            return encode(
                    message.getRequestId(),
                    MessageType.ZOMBIE_DISCOVER_RESPONSE,
                    failure("You must log in first.")
            );
        }

        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Zombie discovery payload is required."
            );
        }

        try {
            ZombieDiscoverRequest request = codec.decodePayload(
                    message.getPayload(),
                    ZombieDiscoverRequest.class
            );

            String alias = request == null
                    || request.getZombieAlias() == null
                    ? ""
                    : request.getZombieAlias().trim();

            if (alias.isBlank()
                    || ZombieRegistry.getTemplate(alias) == null) {
                return encode(
                        message.getRequestId(),
                        MessageType.ZOMBIE_DISCOVER_RESPONSE,
                        failure("Zombie alias is invalid.")
                );
            }

            boolean newlyDiscovered = newsRepository.discoverZombie(
                    userId,
                    alias
            );

            return encode(
                    message.getRequestId(),
                    MessageType.ZOMBIE_DISCOVER_RESPONSE,
                    snapshot(
                            userId,
                            true,
                            newlyDiscovered
                                    ? "Zombie discovery saved."
                                    : "Zombie was already discovered."
                    )
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid zombie discovery payload."
            );
        }
    }

    private NewsResponse snapshot(
            int userId,
            boolean success,
            String message
    ) {
        List<NewsItemDto> items = new ArrayList<>();
        for (News news : newsRepository.getNewsForUser(userId)) {
            items.add(NewsItemDto.fromNews(news));
        }

        return new NewsResponse(
                success,
                message,
                items,
                newsRepository.countUnreadNews(userId),
                new ArrayList<>(
                        newsRepository.getDiscoveredZombieAliases(userId)
                )
        );
    }

    private NewsResponse failure(String message) {
        return new NewsResponse(
                false,
                message,
                List.of(),
                0,
                List.of()
        );
    }

    private Integer authenticatedUserId(
            ClientConnection connection
    ) {
        if (connection == null
                || connection.getSession() == null
                || !connection.getSession().isAuthenticated()) {
            return null;
        }
        return connection.getSession().getUserId();
    }

    private NetworkMessage encode(
            String requestId,
            MessageType responseType,
            NewsResponse response
    ) {
        try {
            return new NetworkMessage(
                    responseType,
                    requestId,
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    requestId,
                    "Could not create news response."
            );
        }
    }
}
