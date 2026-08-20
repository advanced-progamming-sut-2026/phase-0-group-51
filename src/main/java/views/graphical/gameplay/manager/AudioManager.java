package views.graphical.gameplay.manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import views.graphical.ui.GameSettings;

import java.util.HashMap;
import java.util.Map;

public class AudioManager {
    private static AudioManager instance;
    private Music currentMusic;
    private final Map<String, Sound> soundCache = new HashMap<>();

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }


    public void playMusic(String filePath) {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
        }
        currentMusic = Gdx.audio.newMusic(Gdx.files.internal(filePath));
        currentMusic.setLooping(true);
        currentMusic.setVolume(GameSettings.music);
        currentMusic.play();
    }

    public void updateMusicVolume() {
        if (currentMusic != null) {
            currentMusic.setVolume(GameSettings.music);
        }
    }


    public void playSfx(String filePath) {
        Sound sound = soundCache.get(filePath);
        if (sound == null) {
            sound = Gdx.audio.newSound(Gdx.files.internal(filePath));
            soundCache.put(filePath, sound);
        }

        sound.play(GameSettings.soundFx);
    }



    public void dispose() {
        if (currentMusic != null) {
            currentMusic.dispose();
        }
        for (Sound sound : soundCache.values()) {
            sound.dispose();
        }
        soundCache.clear();
    }
}
