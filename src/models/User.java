package models;

import models.games.Level;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String username;
    private String email;
    private String passwordHash;
    private String gender;
    private String nickname;
    private String securityQuestion;
    private String answer;
    private int mostMeowPoint;
    private int gamesPlayed;
    //private Map<PlantType, >;
    private int coins;
    private int gems;
    private Map<Level, Boolean> levels = new HashMap<>();
    private String lastWonGame; //مثلا مرحله 1 فصل 3
    private int miniGamesPlayed;
    private int maxPoint;
    private int questDailyNum;
    private int questNonDailyNum;
    private int seedPacket;
    private int plantFoodNum;
    private int sunAmount;

    public int getSunAmount() {
        return sunAmount;
    }
}

