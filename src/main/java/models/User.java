package models;

import lombok.Getter;
import lombok.Setter;
import models.games.Level;

import java.util.HashMap;
import java.util.Map;
@Getter
@Setter
public class User {
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String gender;
    private String nickname;
    private String securityQuestion;
    private String answer;
    private int mostMeowPoint;
    private int gamesPlayed;
    private int coins;
    private int gems;
    private Map<Level, Boolean> levels = new HashMap<>();
    private String lastWonGame;
    private int miniGamesPlayed;
    private int maxPoint;
    private int questDailyNum;
    private int questNonDailyNum;
    private int seedPacket;
    private int plantFoodNum;
    //  کانستراکتور (برای موقعی که یوزری که قبلا ثبت نام کرده رو از دیتابیس می‌خونیم)
    public User(int id, String username, String email, String passwordHash, String gender,
                String nickname, String securityQuestion, String answer, int coins, int gems,
                int seedPacket, int plantFoodNum, int mostMeowPoint, int maxPoint,
                int gamesPlayed, int miniGamesPlayed, String lastWonGame) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.gender = gender;
        this.nickname = nickname;
        this.securityQuestion = securityQuestion;
        this.answer = answer;
        this.coins = coins;
        this.gems = gems;
        this.seedPacket = seedPacket;
        this.plantFoodNum = plantFoodNum;
        this.mostMeowPoint = mostMeowPoint;
        this.maxPoint = maxPoint;
        this.gamesPlayed = gamesPlayed;
        this.miniGamesPlayed = miniGamesPlayed;
        this.lastWonGame = lastWonGame;
    }

    // کانستراکتور برای ساخت یوزر جدید
    public User(String username, String email, String passwordHash, String gender,
                String nickname, String securityQuestion, String answer) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.gender = gender;
        this.nickname = nickname;
        this.securityQuestion = securityQuestion;
        this.answer = answer;
        //بقیه فیلد ها توی دیتابیس خود به خود 0 تنظیم می‌شن
    }

}

