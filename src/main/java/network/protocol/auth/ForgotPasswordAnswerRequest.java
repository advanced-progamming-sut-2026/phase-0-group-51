package network.protocol.auth;

public class ForgotPasswordAnswerRequest {
    private String answer;

    public ForgotPasswordAnswerRequest() {
    }

    public ForgotPasswordAnswerRequest(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}