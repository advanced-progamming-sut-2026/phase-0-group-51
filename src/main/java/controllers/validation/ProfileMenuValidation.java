package controllers.validation;

public class ProfileMenuValidation {
    public boolean areUsernamesTheSame(String oldUsername,String newUsername){
        return oldUsername.equals(newUsername);
    }
    public boolean areNicknamesTheSame(String oldNickname,String newNickname){
        return oldNickname.equals(newNickname);
    }
    public boolean areEmailsTheSame(String oldEmail,String newEmail){
        return oldEmail.equals(newEmail);
    }
    public boolean arePasswordsTheSame(String oldPass, String newPass){
        return oldPass.equals(newPass);
    }
}
