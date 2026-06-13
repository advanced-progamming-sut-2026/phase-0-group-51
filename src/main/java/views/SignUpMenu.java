package views;

import java.util.Scanner;

public class SignUpMenu implements AppMenu{
    @Override
    public void check(Scanner scanner) {

    }
    public void handleRegister(String input) {} //register -u <username> -p <password>... جدا سازی دستور
       public void handleUsername (String username){} //چک کردن رجکس valid یوزرنیم - ایمیل...
       public void handlePassword (String pass){ }
       public void handleNickname(String nickname){}
       public void handleEmail(String email){} //بعد بررسی شرط ها تابع کنترلر رو صدا میزنیم
       public void handleGender(String gender){}
    public void handleSecurityQuestion(String input){ }
       public void handleQuestion(String question){}
       public void handleAnswer(String answer){}
}
