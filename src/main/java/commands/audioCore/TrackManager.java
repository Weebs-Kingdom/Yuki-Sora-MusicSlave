package commands.audioCore;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.TrackMarkerHandler;
import core.Engine;
import core.UtilityBase;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackManager extends AudioEventAdapter {

    private final AudioPlayer PLAYER;
    private final Queue<AudioInfo> queue;
    private boolean repeatSong = false;
    private Engine engine;
    private VoiceChannel vc;
    private Timer updateTimer;

    public TrackManager(AudioPlayer player, VoiceChannel vc, Engine engine) {
        this.engine = engine;
        this.vc = vc;
        this.PLAYER = player;
        this.queue = new LinkedBlockingQueue<>();

        updateTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(!queue.isEmpty())
                    if(queue.element() != null){
                        String info = "Title: " + queue.element().getTrack().getInfo().title + "\n" +
                                "Duration: " + "`[ " + UtilityBase.getTimestamp(queue.element().getTrack().getPosition()) + "/ " + UtilityBase.getTimestamp(queue.element().getTrack().getDuration()) + " ]`" + "\n" +
                                "Author: " + queue.element().getTrack().getInfo().author + "\" }";
                        engine.getDiscApplicationEngine().getBotJDA().getPresence().setActivity(Activity.listening(info));
                    }
                engine.getDiscApplicationEngine().getBotJDA().getPresence().setActivity(null);
            }
        };
        updateTimer.schedule(task, 1000);
    }

    public void repeatSong() {
        this.repeatSong = true;
    }

    public void stopRepeat() {
        this.repeatSong = false;
    }

    public void queue(AudioTrack track, Member author) {
        AudioInfo info = new AudioInfo(track, author);
        queue.add(info);

        if (PLAYER.getPlayingTrack() == null) {
            PLAYER.playTrack(track);
        }
    }

    public Set<AudioInfo> getQueue() {
        return new LinkedHashSet<>(queue);
    }

    public AudioInfo getInfo(AudioTrack track) {
        return queue.stream()
                .filter(info -> info.getTrack().equals(track))
                .findFirst().orElse(null);
    }

    public void purgeQueue() {
        queue.clear();
    }

    public void shuffleQueue() {
        List<AudioInfo> cQueue = new ArrayList<>(getQueue());
        AudioInfo current = cQueue.get(0);
        cQueue.remove(0);
        Collections.shuffle(cQueue);
        cQueue.add(0, current);
        purgeQueue();
        queue.addAll(cQueue);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        AudioInfo info = queue.element();
        VoiceChannel vChan = info.getAuthor().getVoiceState().getChannel();

        if (vChan == null) {
            player.stopTrack();
        } else {
            info.getAuthor().getGuild().getAudioManager().openAudioConnection(vChan);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        stopAudioConnection();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        stopAudioConnection();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs, StackTraceElement[] stackTrace) {
        stopAudioConnection();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason != AudioTrackEndReason.LOAD_FAILED) {
            AudioInfo info;
            try {
                info = queue.element();
                if(info == null){
                    stopAudioConnection();
                    return;
                }
            } catch (Exception e){
                stopAudioConnection();
                return;
            }

            if (!repeatSong) {
                queue.poll();
            }

            if (queue.isEmpty()) {
                stopAudioConnection();
            } else {
                try {
                    player.startTrack(queue.element().getTrack().makeClone(), false);
                } catch (Exception e) {
                    stopAudioConnection();
                }
            }
        } else {
            vc.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    private void stopAudioConnection(){
        System.out.println(engine);
        System.out.println(engine.getDiscApplicationEngine());
        System.out.println(engine.getDiscApplicationEngine().getBotJDA());
        System.out.println(engine.getDiscApplicationEngine().getBotJDA().getPresence());
        System.out.println(engine.getDiscApplicationEngine().getBotJDA().getPresence().getActivity());
        updateTimer.cancel();
        engine.getDiscApplicationEngine().getBotJDA().getPresence().setActivity(null);
        vc.getGuild().getAudioManager().closeAudioConnection();
    }

    public boolean isRepeatSong() {
        return repeatSong;
    }
}
