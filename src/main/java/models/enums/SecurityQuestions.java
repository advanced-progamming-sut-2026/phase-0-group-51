package models.enums;

import lombok.Getter;
@Getter
public enum SecurityQuestions {
    Q1(1,"Once and for all: tea or coffee?"),
    Q2(2,"Be honest : your first-Semester rank?"),
    Q3(3,"The name of your celebrity crush?"),
    Q4(4,"What's your biggest fear in life?"),
    Q5(5,"The name of your university crush?"),
    Q6(6,"What's your biggest red flag?"),
    Q7(7,"The name of the last person you've blocked?"),
    Q8(8,"Honestly : your Riazi_1 grade?"),
    Q9(9,"But seriously,does true love exist? (Yes/No)"),
    Q10(10,"All in all, Mrhessabi or Banoo_Laale?");


    private final int num;
    private final String question;
    SecurityQuestions(int num,String question){
        this.num=num;
        this.question=question;
    }
    public static String listOfSecurityQuestions(){
        StringBuilder sb = new StringBuilder();
        for(SecurityQuestions sq: SecurityQuestions.values()){
            sb.append(sq.num).append(".").append(sq.question).append("\n");
        }
        return sb.toString();
    }

    public static String getQuestion(int num) {
        for (SecurityQuestions sq : SecurityQuestions.values()) {
            if (sq.getNum() == num) {
                return sq.getQuestion();
            }
        }
        return "";
    }

}
