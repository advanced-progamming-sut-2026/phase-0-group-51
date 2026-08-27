package network.server.service;

import Data.database.PlantRepository;
import Data.database.UserRepository;
import Data.loader.PlantRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import controllers.HashUtil;
import controllers.validation.SignUpValidation;
import models.User;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.auth.*;
import network.server.ClientConnection;

public class AuthService {
    private final UserRepository userRepository =
            new UserRepository();

    private final NetworkJsonCodec codec =
            new NetworkJsonCodec();

    public NetworkMessage handleRegister(
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Registration payload is required."
            );
        }

        try {
            RegisterRequest request = codec.decodePayload(
                    message.getPayload(),
                    RegisterRequest.class
            );

            RegisterResponse response = register(request);

            return new NetworkMessage(
                    MessageType.REGISTER_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid registration payload."
            );
        }
    }

    public NetworkMessage handleLogin(
            ClientConnection connection,
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Login payload is required."
            );
        }

        try {
            LoginRequest request = codec.decodePayload(
                    message.getPayload(),
                    LoginRequest.class
            );

            LoginResponse response = login(connection, request);

            return new NetworkMessage(
                    MessageType.LOGIN_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid login payload."
            );
        }
    }

    public LoginResponse login(
            ClientConnection connection,
            LoginRequest request
    ) {
        if (connection.getSession().isAuthenticated()) {
            return loginFailure(
                    "This connection is already logged in."
            );
        }

        String validationError =
                validateLoginRequest(request);

        if (validationError != null) {
            return loginFailure(validationError);
        }

        String username = request.getUsername().trim();

        User user =
                userRepository.getUserByUsername(username);

        if (user == null) {
            return loginFailure(
                    "Username does not exist."
            );
        }

        String passwordHash =
                HashUtil.hashPassword(request.getPassword());

        if (!user.getPasswordHash().equals(passwordHash)) {
            return loginFailure(
                    "Password is incorrect."
            );
        }

        PlantRepository.unlockPlantsAndReturnNew(
                user.getId(),
                PlantRegistry.getStarterPlantIds()
        );

        connection.getSession().authenticate(
                user.getId(),
                user.getUsername()
        );

        return new LoginResponse(
                true,
                "Login successful.",
                UserProfileDto.fromUser(user)
        );
    }

    private LoginResponse loginFailure(String message) {
        return new LoginResponse(
                false,
                message,
                null
        );
    }

    private String validateLoginRequest(
            LoginRequest request
    ) {
        if (request == null) {
            return "Login data is required.";
        }

        if (request.getUsername() == null
                || request.getUsername().isBlank()) {
            return "Please enter your username.";
        }

        if (request.getPassword() == null
                || request.getPassword().isEmpty()) {
            return "Please enter your password.";
        }

        return null;
    }

    public NetworkMessage handleLogout(
            ClientConnection connection,
            NetworkMessage message
    ) {
        LogoutResponse response = logout(connection);

        try {
            return new NetworkMessage(
                    MessageType.LOGOUT_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Could not create logout response."
            );
        }
    }

    public LogoutResponse logout(
            ClientConnection connection
    ) {
        if (!connection.getSession().isAuthenticated()) {
            return new LogoutResponse(
                    false,
                    "This connection is not logged in."
            );
        }

        connection.getSession().clear();

        return new LogoutResponse(
                true,
                "Logout successful."
        );
    }

    public RegisterResponse register(
            RegisterRequest request
    ) {
        String error = validateRequest(request);

        if (error != null) {
            return failure(error);
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (userRepository.usernameExists(username)) {
            return failure("Username already exists!");
        }

        if (userRepository.emailExists(email)) {
            return failure("Email already exists!");
        }

        User user = createUser(request);

        if (!userRepository.register(user)) {
            return failure("Registration failed.");
        }

        return new RegisterResponse(
                true,
                "Registration completed successfully."
        );
    }

    private User createUser(RegisterRequest request) {
        String passwordHash =
                HashUtil.hashPassword(request.getPassword());

        return new User(
                request.getUsername().trim(),
                request.getEmail().trim(),
                passwordHash,
                request.getGender().trim(),
                request.getNickname().trim(),
                request.getSecurityQuestion(),
                request.getAnswer().trim()
        );
    }

    private RegisterResponse failure(String message) {
        return new RegisterResponse(false, message);
    }

    private String validateRequest(
            RegisterRequest request
    ) {
        if (request == null) {
            return "Registration data is required.";
        }

        SignUpValidation validation =
                new SignUpValidation();

        String basicError =
                validateBasicFields(request, validation);

        if (basicError != null) {
            return basicError;
        }

        String passwordError =
                validatePassword(request, validation);

        if (passwordError != null) {
            return passwordError;
        }

        String emailError =
                validateEmail(request, validation);

        if (emailError != null) {
            return emailError;
        }

        return validateSecurity(request, validation);
    }

    private String validateBasicFields(
            RegisterRequest request,
            SignUpValidation validation
    ) {
        if (!validation.isNotBlank(request.getUsername())) {
            return "Please enter your username.";
        }

        if (!validation.isUsernameValid(
                request.getUsername().trim())) {
            return "Username can only contain "
                    + "A-Za-z letters, numbers, and the symbol -.";
        }

        if (!validation.isNotBlank(request.getNickname())) {
            return "Please enter your nickname.";
        }

        if (!validation.isNicknameLengthValid(
                request.getNickname().trim())) {
            return "Nickname length must be between "
                    + "3 and 30 characters.";
        }

        if (!validation.isNotBlank(request.getGender())) {
            return "Please enter your gender.";
        }

        if (!validation.isGenderValid(
                request.getGender().trim())) {
            return "Please select a valid gender.";
        }

        return null;
    }

    private String validatePassword(
            RegisterRequest request,
            SignUpValidation validation
    ) {
        String password = request.getPassword();

        if (!validation.isNotBlank(password)) {
            return "Please enter your password.";
        }

        if (!validation.isPasswordValid(password)) {
            return "Password contains invalid characters.";
        }

        if (!validation.isPasswordStrong(password)) {
            return weakPasswordMessage(
                    password,
                    validation
            );
        }

        if (!validation.are2passwordsSame(
                password,
                request.getPasswordConfirm())) {
            return "Passwords do not match.";
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

    private String validateEmail(
            RegisterRequest request,
            SignUpValidation validation
    ) {
        String email = request.getEmail();

        if (!validation.isNotBlank(email)) {
            return "Please enter your email.";
        }

        email = email.trim();

        if (!validation.hasExactlyOneAtSign(email)) {
            return "Email must contain exactly one @.";
        }

        if (validation.hasInvalidChar(email)) {
            return "Email contains invalid characters.";
        }

        String[] parts = email.split("@", -1);

        if (!validation.isFirstPartEmailValid(parts[0])) {
            return "The first part of email is invalid.";
        }

        if (!validation.isSecondPartEmailValid(parts[1])) {
            return "The domain part of email is invalid.";
        }

        return null;
    }

    private String validateSecurity(
            RegisterRequest request,
            SignUpValidation validation
    ) {
        String question = String.valueOf(
                request.getSecurityQuestion()
        );

        if (!validation.isQuestionNumValid(question)) {
            return "Security question must be from 1 to 10.";
        }

        if (!validation.isNotBlank(request.getAnswer())) {
            return "Please enter your security answer.";
        }

        if (!validation.are2answersSame(
                request.getAnswer(),
                request.getAnswerConfirm())) {
            return "Security answers do not match.";
        }

        return null;
    }
}