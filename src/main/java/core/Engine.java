package core;

public class Engine {

    UtilityBase utilityBase = new UtilityBase(this);
    Properties properties;
    FileUtils fileUtils = new FileUtils(this);
    ApiCommandHandler commandHandler = new ApiCommandHandler();
    BotRequestApi botRequestApi = new BotRequestApi(this);
    DiscApplicationEngine discApplicationEngine = new DiscApplicationEngine(this);

    public void boot(String[] args) {
        loadProperties();
        new ConsoleCommandHandler(this);
        if (args.length >= 1)
            if (args[0].equals("start")) {
                botRequestApi.boot(false);
                discApplicationEngine.startBotApplication();
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
