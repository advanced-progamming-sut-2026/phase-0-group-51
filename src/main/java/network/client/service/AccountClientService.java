package network.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import network.client.NetworkClient;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.auth.RegisterRequest;
import network.protocol.auth.RegisterResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.auth.LogoutResponse;

public class AccountClientService {
    private final NetworkClient networkClient;
    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public AccountClientService(
            NetworkClient networkClient
    ) {
        this.networkClient = networkClient;
    }

    public CompletableFuture<RegisterResponse> register(
            RegisterRequest request
    ) throws IOException {
        String payload = codec.encodePayload(request);

        NetworkMessage message =
                new NetworkMessage(
                        MessageType.REGISTER_REQUEST,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(message)
                .thenApply(this::decodeRegisterResponse);
    }

    private RegisterResponse decodeRegisterResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new RegisterResponse(
                    false,
                    message.getPayload()
            );
        }

        if (message.getType()
                != MessageType.REGISTER_RESPONSE) {
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
                    RegisterResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }

    public CompletableFuture<LoginResponse> login(
            LoginRequest request
    ) throws IOException {
        String payload =
                codec.encodePayload(request);

        NetworkMessage message =
                new NetworkMessage(
                        MessageType.LOGIN_REQUEST,
                        UUID.randomUUID().toString(),
                        payload
                );

        return networkClient
                .sendRequest(message)
                .thenApply(this::decodeLoginResponse);
    }

    private LoginResponse decodeLoginResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new LoginResponse(
                    false,
                    message.getPayload(),
                    null
            );
        }

        if (message.getType()
                != MessageType.LOGIN_RESPONSE) {
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
                    LoginResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }

    public CompletableFuture<LogoutResponse> logout()
            throws IOException {
        NetworkMessage message =
                new NetworkMessage(
                        MessageType.LOGOUT_REQUEST,
                        UUID.randomUUID().toString(),
                        null
                );

        return networkClient
                .sendRequest(message)
                .thenApply(this::decodeLogoutResponse);
    }

    private LogoutResponse decodeLogoutResponse(
            NetworkMessage message
    ) {
        if (message.getType() == MessageType.ERROR) {
            return new LogoutResponse(
                    false,
                    message.getPayload()
            );
        }

        if (message.getType()
                != MessageType.LOGOUT_RESPONSE) {
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
                    LogoutResponse.class
            );
        } catch (JsonProcessingException exception) {
            throw new CompletionException(exception);
        }
    }
}