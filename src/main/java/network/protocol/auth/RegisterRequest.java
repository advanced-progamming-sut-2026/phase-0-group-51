package network.protocol.auth;

public class RegisterRequest {
    private String username;
    private String password;
    private String passwordConfirm;
    private String nickname;
    private String email;
    private String gender;
    private int securityQuestion;
    private String answer;
    private String answerConfirm;

    public RegisterRequest() {
    }

    public RegisterRequest(
            String username,
            String password,
            String passwordConfirm,
            String nickname,
            String email,
            String gender,
            int securityQuestion,
            String answer,
            String answerConfirm
    ) {
        this.username = username;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.securityQuestion = securityQuestion;
        this.answer = answer;
        this.answerConfirm = answerConfirm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(int securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAnswerConfirm() {
        return answerConfirm;
    }

    public void setAnswerConfirm(String answerConfirm) {
        this.answerConfirm = answerConfirm;
    }
}