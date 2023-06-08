package listeners;

import core.Engine;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;


public class GuildVoiceLeaveEvent extends ListenerAdapter {

    private final Engine engine;

    public GuildVoiceLeaveEvent(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if(event.getChannelLeft() != null && event.getChannelJoined() == null){
            if(!event.getMember().getUser().isBot()) {
                AudioChannel vc = event.getChannelLeft();
                for (Member m:vc.getMembers()) {
                    if(m.getUser().getId().equals(engine.getDiscApplicationEngine().getBotJDA().getSelfUser().getId())){
                        if(vc.getMembers().size() == 1){
                            engine.getCommandHandler().getAudioCommand().stop(event.getMember());
                        }
                    }
                }
            }
        } else if(event.getChannelJoined() != null && event.getChannelLeft() != null){
            if(!event.getMember().getUser().isBot()) {
                AudioChannel vc = event.getChannelLeft();
                for (Member m:vc.getMembers()) {
                    if(m.getUser().getId().equals(engine.getDiscApplicationEngine().getBotJDA().getSelfUser().getId())){
                        if(vc.getMembers().size() == 1){
                            engine.getCommandHandler().getAudioCommand().stop(event.getMember());
                        }
                    }
                }
            }
        }
    }
}
