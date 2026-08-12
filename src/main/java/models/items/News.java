package models.items;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class News {
private int id;
private int userId;
private String message;
private String createdAt;
private boolean isRead;
}
