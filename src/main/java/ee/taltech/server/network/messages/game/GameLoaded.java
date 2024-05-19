package ee.taltech.server.network.messages.game;

public class GameLoaded {
    private Integer gameID;
    private boolean loaded;

    public GameLoaded(Integer gameID, boolean loaded) {
        this.gameID = gameID;
        this.loaded = loaded;
    }

    public GameLoaded() {}

    public boolean isLoaded() {
        return loaded;
    }

    public Integer getGameID() {
        return gameID;
    }
}
