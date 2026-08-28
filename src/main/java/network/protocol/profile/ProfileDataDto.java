package network.protocol.profile;

import models.User;

public class ProfileDataDto {
    private String username;
    private String nickname;
    private String email;
    private int gamesPlayed;
    private int coins;
    private int gems;
    private int passedLevels;
    private int mostMeowPoint;

    public ProfileDataDto() {
    }

    public ProfileDataDto(
            String username,
            String nickname,
            String email,
            int gamesPlayed,
            int coins,
            int gems,
            int passedLevels,
            int mostMeowPoint
    ) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.gamesPlayed = gamesPlayed;
        this.coins = coins;
        this.gems = gems;
        this.passedLevels = passedLevels;
        this.mostMeowPoint = mostMeowPoint;
    }

    public static ProfileDataDto fromUser(
            User user,
            int passedLevels
    ) {
        return new ProfileDataDto(
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getGamesPlayed(),
                user.getCoins(),
                user.getGems(),
                passedLevels,
                user.getMostMeowPoint()
        );
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = gems;
    }

    public int getPassedLevels() {
        return passedLevels;
    }

    public void setPassedLevels(int passedLevels) {
        this.passedLevels = passedLevels;
    }

    public int getMostMeowPoint() {
        return mostMeowPoint;
    }

    public void setMostMeowPoint(int mostMeowPoint) {
        this.mostMeowPoint = mostMeowPoint;
    }
}
