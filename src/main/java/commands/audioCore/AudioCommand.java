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
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;
import core.Engine;
import core.UtilityBase;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.apache.hc.core5.http.ParseException;
import org.apache.http.client.config.RequestConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AudioCommand {

    private final int PLAYLIST_LIMIT = 1000;
    private final AudioPlayerManager MANAGER = new DefaultAudioPlayerManager();
    private final Map<Guild, Map.Entry<AudioPlayer, TrackManager>> PLAYERS = new HashMap<>();
    private int added = 0;
    private SpotifyApi spotifyApi;
    private ClientCredentialsRequest clientCredentialsRequest;

    private Engine engine;

    public AudioCommand(Engine engine) {
        this.engine = engine;

        spotifyApi = new SpotifyApi.Builder().setRedirectUri(SpotifyHttpManager.makeUri("https://weebskingdom.com")).setClientId(engine.getProperties().spotifyClientId).setClientSecret(engine.getProperties().spotifyClientSecret).build();
        clientCredentialsRequest = spotifyApi.clientCredentials()
                .build();

        AudioSourceManagers.registerRemoteSources(MANAGER);
        MANAGER.setHttpRequestConfigurator(config ->
                RequestConfig.copy(config)
                        .setSocketTimeout(10000)
                        .setConnectTimeout(10000)
                        .build()
        );
        Timer t = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                authorizationCodeUri_Async();
            }
        };

        t.schedule(tt, 100, 1*60*1000);
    }

    public void authorizationCodeUri_Async() {
        try {
            final CompletableFuture<ClientCredentials> clientCredentialsFuture = clientCredentialsRequest.executeAsync();

            // Thread free to do other tasks...

            // Example Only. Never block in production code.
            final ClientCredentials clientCredentials = clientCredentialsFuture.join();

            // Set access token for further "spotifyApi" object usage
            spotifyApi.setAccessToken(clientCredentials.getAccessToken());

            System.out.println("Expires in: " + clientCredentials.getExpiresIn());
        } catch (CompletionException e) {
            System.out.println("Error: " + e.getCause().getMessage());
        } catch (CancellationException e) {
            System.out.println("Async operation cancelled.");
        }
    }

    private AudioPlayer createPlayer(Member author) {
        Guild g = author.getGuild();
        AudioPlayer p = MANAGER.createPlayer();
        TrackManager m = new TrackManager(p, author.getVoiceState().getChannel(), engine);
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

        MANAGER.setFrameBufferDuration(10000);
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
     * Returnt aus der AudioInfo eines Tracks die Informationen als String.
     *
     * @param info AudioInfo
     * @return Informationen als String
     */
    private String buildQueueMessage(AudioInfo info) {
        AudioTrackInfo trackInfo = info.getTrack().getInfo();
        String title = trackInfo.title;
        long length = trackInfo.length;
        return "`[ " + UtilityBase.getTimestamp(length) + " ]` " + title + "\n";
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


        if(input.contains("open.spotify.com/") || input.contains("spotify:playlist:") || input.contains("spotify:track:")){
            String[] spotifyInfo = getYoutubeSearchBySpotify(input);
            if(spotifyInfo == null){
                return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: Song on spotify not found\"}";
            }
            for(String s: spotifyInfo){
                loadTrack("ytsearch: " + s, m);
            }

            return "{ \"status\" : \"200\", \"response\" : \":arrow_forward: Song is now playing\"}";
        }

        if (!(input.startsWith("http://") || input.startsWith("https://")))
            input = "ytsearch: " + input;

        loadTrack(input, m);
        return "{ \"status\" : \"200\", \"response\" : \":arrow_forward: Song is now playing\"}";
    }

    public String repeat(Member m) {
        if (isIdle(m)) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: There is no queue\"}";
        }
        if (!getManager(m).isRepeatSong()) {
            getManager(m).repeatSong();
            return "{ \"status\" : \"200\", \"response\" : \":repeat: Song is now on repeat\"}";
        } else {
            getManager(m).stopRepeat();
            return "{ \"status\" : \"200\", \"response\" : \":no_entry_sign: Repeating stopped\"}";
        }
    }

    public String add(String[] args, Member m) {
        String input = Arrays.stream(args).skip(1).map(s -> " " + s).collect(Collectors.joining()).substring(1);
        if (args.length < 2) {
            return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: Song not found\"}";
        }

        if(input.contains("open.spotify.com/") || input.contains("spotify:playlist:") || input.contains("spotify:track:")){
            String[] spotifyInfo = getYoutubeSearchBySpotify(input);
            if(spotifyInfo == null){
                return "{ \"status\" : \"400\", \"response\" : \":no_entry_sign: Song on spotify not found\"}";
            }
            for(String s: spotifyInfo){
                loadTrack("ytsearch: " + s, m);
            }

            return "{ \"status\" : \"200\", \"response\" : \":arrow_forward: Song is now playing\"}";
        }

        if (!(input.startsWith("http://") || input.startsWith("https://")))
            input = "ytsearch: " + input;

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
                "Duration: " + "`[ " + UtilityBase.getTimestamp(track.getPosition()) + "/ " + UtilityBase.getTimestamp(track.getDuration()) + " ]`" + "\n" +
                "Author: " + info.author + "\n" +
                "URL: " + info.uri + "\" }";
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

        long milis = getManager(m).getQueue().stream().mapToLong(info -> info.getTrack().getDuration()).sum();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Instant t = Instant.ofEpochMilli(milis);

        String timeString = formatter.format(t);


        String data = ":information_source: **CURRENT QUEUE** :information_source: \n" +
                "*Total playlist duration: " + timeString + "*\n\n"
                + "*[" + " Tracks | Side " + sideNumb + " / " + sideNumbAll + "]*\n" +
                "\n\n"
                + out;

        data = data.replace("\"", "\\\"");

        return "{ \"status\" : \"200\", \"response\" : \"" + data +"\" }";
    }

    private String[] getYoutubeSearchBySpotify(String url){
        if(url.contains("/track/")){
            String[] args = url.split("/track/");
            String id = args[1].split("\\?si=")[0];
            try {
                return new String[]{getTrackInfo(id)};
            } catch (Exception e) {
                return null;
            }
        } else if (url.contains("/playlist/")){
            String[] args = url.split("/playlist/");
            String id = args[1].split("\\?si=")[0];
            return loadPlaylist(id);
        } else if(url.contains("spotify:track:")){
            String id = url.substring(14);
            try {
                return new String[]{getTrackInfo(id)};
            } catch (Exception e) {
                return null;
            }
        } else if(url.contains("spotify:playlist:")){
            String id = url.substring(17);
            return loadPlaylist(id);
        }
        return null;
    }

    private String[] loadPlaylist(String id){
        Playlist pl = null;
        try {
            pl = spotifyApi.getPlaylist(id).build().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlaylistTrack[] playlist = pl.getTracks().getItems();
        String[] plYtSearch = new String[playlist.length];
        for (int i = 0; i < playlist.length; i++) {
            try {
                plYtSearch[i] = getTrackInfo(playlist[i].getTrack().getId());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return plYtSearch;
    }

    private String getTrackInfo(String id) throws Exception{
        Track t = spotifyApi.getTrack(id).build().execute();
        return t.getName() + " " + getArtists(t);
    }

    private String getArtists(Track t){
        String s = "";
        for (ArtistSimplified a:t.getArtists()) {
            s += a.getName() + " ";
        }
        return s.substring(0, s.length() -2);
    }
}