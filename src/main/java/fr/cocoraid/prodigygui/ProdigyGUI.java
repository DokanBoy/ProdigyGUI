package fr.cocoraid.prodigygui;

import fr.cocoraid.prodigygui.bridge.EconomyBridge;
import fr.cocoraid.prodigygui.bridge.PlaceholderAPIBridge;
import fr.cocoraid.prodigygui.config.CoreConfig;
import fr.cocoraid.prodigygui.event.BreakBlockEvent;
import fr.cocoraid.prodigygui.event.ItemInteractEvent;
import fr.cocoraid.prodigygui.event.JoinQuitEvent;
import fr.cocoraid.prodigygui.language.Language;
import fr.cocoraid.prodigygui.language.LanguageLoader;
import fr.cocoraid.prodigygui.loader.CommandListener;
import fr.cocoraid.prodigygui.loader.FileLoader;
import fr.cocoraid.prodigygui.protocol.InteractableItemProtocol;
import fr.cocoraid.prodigygui.task.ThreeDimensionalGUITask;
import fr.cocoraid.prodigygui.threedimensionalgui.ThreeDimensionGUI;
import fr.cocoraid.prodigygui.threedimensionalgui.ThreeDimensionalMenu;
import fr.cocoraid.prodigygui.utils.CC;
import fr.cocoraid.prodigygui.utils.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProdigyGUI extends JavaPlugin {

    /**
     * TODO: make thing buyable
     */


    private Language language;
    private CoreConfig config;

    private static ProdigyGUI instance;
    private Metrics metrics;


    @Override
    public void onEnable() {
        instance = this;
        ConsoleCommandSender c = Bukkit.getServer().getConsoleSender();
        metrics = new Metrics(this);

        loadConfiguration();

        if (!EconomyBridge.setupEconomy()) {
            getLogger().warning("Vault with a compatible economy plugin was not found! Icons with a PRICE or commands that give money will not work.");
        }

        PlaceholderAPIBridge placeholderAPIBridge = new PlaceholderAPIBridge();
        placeholderAPIBridge.setupPlugin();
        if (placeholderAPIBridge.hasValidPlugin()) {
            getLogger().info("Hooked PlaceholderAPI");
        }

        new LanguageLoader(this);


        if(!LanguageLoader.getLanguages().containsKey(config.language.toLowerCase())) {
            c.sendMessage("§c Language not found ! Please check your language folder");
        } else
            language = LanguageLoader.getLanguage(config.language.toLowerCase());
        c.sendMessage(CC.d_green + "Language: " + (language == null ? "english" : config.language.toLowerCase()));
        if(language == null)
            language = LanguageLoader.getLanguage("english");


        new FileLoader(this);
        new InteractableItemProtocol(this);
        new ThreeDimensionalGUITask(this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(), this);
        Bukkit.getPluginManager().registerEvents(new JoinQuitEvent(), this);
        Bukkit.getPluginManager().registerEvents(new BreakBlockEvent(), this);
        Bukkit.getPluginManager().registerEvents(new ItemInteractEvent(), this);

    }

    @Override
    public void onDisable() {
        ProdigyGUIPlayer.getProdigyPlayers().values().stream().filter(pp -> pp.getThreeDimensionGUI() != null && pp.getThreeDimensionGUI().isSpawned()).forEach(pp -> {
            pp.getThreeDimensionGUI().closeGui();
        });
        try {
            config.save();
        }
        catch(final InvalidConfigurationException ex) {
            ex.printStackTrace();
            getLogger().log(Level.SEVERE, "Oooops ! Something went wrong while saving the configuration !");
        }
    }

    private void loadConfiguration() {
        final Logger logger = getLogger();
        try {
            config = new CoreConfig(new File(this.getDataFolder(), "configuration.yml"));
            config.load();
        } catch(final InvalidConfigurationException ex) {
            ex.printStackTrace();
            logger.log(Level.SEVERE, "Oooops ! Something went wrong while loading the configuration !");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }



    private ThreeDimensionalMenu checkConditions(CommandSender sender, String[] args) {
        if(sender instanceof Player) {
            Player p = (Player) sender;
            if(!p.hasPermission("prodigygui.other.open")) {
                p.sendMessage(language.no_permission);
                return null;
            }
        }

        if(Bukkit.getPlayer(args[2]) == null || (Bukkit.getPlayer(args[2]) != null && !Bukkit.getPlayer(args[2]).isOnline())) {
            sender.sendMessage("Player " + args[2] + " is not online");
            return null;
        }


        ThreeDimensionalMenu menu  = ThreeDimensionalMenu.getMenus().stream()
                .filter(m -> m.getFileName().replace(".yml", "").equalsIgnoreCase(args[1])).findAny()
                .orElseGet(() -> null);
        if(menu == null) {
            sender.sendMessage("§cMenu " + args[1] + " could not be found !");
            return null;
        } else return menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("prodigygui")) {

            if(args.length == 1) {
                if(args[0].equalsIgnoreCase("reload")) {

                    if(sender instanceof Player && !((Player)sender).hasPermission("prodigygui.reload")) {
                        sender.sendMessage("§cYou do not have the permission !");
                        return false;
                    }
                    ProdigyGUIPlayer.getProdigyPlayers().values().stream().filter(pp -> pp.getThreeDimensionGUI() != null && pp.getThreeDimensionGUI().isSpawned()).forEach(pp -> {
                        pp.getThreeDimensionGUI().closeGui();
                    });

                    try {
                        config.save();
                    }
                    catch(final InvalidConfigurationException ex) {
                        ex.printStackTrace();
                        getLogger().log(Level.SEVERE, "Oooops ! Something went wrong while saving the configuration !");
                    }
                    loadConfiguration();

                    ConsoleCommandSender c = Bukkit.getServer().getConsoleSender();
                    if(!LanguageLoader.getLanguages().containsKey(config.language.toLowerCase())) {
                        c.sendMessage("§c Language not found ! Please check your language folder");
                    } else
                        language = LanguageLoader.getLanguage(config.language.toLowerCase());
                    c.sendMessage(CC.d_green + "Language: " + (language == null ? "english" : config.language.toLowerCase()));
                    if(language == null)
                        language = LanguageLoader.getLanguage("english");
                    ThreeDimensionalMenu.getMenus().clear();
                    new FileLoader(this);
                    if(sender instanceof Player)
                        ((Player)sender).sendMessage("§bConfiguration reloaded !");
                    else
                        c.sendMessage("§bConfiguration reloaded !");


                }
            }

            //prodigygui open <menu> <player>
            if(args.length == 3) {
                ThreeDimensionalMenu menu = checkConditions(sender, args);
                if(menu == null) return false;

                new ThreeDimensionGUI(Bukkit.getPlayer(args[2]), menu)
                        .openGui();
            }



            //prodigygui open <menu> <yawRotation> <x> <y> <z> <playername>
            else if (args.length == 7) {
                if(args[0].equalsIgnoreCase("open")) {

                    ThreeDimensionalMenu menu = checkConditions(sender, args);
                    if (menu == null) return false;

                    try {
                        Double.valueOf(args[2]);
                    } catch (Exception e) {
                        sender.sendMessage("§cThe yaw rotation " + args[3] + " must be integer !");
                        sender.sendMessage("§c/prodigygui open <menu> <playername> <yawRotation> <x> <y> <z> ");
                        return false;
                    }

                    try {
                        Double.valueOf(args[4]);
                        Double.valueOf(args[5]);
                        Double.valueOf(args[6]);
                    } catch (Exception e) {
                        sender.sendMessage("§cThe y,y,z positions " + args[1] + " must be integer !");
                        sender.sendMessage("§c/prodigygui open <menu> <playername> <yawRotation> <x> <y> <z>");
                        return false;
                    }


                    new ThreeDimensionGUI(Bukkit.getPlayer(args[2]), menu)
                            .setRotation(Float.valueOf(args[3]))
                            .setCenter(Double.valueOf(args[4]), Double.valueOf(args[5]), Double.valueOf(args[6]))
                            .openGui();

                }
                //prodigygui open <menu> <playername> <yawRotation>
            } else if(args.length == 4) {
                if(args[0].equalsIgnoreCase("open")) {
                    ThreeDimensionalMenu menu = checkConditions(sender, args);
                    if(menu == null) return false;

                    try {
                        Double.valueOf(args[3]);
                    } catch (Exception e) {
                        sender.sendMessage("§cThe yaw rotation " + args[3] + " must be integer !");
                        sender.sendMessage("§c/prodigygui open <menu> <playername> <yawRotation>");
                        return false;
                    }

                    Player p = Bukkit.getPlayer(args[2]);
                    new ThreeDimensionGUI(p, menu)
                            .setRotation(Float.valueOf(args[3]))
                            .openGui();


                }

            } else {
                sender.sendMessage("§c/prodigygui open <menu> <playername> <yawRotation> <x> <y> <z>");
                sender.sendMessage("§c/prodigygui open <menu> <playername> <yawRotation>");
                sender.sendMessage("§c/prodigygui open <menu> <playername>");
            }

        }

        return false;
    }

    public CoreConfig getConfiguration() {
        return config;
    }

    public Language getLanguage() {
        return language;
    }

    public static ProdigyGUI getInstance() {
        return instance;
    }
}
