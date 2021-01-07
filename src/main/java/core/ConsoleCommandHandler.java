package core;

import org.json.simple.JSONObject;

import java.util.Scanner;

public class ConsoleCommandHandler {

    Engine engine;

    public ConsoleCommandHandler(Engine engine) {
        this.engine = engine;
        new Thread(new SystemInListener()).start();
    }

    public JSONObject handleApiCommand(JSONObject req){
        String request = "";
        JSONObject response = new JSONObject();
        request = (String) req.get("req");
        if(req.containsKey("c1")){
            request = request + " " + req.get("c1");
            if(req.containsKey("c2")){
                request = request + " " + req.get("c2");
            }
        }

        if(handleConsoleCommand(request) == false){
            response.put("status", "400");
            response.put("response", "invalid command");
        } else {
            response.put("status", "200");
            response.put("response", "executed command!");
        }
        return response;
    }

    public boolean handleConsoleCommand(String command) {
        String args0;
        try {
            args0 = command.split(" ")[0];
        } catch (Exception e) {
            return false;
        }
        switch (args0.toLowerCase()) {
            case "save":
                engine.saveProperties();
                break;

            case "load":
                engine.loadProperties();
                break;
            case "debug":
                engine.getProperties().debug = !engine.getProperties().debug;
                System.out.println("Debug is now " + engine.getProperties().debug);
                break;

            case "showtime":
                engine.getProperties().showTime = !engine.getProperties().showTime;
                System.out.println("Show time is now " + engine.getProperties().showTime);
                break;

            case "startbot":
                engine.getDiscApplicationEngine().startBotApplication();
                engine.getBotRequestApi().boot(false);
                break;

            case "stopbot":
                engine.getDiscApplicationEngine().shutdownBotApplication();
                break;

            case "disctoken":
                try {
                    engine.getProperties().discBotApplicationToken = command.split(" ")[1];
                } catch (Exception e) {
                    engine.getUtilityBase().printOutput("Invalid!", false);
                    return false;
                }
                engine.getUtilityBase().printOutput("Setted Discord token", false);
                break;

            case "stop":
                engine.shutdown();
                break;

            case "apiport":
                String key = "";
                try {
                    key = command.split(" ")[1];
                } catch (Exception e) {
                    engine.getUtilityBase().printOutput("Invalid", false);
                    return false;
                }
                try {
                    engine.getProperties().apiPort = Integer.parseInt(key);
                    engine.getUtilityBase().printOutput("Set api port to " + key, false);
                } catch (Exception e){
                    engine.getUtilityBase().printOutput("Invalid", false);
                }
                break;

            case "help":
                System.out.println("load - loads all files (override)\nsave - saves all files\nstartBot - starts the bot...UwU\nstopBot - stops the bot\ndisctoken <token> - sets api token\napiport <port> - chnages api port\ndebug - turns on debug mode to see more\nshowtime - shows time at console output");
                break;

            default:
                System.out.println("unknown command! Use \"help\" to help...yourself :D");
                break;
        }
        return true;
    }

    private class SystemInListener implements Runnable {

        @Override
        public void run() {
            String line;
            Scanner scanner = new Scanner(System.in);
            while (true) {
                line = scanner.nextLine();
                try {
                    handleConsoleCommand(line);
                } catch (Exception e){
                    System.out.println("error while executing command");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String extractMessage(String extractor, int startAt){
        String message = "";
        String[] commandSplit = extractor.split(" ");
        if (commandSplit.length > startAt) {
            message = commandSplit[startAt];
            for (int i = startAt+1; i < commandSplit.length; i++) {
                if(commandSplit[i].endsWith("\\n"))
                    message = message + " " + commandSplit[i].replace("\\n", "") + "\n";
                else
                    message = message + " " + commandSplit[i];
            }
        } else {
            engine.getUtilityBase().printOutput("Invalid amount of characters!", false);
            return null;
        }
        return message;
    }

    private String convert(String s){
        return s.replace("\\n", "\n");
    }
}