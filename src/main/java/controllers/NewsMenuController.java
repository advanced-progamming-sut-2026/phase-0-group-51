package controllers;

import Data.database.NewsRepository;
import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.items.News;

import java.util.List;

public class NewsMenuController {
    private final NewsRepository repository = new NewsRepository();
    public Result showUnreadNews() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return new Result(false, "You must log in before viewing news.\n", null
            );
        }
        List<News> newsList = repository.getNewsForUser(user.getId());
        StringBuilder output = new StringBuilder();
        int index = 1;
        for (News news : newsList) {
            if (news.isRead()) {
                continue;
            }
            output.append(index++)
                    .append(". ")
                    .append(news.getMessage())
                    .append('\n');
            repository.markAsRead(
                    user.getId(),
                    news.getId()
            );
        }

        if (index == 1) {
            return new Result(
                    false,
                    "No unread news at the moment.\n",
                    null
            );
        }
        return new Result(true, output.toString(), null);
    }

    public Result showAllNews() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return new Result(
                    false, "You must log in before viewing news.\n", null);
        }
        List<News> newsList = repository.getNewsForUser(user.getId());
        if (newsList.isEmpty()) {
            return new Result(
                    false, "No news at the moment.\n", null
            );
        }
        StringBuilder output = new StringBuilder();
        int index = 1;
        for (News news : newsList) {
            String status = news.isRead()
                    ? "[READ]"
                    : "[UNREAD]";

            output.append(index++)
                    .append(". ")
                    .append(status)
                    .append(' ')
                    .append(news.getMessage())
                    .append('\n');
            if (!news.isRead()) {
                repository.markAsRead(user.getId(), news.getId());
            }
        }
        return new Result(true, output.toString(), null);
    }

    public Result showCurrentMenu() {
        return new Result(true, "You are now in the news menu.\n", null
        );
    }
    public void exitMenu() {
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
    }
}
