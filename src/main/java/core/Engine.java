package core;

public class Engine {

    private final UtilityBase utilityBase = new UtilityBase(this);
    private Properties properties;
    private final FileUtils fileUtils = new FileUtils(this);
    private ApiCommandHandler commandHandler;
    private final BotRequestApi botRequestApi = new BotRequestApi(this);
    private final DiscApplicationEngine discApplicationEngine = new DiscApplicationEngine(this);

    public void boot(String[] args) {
        loadProperties();
        new ConsoleCommandHandler(this);

        commandHandler = new ApiCommandHandler(this);
        if (args.length >= 1)
            if (args[0].equals("start")) {
                botRequestApi.boot(false);
                discApplicationEngine.startBotApplication();
            } else if(args[0].equals("test")){
                System.out.println(commandHandler.handleApiCommand(fileUtils.convertStringToJson("{\"data\": {\"inst\" : \"help\"}" +
                        "}"), this));
            }
    }

    public void saveProperties() {
        utilityBase.printOutput(" !Saving properties!", false);
        try {
            fileUtils.saveObject(fileUtils.home + "/properties.prop", properties);
        } catch (Exception e) {
            if (properties.debug) {
                e.printStackTrace();
            }
            utilityBase.printOutput(" !!!Error while saving properties - maybe no permission!!!", false);
        }
    }

    public void loadProperties() {
        utilityBase.printOutput(" !Loading properties!", false);
        try {
            properties = (Properties) fileUtils.loadObject(fileUtils.home + "/properties.prop");
        } catch (Exception e) {
            e.printStackTrace();
            utilityBase.printOutput(" !!!Error while loading properties - maybe never created -> creating new file!!!", false);
            properties = new Properties();
        }
        if (properties == null) {
            properties = new Properties();
        }

        saveProperties();
    }

    public void shutdown() {
        saveProperties();
        System.exit(0);
    }

    public UtilityBase getUtilityBase() {
        return utilityBase;
    }

    public Properties getProperties() {
        return properties;
    }

    public FileUtils getFileUtils() {
        return fileUtils;
    }

    public ApiCommandHandler getCommandHandler() {
        return commandHandler;
    }

    public BotRequestApi getBotRequestApi() {
        return botRequestApi;
    }


    public DiscApplicationEngine getDiscApplicationEngine() {
        return discApplicationEngine;
    }
}
