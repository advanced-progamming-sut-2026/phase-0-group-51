package controllers;

import models.App;
import models.Result;
import models.User;
import models.enums.Menu;
import models.items.News;
import network.client.ClientNewsState;
import network.protocol.news.NewsItemDto;

import java.util.ArrayList;
import java.util.List;

public class NewsMenuController {
    public Result showUnreadNews() {
        User user = App.getInstance().getLoggedInUser();
        if (user == null) {
            return new Result(
                    false,
                    "You must log in before viewing news.\n",
                    null
            );
        }

        if (!ClientNewsState.isLoaded()) {
            return new Result(
                    false,
                    "News has not been loaded from the server yet.\n",
                    null
            );
        }

        StringBuilder output = new StringBuilder();
        int index = 1;
        for (NewsItemDto item : ClientNewsState.news()) {
            if (item.isRead()) {
                continue;
            }
            output.append(index++)
                    .append(". ")
                    .append(item.getMessage())
                    .append('\n');
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
                    false,
                    "You must log in before viewing news.\n",
                    null
            );
        }

        if (!ClientNewsState.isLoaded()) {
            return new Result(
                    false,
                    "News has not been loaded from the server yet.\n",
                    null
            );
        }

        List<NewsItemDto> newsList = ClientNewsState.news();
        if (newsList.isEmpty()) {
            return new Result(
                    false,
                    "No news at the moment.\n",
                    null
            );
        }

        StringBuilder output = new StringBuilder();
        int index = 1;
        for (NewsItemDto item : newsList) {
            String status = item.isRead()
                    ? "[READ]"
                    : "[UNREAD]";

            output.append(index++)
                    .append(". ")
                    .append(status)
                    .append(' ')
                    .append(item.getMessage())
                    .append('\n');
        }

        return new Result(true, output.toString(), null);
    }

    public Result showCurrentMenu() {
        return new Result(
                true,
                "You are now in the news menu.\n",
                null
        );
    }

    public void exitMenu() {
        App.getInstance().setCurrentMenu(Menu.MAIN_MENU);
    }

    public int getUnreadNewsCount() {
        return ClientNewsState.isLoaded()
                ? ClientNewsState.unreadCount()
                : 0;
    }

    public List<News> openAllNews() {
        if (!ClientNewsState.isLoaded()) {
            return List.of();
        }

        List<News> result = new ArrayList<>();
        for (NewsItemDto item : ClientNewsState.news()) {
            result.add(
                    new News(
                            item.getId(),
                            0,
                            item.getMessage(),
                            item.getCreatedAt(),
                            item.isRead()
                    )
            );
        }
        return result;
    }
}
