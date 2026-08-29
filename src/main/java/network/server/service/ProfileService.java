package network.server.service;

import Data.database.AuthSessionRepository;
import Data.database.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.HashUtil;
import controllers.validation.SignUpValidation;
import models.User;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.profile.ProfileDataDto;
import network.protocol.profile.ProfilePasswordChangeRequest;
import network.protocol.profile.ProfileResponse;
import network.protocol.profile.ProfileUpdateRequest;
import network.server.ClientConnection;

public class ProfileService {
    private final UserRepository userRepository =
            new UserRepository();

    private final AuthSessionRepository authSessionRepository =
            new AuthSessionRepository();

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public NetworkMessage handleGet(
            ClientConnection connection,
            NetworkMessage message
    ) {
        ProfileResponse response = getProfile(connection);
        return encodeResponse(
                MessageType.PROFILE_GET_RESPONSE,
                message.getRequestId(),
                response
        );
    }

    public NetworkMessage handleUpdate(
            ClientConnection connection,
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Profile update payload is required."
            );
        }

        try {
            ProfileUpdateRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            ProfileUpdateRequest.class
                    );

            ProfileResponse response =
                    updateProfile(connection, request);

            return encodeResponse(
                    MessageType.PROFILE_UPDATE_RESPONSE,
                    message.getRequestId(),
                    response
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid profile update payload."
            );
        }
    }

    public NetworkMessage handlePasswordChange(
            ClientConnection connection,
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Password change payload is required."
            );
        }

        try {
            ProfilePasswordChangeRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            ProfilePasswordChangeRequest.class
                    );

            ProfileResponse response =
                    changePassword(connection, request);

            return encodeResponse(
                    MessageType.PROFILE_PASSWORD_CHANGE_RESPONSE,
                    message.getRequestId(),
                    response
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid password change payload."
            );
        }
    }

    private ProfileResponse getProfile(
            ClientConnection connection
    ) {
        User user = currentUser(connection);

        if (user == null) {
            return failure("You must log in first.");
        }

        return success(
                "Profile loaded.",
                toProfile(user)
        );
    }

    private ProfileResponse updateProfile(
            ClientConnection connection,
            ProfileUpdateRequest request
    ) {
        User user = currentUser(connection);

        if (user == null) {
            return failure("You must log in first.");
        }

        if (request == null) {
            return failure("Profile data is required.");
        }

        String username = trim(request.getUsername());
        String nickname = trim(request.getNickname());
        String email = trim(request.getEmail());

        SignUpValidation validation =
                new SignUpValidation();

        String validationError = validateProfileFields(
                username,
                nickname,
                email,
                validation
        );

        if (validationError != null) {
            return failure(validationError);
        }

        if (!username.equals(user.getUsername())
                && userRepository.usernameExists(username)) {
            return failure("Username already exists.");
        }

        if (!email.equals(user.getEmail())
                && userRepository.emailExistsForAnotherUser(
                email,
                user.getId()
        )) {
            return failure("Email already exists.");
        }

        boolean updated = userRepository.updateProfile(
                user.getId(),
                username,
                nickname,
                email
        );

        if (!updated) {
            return failure("Profile could not be saved.");
        }

        connection.getSession().authenticate(
                user.getId(),
                username,
                connection.getSession()
                        .getPersistentTokenHash()
        );

        User freshUser =
                userRepository.getUserById(user.getId());

        if (freshUser == null) {
            return failure("Profile was saved but could not be reloaded.");
        }

        return success(
                "Profile updated successfully.",
                toProfile(freshUser)
        );
    }

    private ProfileResponse changePassword(
            ClientConnection connection,
            ProfilePasswordChangeRequest request
    ) {
        User user = currentUser(connection);

        if (user == null) {
            return failure("You must log in first.");
        }

        if (request == null
                || request.getOldPassword() == null
                || request.getOldPassword().isEmpty()) {
            return failure("Please enter your current password.");
        }

        if (request.getNewPassword() == null
                || request.getNewPassword().isEmpty()) {
            return failure("Please enter your new password.");
        }

        String oldPasswordHash =
                HashUtil.hashPassword(
                        request.getOldPassword()
                );

        if (!oldPasswordHash.equals(
                user.getPasswordHash()
        )) {
            return failure("Current password is incorrect.");
        }

        String newPassword = request.getNewPassword();
        String newPasswordHash =
                HashUtil.hashPassword(newPassword);

        if (newPasswordHash.equals(
                user.getPasswordHash()
        )) {
            return failure(
                    "New password must be different from current password."
            );
        }

        SignUpValidation validation =
                new SignUpValidation();

        if (!validation.isPasswordValid(newPassword)) {
            return failure(
                    "Password contains invalid characters."
            );
        }

        if (!validation.isPasswordStrong(newPassword)) {
            return failure(
                    weakPasswordMessage(
                            newPassword,
                            validation
                    )
            );
        }

        if (!userRepository.updatePassword(
                user.getUsername(),
                newPasswordHash
        )) {
            return failure("Password could not be saved.");
        }

        authSessionRepository.deleteAllForUser(
                user.getId()
        );

        connection.getSession().authenticate(
                user.getId(),
                user.getUsername(),
                null
        );

        User freshUser =
                userRepository.getUserById(user.getId());

        return success(
                "Password changed successfully.",
                freshUser == null
                        ? toProfile(user)
                        : toProfile(freshUser)
        );
    }

    private User currentUser(
            ClientConnection connection
    ) {
        if (connection == null
                || !connection.getSession()
                .isAuthenticated()) {
            return null;
        }

        Integer userId =
                connection.getSession().getUserId();

        if (userId == null) {
            return null;
        }

        return userRepository.getUserById(userId);
    }

    private ProfileDataDto toProfile(User user) {
        int passedLevels =
                userRepository.getPassedLevels(user.getId());

        return ProfileDataDto.fromUser(
                user,
                passedLevels
        );
    }

    private String validateProfileFields(
            String username,
            String nickname,
            String email,
            SignUpValidation validation
    ) {
        if (username.isEmpty()) {
            return "Please enter your username.";
        }

        if (!validation.isUsernameValid(username)) {
            return "Username can only contain A-Za-z letters, numbers, and the symbol -.";
        }

        if (nickname.isEmpty()) {
            return "Please enter your nickname.";
        }

        if (!validation.isNicknameLengthValid(nickname)) {
            return "Nickname length must be between 3 and 30 characters.";
        }

        if (email.isEmpty()) {
            return "Please enter your email.";
        }

        if (!validation.hasExactlyOneAtSign(email)) {
            return "Email must contain exactly one @.";
        }

        if (validation.hasInvalidChar(email)) {
            return "Email contains invalid characters.";
        }

        String[] parts = email.split("@", -1);

        if (parts.length != 2
                || !validation.isFirstPartEmailValid(
                parts[0]
        )) {
            return "The first part of email is invalid.";
        }

        if (!validation.isSecondPartEmailValid(
                parts[1]
        )) {
            return "The domain part of email is invalid.";
        }

        return null;
    }

    private String weakPasswordMessage(
            String password,
            SignUpValidation validation
    ) {
        StringBuilder message =
                new StringBuilder("Password is too weak.");

        if (!validation.isWeakPasswordLongEnough(password)) {
            message.append(
                    " It must be at least 8 characters long."
            );
        }

        if (!validation.hasWeakPasswordUpperCaseLetter(password)) {
            message.append(
                    " It must contain an uppercase letter."
            );
        }

        if (!validation.hasWeakPasswordLowerCaseLetter(password)) {
            message.append(
                    " It must contain a lowercase letter."
            );
        }

        if (!validation.hasWeakPasswordNum(password)) {
            message.append(
                    " It must contain a number."
            );
        }

        if (!validation.hasWeakPasswordSpecialSymbol(password)) {
            message.append(
                    " It must contain a special symbol."
            );
        }

        return message.toString();
    }

    private NetworkMessage encodeResponse(
            MessageType type,
            String requestId,
            ProfileResponse response
    ) {
        try {
            return new NetworkMessage(
                    type,
                    requestId,
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    requestId,
                    "Could not encode profile response."
            );
        }
    }

    private ProfileResponse success(
            String message,
            ProfileDataDto profile
    ) {
        return new ProfileResponse(
                true,
                message,
                profile
        );
    }

    private ProfileResponse failure(String message) {
        return new ProfileResponse(
                false,
                message,
                null
        );
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
