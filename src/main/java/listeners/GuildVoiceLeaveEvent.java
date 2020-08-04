package listeners;

import core.Engine;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class GuildVoiceLeaveEvent extends ListenerAdapter {

    private Engine engine;

    public GuildVoiceLeaveEvent(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void onGuildVoiceLeave(net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent event) {
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
