package models;

import lombok.Getter;
import lombok.Setter;
import models.greenHouse.GreenHouse;
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
    private int securityQuestion;
    private String answer;
    private int mostMeowPoint;
    private int gamesPlayed;
    private int coins;
    private int gems;
    private Map<Level, Boolean> levels = new HashMap<>();
    private String lastWonGame;
    private int miniGamesPlayed;
    private int difficultyLevel;
    private int maxPoint;
    private int questDailyNum;
    private int questNonDailyNum;
    private int plantFoodNum;
    private GreenHouse greenHouse;

    public User(int id, String username, String email, String passwordHash, String gender,
                String nickname, int securityQuestion, String answer, int coins, int gems, int plantFoodNum,
                int mostMeowPoint, int maxPoint,
                int gamesPlayed, int miniGamesPlayed, String lastWonGame, int difficultyLevel) {
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
        this.plantFoodNum = plantFoodNum;
        this.mostMeowPoint = mostMeowPoint;
        this.maxPoint = maxPoint;
        this.gamesPlayed = gamesPlayed;
        this.miniGamesPlayed = miniGamesPlayed;
        this.lastWonGame = lastWonGame;
        this.difficultyLevel=difficultyLevel;
    }


    public User(String username, String email, String passwordHash, String gender,
                String nickname, int securityQuestion, String answer) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.gender = gender;
        this.nickname = nickname;
        this.securityQuestion = securityQuestion;
        this.answer = answer;
        this.difficultyLevel = 3;
    }


}

