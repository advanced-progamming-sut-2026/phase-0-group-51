package views;

import java.util.Scanner;

public class ProfileMenu implements AppMenu{
    @Override
    public void check(Scanner scanner) {
        String line = scanner.nextLine().trim();
     //به جز 4 تابع زیر , تابع profileShowInfo کنترلر صدا زده شود
    }
    public void handleChangeUsername(String input){}
    public void handleChangeNickname(String input){}
    public void handleChangeEmail(String input){}
    public void handleChangePassword(String password){}
}
