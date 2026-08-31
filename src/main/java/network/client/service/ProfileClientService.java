package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.profile.ProfileDifficultyUpdateRequest;
import network.protocol.profile.ProfilePasswordChangeRequest;
import network.protocol.profile.ProfileResponse;
import network.protocol.profile.ProfileUpdateRequest;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ProfileClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public ProfileClientService(
            NetworkClient networkClient
    ) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<ProfileResponse> getProfile()
            throws IOException {
        NetworkMessage message =
                new NetworkMessage(
                        MessageType.PROFILE_GET_REQUEST,
                        UUID.randomUUID().toString(),
                        null
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        response -> decodeResponse(
                                response,
                                MessageType.PROFILE_GET_RESPONSE
                        )
                );
    }

    public CompletableFuture<ProfileResponse> updateProfile(
            ProfileUpdateRequest request
    ) throws IOException {
        String payload = codec.encodePayload(request);

        NetworkMessage message =
                new NetworkMessage(
                        MessageType.PROFILE_UPDATE_REQUEST,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        response -> decodeResponse(
                                response,
                                MessageType.PROFILE_UPDATE_RESPONSE
                        )
                );
    }

    public CompletableFuture<ProfileResponse> updateDifficulty(
            int difficultyLevel
    ) throws IOException {
        String payload = codec.encodePayload(
                new ProfileDifficultyUpdateRequest(difficultyLevel)
        );

        NetworkMessage message =
                new NetworkMessage(
                        MessageType.PROFILE_DIFFICULTY_UPDATE_REQUEST,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        response -> decodeResponse(
                                response,
                                MessageType.PROFILE_DIFFICULTY_UPDATE_RESPONSE
                        )
                );
    }

    public CompletableFuture<ProfileResponse> changePassword(
            ProfilePasswordChangeRequest request
    ) throws IOException {
        String payload = codec.encodePayload(request);

        NetworkMessage message =
                new NetworkMessage(
                        MessageType.PROFILE_PASSWORD_CHANGE_REQUEST,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(message)
                .thenApply(
                        response -> decodeResponse(
                                response,
                                MessageType.PROFILE_PASSWORD_CHANGE_RESPONSE
                        )
                );
    }

    private ProfileResponse decodeResponse(
            NetworkMessage message,
            MessageType expectedType
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new ProfileResponse(
                    false,
                    message.getPayload(),
                    null
            );
        }

        if (message.getType() != expectedType) {
            throw new CompletionException(
                    new IllegalStateException(
                            "Unexpected response type: "
                                    + message.getType()
                    )
            );
        }

        try {
            return codec.decodePayload(
                    message.getPayload(),
                    ProfileResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}
