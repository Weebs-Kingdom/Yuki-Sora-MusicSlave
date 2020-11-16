package listeners;

import core.Engine;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public class GuildVoiceLeaveEvent extends ListenerAdapter {

    private final Engine engine;

    public GuildVoiceLeaveEvent(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void onGuildVoiceLeave(net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent event) {
        if(!event.getMember().getUser().isBot()) {
            VoiceChannel vc = event.getChannelLeft();
            for (Member m:vc.getMembers()) {
                if(m.getUser().getId().equals(engine.getDiscApplicationEngine().getBotJDA().getSelfUser().getId())){
                    if(vc.getMembers().size() == 1){
                        engine.getCommandHandler().getAudioCommand().stop(event.getMember());
                    }
                }
            }
        }
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if(!event.getMember().getUser().isBot()) {
            VoiceChannel vc = event.getChannelLeft();
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
