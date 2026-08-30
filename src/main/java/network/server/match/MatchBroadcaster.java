package network.server.match;

import models.minigames.iZombie.multiplayer.MatchRole;

public interface MatchBroadcaster {

    void toRole(MatchRole role, Object message);

    void toBoth(Object message);
}
