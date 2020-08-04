package core;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;

import javax.security.auth.login.LoginException;

public class DiscApplicationEngine {

    private final Engine engine;

    private final String consMsgDef = "[Discord application]";
    private boolean isRunning = false;

    private JDABuilder builder;
    private JDA botJDA;

    public DiscApplicationEngine(Engine engine) {
        this.engine = engine;
    }

    public void startBotApplication(){
        if(isRunning){
            engine.getUtilityBase().printOutput(consMsgDef + " !!!Bot start failure - bot is already running!!!", false);
            return;
        }
        if(engine.getProperties().discBotApplicationToken == null){
            if(engine.getProperties().discBotApplicationToken.equalsIgnoreCase("")){
                engine.getUtilityBase().printOutput(consMsgDef + " !!!Bot start failure - token invalid!!!", false);
                return;
            }
        }
        engine.getUtilityBase().printOutput(consMsgDef + " !Bot start initialized!", false);
        isRunning = true;

        builder = new JDABuilder(AccountType.BOT);
        builder.setToken(engine.getProperties().discBotApplicationToken);
        builder.setAutoReconnect(true);
        builder.setStatus(OnlineStatus.ONLINE);
        setBotApplicationGame(null, Game.GameType.DEFAULT);
        try {
            botJDA = builder.build();
        } catch (LoginException e) {
            if(engine.getProperties().debug){e.printStackTrace();}
            engine.getUtilityBase().printOutput(consMsgDef + " !!!Bot start failure - maybe token invalid!!!", false);
            isRunning = false;
            return;
        }
        engine.getUtilityBase().printOutput(consMsgDef + " !Bot successfully started!", false);
    }

    private void setBotApplicationGame(String game, Game.GameType type) {
        builder.setGame(new Game("") {
            @Override
            public String getName() {
                if (game != null) {
                    return game;
                } else {
                    return engine.getProperties().discBotApplicationGame;
                }
            }

            @Override
            public String getUrl() {
                return null;
            }

            @Override
            public GameType getType() {
                return type;
            }
        });
    }

    public void shutdownBotApplication() {
        if(!isRunning){
            engine.getUtilityBase().printOutput(consMsgDef + " ~The bot is already offline!", false);
            return;
        }
        engine.getUtilityBase().printOutput(consMsgDef + " ~Bot shutting down!",false);
        try {
            botJDA.shutdownNow();
        } catch (Exception e) {
            engine.getUtilityBase().printOutput(consMsgDef + " ~Bot cant shutdownBotApplication, eventually never starts?", false);
        }
        isRunning = false;
    }

    public JDA getBotJDA() {
        return botJDA;
    }
}
