package core;

import commands.audioCore.AudioCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.json.simple.JSONObject;

public class ApiCommandHandler {

    private final AudioCommand audioCommand = new AudioCommand();

    public JSONObject handleApiCommand(JSONObject msg, Engine engine){
        JSONObject data = (JSONObject) msg.get("data");
        String inst = "";
        String guild = "";
        String member = "";
        String response = "";

        try {
            inst = (String) data.get("inst");
            guild = (String) data.get("guild");
            member = (String) data.get("member");
        } catch (Exception e){
        }

        String[] args = inst.split(" ");
        Guild g = engine.getDiscApplicationEngine().getBotJDA().getGuildById(guild);
        Member m = g.getMemberById(member);

        switch (args[0].toLowerCase()){
            case "add":
                response = audioCommand.add(args, m);
                break;

            case "p":
            case "play":
                response = audioCommand.play(args, m);
                break;

            case "stop":
                response = audioCommand.stop(m);
                break;

            case "s":
            case "skip":
                response = audioCommand.skip(args, m);
                break;

            case "sh":
            case "shuffle":
                response = audioCommand.shuffle(m);
                break;

            case "now":
            case "info":
                response = audioCommand.info(m);
                break;

            case "playlist":
            case "pl":
            case "q":
            case "queue":
                response = audioCommand.showQueue(args, m);
                break;

            case "help":
                response =
                        "p <url/ytsearch> - play a song\n" +
                        "s - skip\n" +
                        "stop - stop\n" +
                        "sh - shuffle playlist\n" +
                        "pl - shows playlist\n" +
                        "add <url/ytsearch> - add song to playlist\n" +
                        "info - shows info from current song";
                break;
        }

        return engine.getFileUtils().convertStringToJson(response);
    }

    public AudioCommand getAudioCommand() {
        return audioCommand;
    }
}
