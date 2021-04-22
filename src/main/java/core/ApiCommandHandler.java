package core;

import commands.audioCore.AudioCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.json.simple.JSONObject;

public class ApiCommandHandler {

    private AudioCommand audioCommand;

    public ApiCommandHandler(Engine engine) {
        audioCommand = new AudioCommand(engine);
    }

    public JSONObject handleApiCommand(JSONObject msg, Engine engine) {
        JSONObject data = (JSONObject) msg.get("data");
        String inst = "";
        String guild = "";
        String member = "";
        String response = "";

        try {
            inst = (String) data.get("inst");
            guild = (String) data.get("guild");
            member = (String) data.get("member");
        } catch (Exception e) {
        }

        String[] args = inst.split(" ");
        String argsNull;
        Guild g = null;
        Member m = null;

        try {
            g = engine.getDiscApplicationEngine().getBotJDA().getGuildById(guild);
        } catch (Exception e) {
            if(engine.getProperties().debug)
                e.printStackTrace();
            engine.getUtilityBase().printOutput("Error in finding guild!", true);
        }

        try {
            m = g.getMemberById(member);
        } catch (Exception e) {
            if(engine.getProperties().debug)
                e.printStackTrace();
            engine.getUtilityBase().printOutput("Error in finding member!", true);
        }

        if (args != null)
            if (args.length == 0 || args.length == 1)
                argsNull = inst;
            else
                argsNull = args[0];
        else
            return engine.getFileUtils().convertStringToJson("{ \"status\" : \"400\", \"response\" : \"Invalid request\"}");

        engine.getUtilityBase().printOutput("Performing command: " + argsNull + " with instructions: " + inst, true);

        argsNull = argsNull.replace(" ", "");
        switch (argsNull.toLowerCase()) {
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

            case "repeat":
                response = audioCommand.repeat(m);
                break;

            case "help":
                response =
                        "{ \"status\" : \"200\", \"response\" : \"" +
                                "p url ytsearch - play a song\n" +
                                "s - skip\n" +
                                "stop - stops song from bein played\n" +
                                "sh - shuffle playlist\n" +
                                "pl - shows playlist\n" +
                                "add url ytsearch - add song to playlist\n" +
                                "info - shows info from current song" +
                                "\" }";
                break;

            default:
                response = "{ \"status\" : \"400\", \"response\" : \"Command not found\"}";
        }
        engine.getUtilityBase().printOutput("Found " + response, true);

        return engine.getFileUtils().convertStringToJson(response);
    }

    public AudioCommand getAudioCommand() {
        return audioCommand;
    }
}
