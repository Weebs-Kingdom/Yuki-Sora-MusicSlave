package core;

import java.io.Serializable;

public class Properties implements Serializable {
    public static final long serialVersionUID = 42L;
    //Discord BotApplication stuff
    public String discBotApplicationToken = "";
    public String discBotApplicationGame = "I like trains";
    public String discBotApplicationPrefix = "-";
    //telegram BotApplication stuff
    //Engine stuff
    public boolean debug = false;
    public boolean showTime = true;
    public String spotifyClientId;
    public String spotifyClientSecret;

    public int apiPort = 0;
}
