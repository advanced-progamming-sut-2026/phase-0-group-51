package models;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class Result {
    private boolean success;
    private String message;
    private Object object ;
    public Result(boolean success, String message,Object object) {
        this.success = success;
        this.message = message;
        this.object=object;
    }

    public boolean success() {
        return success;
    }
    public String message() {
        return message;
    }
}
