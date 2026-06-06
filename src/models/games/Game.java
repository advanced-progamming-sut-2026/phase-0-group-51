package models.games;
import models.Mower;

import java.util.ArrayList;
import java.util.List;
public class Game {
    private Chapter chapter;
    public List<Mower> mowers = new ArrayList<>(); //لیست چمن زنی های فعال هر بازی (کدوماش مونده کدوما رفتن)
    public void addTick(int tick){}
    public int sunAmount;


}
