package core;

import com.pengrad.telegrambot.model.Game;
import listeners.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;

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

        builder = JDABuilder.createDefault(engine.getProperties().discBotApplicationToken);
        builder.setAutoReconnect(true);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.addEventListeners(new GuildVoiceLeaveEvent(engine));
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
