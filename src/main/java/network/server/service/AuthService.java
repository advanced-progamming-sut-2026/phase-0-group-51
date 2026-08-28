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
import models.enums.SecurityQuestions;
import network.protocol.auth.ForgotPasswordAnswerRequest;
import network.protocol.auth.ForgotPasswordAnswerResponse;
import network.protocol.auth.ForgotPasswordStartRequest;
import network.protocol.auth.ForgotPasswordStartResponse;
import network.protocol.auth.PasswordResetRequest;
import network.protocol.auth.PasswordResetResponse;
import Data.database.AuthSessionRepository;

public class AuthService {
    private final UserRepository userRepository =
            new UserRepository();
    private final AuthSessionRepository
            authSessionRepository =
            new AuthSessionRepository();

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

    public NetworkMessage handleForgotPasswordStart(
            ClientConnection connection,
            NetworkMessage message
    ) {
        try {
            ForgotPasswordStartRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            ForgotPasswordStartRequest.class
                    );

            ForgotPasswordStartResponse response =
                    startPasswordRecovery(
                            connection,
                            request
                    );

            return new NetworkMessage(
                    MessageType.FORGOT_PASSWORD_START_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid password recovery request."
            );
        }
    }
    private ForgotPasswordStartResponse
    startPasswordRecovery(
            ClientConnection connection,
            ForgotPasswordStartRequest request
    ) {
        connection.getSession()
                .clearPasswordRecovery();

        if (request == null
                || request.getUsername() == null
                || request.getUsername().isBlank()) {
            return new ForgotPasswordStartResponse(
                    false,
                    "Please enter your username.",
                    null
            );
        }

        if (request.getEmail() == null
                || request.getEmail().isBlank()) {
            return new ForgotPasswordStartResponse(
                    false,
                    "Please enter your email.",
                    null
            );
        }

        String username =
                request.getUsername().trim();

        String email =
                request.getEmail().trim();

        User user =
                userRepository.getUserByUsername(username);

        if (user == null) {
            return new ForgotPasswordStartResponse(
                    false,
                    "Username does not exist.",
                    null
            );
        }

        if (!user.getEmail().equals(email)) {
            return new ForgotPasswordStartResponse(
                    false,
                    "Email is incorrect.",
                    null
            );
        }

        connection.getSession()
                .beginPasswordRecovery(
                        user.getId(),
                        user.getUsername()
                );

        String question =
                SecurityQuestions.getQuestion(
                        user.getSecurityQuestion()
                );

        return new ForgotPasswordStartResponse(
                true,
                "Please answer your security question.",
                question
        );
    }
    public NetworkMessage handleForgotPasswordAnswer(
            ClientConnection connection,
            NetworkMessage message
    ) {
        try {
            ForgotPasswordAnswerRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            ForgotPasswordAnswerRequest.class
                    );

            ForgotPasswordAnswerResponse response =
                    verifyPasswordRecoveryAnswer(
                            connection,
                            request
                    );

            return new NetworkMessage(
                    MessageType.FORGOT_PASSWORD_ANSWER_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid security answer request."
            );
        }
    }
    private ForgotPasswordAnswerResponse
    verifyPasswordRecoveryAnswer(
            ClientConnection connection,
            ForgotPasswordAnswerRequest request
    ) {
        if (!connection.getSession()
                .hasPasswordRecovery()) {
            return new ForgotPasswordAnswerResponse(
                    false,
                    "No password recovery request is active."
            );
        }

        if (request == null
                || request.getAnswer() == null
                || request.getAnswer().isBlank()) {
            return new ForgotPasswordAnswerResponse(
                    false,
                    "Please enter your security answer."
            );
        }

        User user =
                userRepository.getUserByUsername(
                        connection.getSession()
                                .getRecoveryUsername()
                );

        if (user == null) {
            connection.getSession()
                    .clearPasswordRecovery();

            return new ForgotPasswordAnswerResponse(
                    false,
                    "Account no longer exists."
            );
        }

        if (!user.getAnswer().equalsIgnoreCase(
                request.getAnswer().trim()
        )) {
            return new ForgotPasswordAnswerResponse(
                    false,
                    "Security answer is incorrect."
            );
        }

        connection.getSession()
                .verifyPasswordRecovery();

        return new ForgotPasswordAnswerResponse(
                true,
                "Security answer accepted."
        );
    }
    public NetworkMessage handlePasswordReset(
            ClientConnection connection,
            NetworkMessage message
    ) {
        try {
            PasswordResetRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            PasswordResetRequest.class
                    );

            PasswordResetResponse response =
                    resetPassword(
                            connection,
                            request
                    );

            return new NetworkMessage(
                    MessageType.PASSWORD_RESET_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );
        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid password reset request."
            );
        }
    }
    private PasswordResetResponse resetPassword(
            ClientConnection connection,
            PasswordResetRequest request
    ) {
        if (!connection.getSession()
                .canResetPassword()) {
            return new PasswordResetResponse(
                    false,
                    "Security answer must be verified first."
            );
        }

        if (request == null
                || request.getNewPassword() == null) {
            return new PasswordResetResponse(
                    false,
                    "Please enter your new password."
            );
        }

        String newPassword =
                request.getNewPassword();

        SignUpValidation validation =
                new SignUpValidation();

        if (!validation.isPasswordValid(newPassword)) {
            return new PasswordResetResponse(
                    false,
                    "Password contains invalid characters."
            );
        }

        if (!validation.isPasswordStrong(newPassword)) {
            return new PasswordResetResponse(
                    false,
                    weakPasswordMessage(
                            newPassword,
                            validation
                    )
            );
        }

        String hash =
                HashUtil.hashPassword(newPassword);

        String username =
                connection.getSession()
                        .getRecoveryUsername();

        boolean updated =
                userRepository.updatePassword(
                        username,
                        hash
                );

        if (!updated) {
            return new PasswordResetResponse(
                    false,
                    "Password could not be saved."
            );
        }
        Integer recoveryUserId =
                connection.getSession()
                        .getRecoveryUserId();

        if (recoveryUserId != null) {
            authSessionRepository.deleteAllForUser(
                    recoveryUserId
            );
        }

        connection.getSession()
                .clearPasswordRecovery();

        return new PasswordResetResponse(
                true,
                "Password changed successfully."
        );
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

        String rawToken = null;
        String tokenHash = null;

        if (request.isRememberMe()) {
            rawToken =
                    AuthTokenUtil.generateToken();

            tokenHash =
                    AuthTokenUtil.hashToken(rawToken);

            boolean saved =
                    authSessionRepository.saveToken(
                            user.getId(),
                            tokenHash
                    );

            if (!saved) {
                return loginFailure(
                        "Could not create persistent login session."
                );
            }
        }

        connection.getSession().authenticate(
                user.getId(),
                user.getUsername(),
                tokenHash
        );

        return new LoginResponse(
                true,
                "Login successful.",
                UserProfileDto.fromUser(user),
                rawToken
        );
    }
    public NetworkMessage handleResumeSession(
            ClientConnection connection,
            NetworkMessage message
    ) {
        if (message.getPayload() == null) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Session token is required."
            );
        }

        try {
            ResumeSessionRequest request =
                    codec.decodePayload(
                            message.getPayload(),
                            ResumeSessionRequest.class
                    );

            LoginResponse response =
                    resumeSession(
                            connection,
                            request
                    );

            return new NetworkMessage(
                    MessageType.RESUME_SESSION_RESPONSE,
                    message.getRequestId(),
                    codec.encodePayload(response)
            );

        } catch (JsonProcessingException exception) {
            return NetworkMessage.error(
                    message.getRequestId(),
                    "Invalid session resume request."
            );
        }
    }
    private LoginResponse resumeSession(
            ClientConnection connection,
            ResumeSessionRequest request
    ) {
        if (connection.getSession().isAuthenticated()) {
            return loginFailure(
                    "This connection is already logged in."
            );
        }

        if (request == null
                || request.getToken() == null
                || request.getToken().isBlank()) {
            return loginFailure(
                    "Saved session token is required."
            );
        }

        String tokenHash =
                AuthTokenUtil.hashToken(
                        request.getToken()
                );

        String username =
                authSessionRepository
                        .findUsernameByTokenHash(tokenHash);

        if (username == null) {
            return loginFailure(
                    "Saved login session is invalid."
            );
        }

        User user =
                userRepository.getUserByUsername(username);

        if (user == null) {
            authSessionRepository
                    .deleteToken(tokenHash);

            return loginFailure(
                    "Saved account no longer exists."
            );
        }

        authSessionRepository.touch(tokenHash);

        connection.getSession().authenticate(
                user.getId(),
                user.getUsername(),
                tokenHash
        );

        return new LoginResponse(
                true,
                "Session restored.",
                UserProfileDto.fromUser(user),
                null
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
        String tokenHash =
                connection.getSession()
                        .getPersistentTokenHash();

        if (tokenHash != null) {
            authSessionRepository
                    .deleteToken(tokenHash);
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