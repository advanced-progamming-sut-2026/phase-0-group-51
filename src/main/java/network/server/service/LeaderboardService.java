package network.server.service;

import Data.database.LeaderBoardRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import models.leaderBoard.LeaderBoard;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.leaderboard.LeaderboardEntryDto;
import network.protocol.leaderboard.LeaderboardResponse;
import network.server.ClientConnection;

import java.util.List;

public class LeaderboardService {
    private final LeaderBoardRepository repository =
            new LeaderBoardRepository();
    private final NetworkJsonCodec codec = new NetworkJsonCodec();

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        LeaderboardResponse response;

        if (!isAuthenticated(connection)) {
            response = new LeaderboardResponse(
                    false,
                    "You must log in first.",
                    List.of()
            );
        } else {
            response = loadLeaderboard();
        }

        try {
            return new NetworkMessage(
                    MessageType.LEADERBOARD_GET_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Could not create leaderboard response."
            );
        }
    }

    private LeaderboardResponse loadLeaderboard() {
        try {
            List<LeaderboardEntryDto> entries = repository
                    .getAllEntries()
                    .stream()
                    .map(LeaderboardEntryDto::fromModel)
                    .toList();

            return new LeaderboardResponse(
                    true,
                    "Leaderboard loaded.",
                    entries
            );
        } catch (IllegalStateException exception) {
            return new LeaderboardResponse(
                    false,
                    exception.getMessage(),
                    List.of()
            );
        }
    }

    private boolean isAuthenticated(ClientConnection connection) {
        return connection != null
                && connection.getSession() != null
                && connection.getSession().isAuthenticated();
    }
}
