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

   public Result showUnreadNews(){
       User user = App.getInstance().getLoggedInUser();
       List<News> newsList =  repository.getNewsForUser(user.getId());
       boolean found = false;
       StringBuilder sb = new StringBuilder();
       int index =1;
       for(News news : newsList){
           if(!news.isRead()){
               found = true;
               sb.append(index++).append(". ").append(news.getMessage()).append("\n");
               repository.markAsRead(user.getId(), news.getId());
           }
       }
       if(!found){
           return new Result(false, "No unread news at the moment.\n", null);
       }

       return new Result(true,sb.toString(),null);
   }
  public Result showAllNews(){
       User user = App.getInstance().getLoggedInUser();
      List<News> newsList = repository.getNewsForUser(user.getId());
      if(newsList.isEmpty()) {
          return new Result(false, "No news at the moment.\n", null);
      }
      StringBuilder sb = new StringBuilder();
      int index = 1;
      for (News news : newsList) {
          sb.append(index++).append(". ").append(news.getMessage()).append("\n");
      }

      return new Result(true,sb.toString(),null);
  }
    public Result showCurrentMenu(){
        return new Result(true,"You are now in the news menu.\n",null);
    }
    public void exitMenu(){
        App.getInstance().setCurrentMenu(Menu.MainMenu);
    }
}
