package commands.audioCore;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import core.Engine;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.apache.http.client.config.RequestConfig;

import java.util.*;
import java.util.stream.Collectors;

public class AudioCommand {

    private final int PLAYLIST_LIMIT = 1000;
    private final AudioPlayerManager MANAGER = new DefaultAudioPlayerManager();
    private final Map<Guild, Map.Entry<AudioPlayer, TrackManager>> PLAYERS = new HashMap<>();
    int added = 0;

    Engine engine;

    public AudioCommand() {
        AudioSourceManagers.registerRemoteSources(MANAGER);
        MANAGER.setHttpRequestConfigurator(config ->
                RequestConfig.copy(config)
                        .setSocketTimeout(10000)
                        .setConnectTimeout(10000)
                        .build()
        );
    }

    private AudioPlayer createPlayer(Member author) {
        //engine.getDiscEngine().getBotJDA().getGuildById(author.getGuild().getId());
        Guild g = author.getGuild();
        AudioPlayer p = MANAGER.createPlayer();
        TrackManager m = new TrackManager(p);
        p.addListener(m);

        g.getAudioManager().setSendingHandler(new PlayerSendHandler(p));

        PLAYERS.put(g, new AbstractMap.SimpleEntry<>(p, m));

        return p;
    }

    private boolean hasPlayer(Member author) {
        Guild g = author.getGuild();
        return PLAYERS.containsKey(g);
    }

    private AudioPlayer getPlayer(Member author) {
        Guild g = author.getGuild();
        if (hasPlayer(author)) {
            return PLAYERS.get(g).getKey();
        } else {
            return createPlayer(author);
        }
    }

    private TrackManager getManager(Member author) {
        Guild g = author.getGuild();
        return PLAYERS.get(g).getValue();
    }

    private boolean isIdle(Member author) {
        return !hasPlayer(author) || getPlayer(author).getPlayingTrack() == null;
    }

    /**
     * Läd aus der URL oder dem Search String einen Track oder eine Playlist in
     * die Queue.
     *
     * @param identifier URL oder Search String
     * @param author     Member, der den Track / die Playlist eingereiht hat
     */
    private void loadTrack(String identifier, Member author) {

        Guild g = author.getGuild();
        getPlayer(author);
        boolean singleSong = false;

        MANAGER.setFrameBufferDuration(5000);
        if (identifier.startsWith("ytsearch:"))
            singleSong = true;
        final boolean finalSingleSong = singleSong;
        MANAGER.loadItemOrdered(g, identifier, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                getManager(author).queue(track, author);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (finalSingleSong) {
                    getManager(author).queue(playlist.getTracks().get(0), author);
                    return;
                }
                for (int i = 0; i < (playlist.getTracks().size() > PLAYLIST_LIMIT ? PLAYLIST_LIMIT : playlist.getTracks().size()); i++) {
                    added++;
                    getManager(author).queue(playlist.getTracks().get(i), author);
                }
            }

            @Override
            public void noMatches() {
            }

            @Override
            public void loadFailed(FriendlyException exception) {
            }
        });
    }

    private void skip(Member author) {
        getPlayer(author).stopTrack();
    }

    /**
     * Erzeugt aus dem Timestamp in Millisekunden ein hh:mm:ss - Zeitformat.
     *
     * @param milis Timestamp
     * @return Zeitformat
     */
    private String getTimestamp(long milis) {
        long seconds = milis / 1000;
        long hours = Math.floorDiv(seconds, 3600);
        seconds = seconds - (hours * 3600);
        long mins = Math.floorDiv(seconds, 60);
        seconds = seconds - (mins * 60);
        return (hours == 0 ? "" : hours + ":") + String.format("%02d", mins) + ":" + String.format("%02d", seconds);
    }

    /**
     * Returnt aus der AudioInfo eines Tracks die Informationen als String.
     *
     * @param info AudioInfo
     * @return Informationen als String
     */
    private String buildQueueMessage(AudioInfo info) {
        AudioTrackInfo trackInfo = info.getTrack().getInfo();
        String title = trackInfo.title;
        long length = trackInfo.length;
        return "`[ " + getTimestamp(length) + " ]` " + title + "\n";
    }

    public String play(String[] args, Member m) {
        String input;
        if (isIdle(m)) {

        } else {
            getManager(m).purgeQueue();
            skip(m);
        }

        input = Arrays.stream(args).skip(1).map(s -> " " + s).collect(Collectors.joining()).substring(1);
        if (args.length < 2) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: Song not found\"}";
        }
        if (input.startsWith("all")) {
            input = Arrays.stream(args).skip(2).map(s -> " " + s).collect(Collectors.joining()).substring(1);
        }
        if (!(input.startsWith("http://") || input.startsWith("https://"))) {
            input = "ytsearch: " + input;
        }
        loadTrack(input, m);
        return "{ \"status\" : \"200\", \"response\" : \":arrow_forward: Song is now playing\"}";
    }

    public String repeat(Member m){
        if(getManager(m).getRepeatingSong() == null){
            getManager(m).repeatSong(getPlayer(m).getPlayingTrack());
            return "{ \"status\" : \"200\", \"response\" : \":repeat: Song is now on repeat\"}";
        } else {
            getManager(m).repeatSong(null);
            return "{ \"status\" : \"200\", \"response\" : \":no_entry_sign: Repeating stopped\"}";
        }
    }

    public String add(String[] args, Member m) {
        String input = Arrays.stream(args).skip(1).map(s -> " " + s).collect(Collectors.joining()).substring(1);
        if (args.length < 2) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: Song not found\"}";
        }
        if (!(input.startsWith("http://") || input.startsWith("https://"))) {
            input = "ytsearch: " + input;
        }
        if (input.startsWith("all")) {
            input = Arrays.stream(args).skip(2).map(s -> " " + s).collect(Collectors.joining()).substring(1);
        }
        loadTrack(input, m);
        return "{ \"status\" : \"200\", \"response\" : \":musical_note: Song added\"}";
    }

    public String skip(String[] args, Member m) {
        if (isIdle(m)) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: There is no queue\"}";
        }
        for (int i = (args.length > 1 ? Integer.parseInt(args[1]) : 1); i == 1; i--) {
            skip(m);
        }
        return "{ \"status\" : \"200\", \"response\" : \":track_next: Skipped\"}";
    }

    public String stop(Member m) {
        if (isIdle(m)) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: No queue found\"}";
        }

        getManager(m).purgeQueue();
        skip(m);
        Guild g = m.getGuild();
        g.getAudioManager().closeAudioConnection();
        return "{ \"status\" : \"200\", \"response\" : \":stop_button: Song stopped\"}";
    }

    public String shuffle(Member m) {
        if (isIdle(m)) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: No list found\"}";
        }
        getManager(m).shuffleQueue();
        return "{ \"status\" : \"200\", \"response\" : \":twisted_rightwards_arrows: Queue shuffled\"}";
    }

    public String info(Member m) {
        if (isIdle(m)) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: No list found\"}";
        }

        AudioTrack track = getPlayer(m).getPlayingTrack();
        AudioTrackInfo info = track.getInfo();

        return "{ \"status\" : \"200\", \"response\" : \"** :musical_note: CURRENT TRACK INFO:** :information_source:\n" +
                "Title: " + info.title + "\n" +
                "Duration: " + "`[ " + getTimestamp(track.getPosition()) + "/ " + getTimestamp(track.getDuration()) + " ]`" + "\n" +
                "Author: " + info.author + "\" }";
    }

    public String showQueue(String[] args, Member m) {
        if (isIdle(m)) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: No list found\"}";
        }

        int sideNumb = args.length > 1 ? Integer.parseInt(args[1]) : 1;

        List<String> tracks = new ArrayList<>();
        List<String> trackSublist;

        getManager(m).getQueue().forEach(audioInfo -> tracks.add(buildQueueMessage(audioInfo)));

        if (tracks.size() > 20) {
            trackSublist = tracks.subList((sideNumb - 1) * 20, (sideNumb - 1) * 20 + 20);
        } else {
            trackSublist = tracks;
        }

        String out = trackSublist.stream().collect(Collectors.joining("\n"));
        int sideNumbAll = tracks.size() >= 20 ? tracks.size() / 20 : 1;
        return "{ \"status\" : \"200\", \"response\" : \"**CURRENT QUEUE:** :information_source: \n"
                + "*[" + " Tracks | Side " + sideNumb + " / " + sideNumbAll + "]*\n\n"
                + out + "\" }";
    }
}