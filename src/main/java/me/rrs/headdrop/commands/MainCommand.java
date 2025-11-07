package me.rrs.headdrop.commands;


import me.rrs.headdrop.HeadDrop;
import me.rrs.headdrop.listener.HeadGUI;
import me.rrs.headdrop.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final Lang lang = new Lang();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(miniMessage.deserialize("<gold>HeadDrop by RRS</gold>"));
        } else {
            switch (args[0].toLowerCase()) {
                case "help":
                    sendHelpMessage(sender);
                    break;
                case "reload":
                    reloadConfigAndLang(sender);
                    break;
                case "leaderboard":
                    showLeaderboard(sender);
                    break;
                case "debug":
                    generateDebugFile(sender);
                    break;
                case "gui":
                    openGUI(sender);
                    break;
                case "admin":
                    handleAdminCommand(sender, args);
                    break;
            }
        }
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        if (sender instanceof Player player) {
            Component message = miniMessage.deserialize("""
            <dark_green>HeadDrop</dark_green> <reset>plugin by RRS.
            
            <aqua>> <light_purple>/headdrop help</light_purple> <reset>-> you already discovered it!
            
            <aqua>> <light_purple>/headdrop reload</light_purple> <reset>-> reload plugin config.
            
            <aqua>> <light_purple>/myhead</light_purple> <reset>-> Get your head.
            
            <aqua>> <light_purple>/head &lt;player Name&gt;</light_purple> <reset>-> Get another player head.
            """);

            player.sendMessage(message);
        }
    }

    private void reloadConfigAndLang(CommandSender sender) {
        if (sender instanceof Player player) {
            if (player.hasPermission("headdrop.reload")) {
                try {
                    HeadDrop.getInstance().getLang().reload();
                    HeadDrop.getInstance().getConfiguration().reload();
                    Component message = miniMessage.deserialize("<green>[HeadDrop]</green> <reset>Reloaded");
                    sender.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                lang.noPerm(player);
            }
        } else {
            try {
                HeadDrop.getInstance().getConfiguration().reload();
                HeadDrop.getInstance().getLang().reload();
                Bukkit.getLogger().info(HeadDrop.getInstance().getLang().getString("Reload"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showLeaderboard(CommandSender sender) {
        if (!sender.hasPermission("headdrop.view.leaderboard")){
            return;
        }
        if (!HeadDrop.getInstance().getConfiguration().getBoolean("Database.Enable")){
            Bukkit.getLogger().severe("[HeadDrop] Enable database on config!");
            if (sender instanceof Player) sender.sendMessage("[HeadDrop] This is an error. report this to admin!");
            return;
        }
        Map<String, Integer> playerData = HeadDrop.getInstance().getDatabase().getPlayerData();
        List<Map.Entry<String, Integer>> sortedData = new ArrayList<>(playerData.entrySet());
        sortedData.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        sender.sendMessage(miniMessage.deserialize("<gold><bold>=-=-= Top Head Hunters =-=-=</bold></gold>"));
        sender.sendMessage(miniMessage.deserialize("<gray>----------------------------</gray>"));

        for (int i = 0; i < Math.min(sortedData.size(), 10); i++) {
            Map.Entry<String, Integer> entry = sortedData.get(i);
            Component message = miniMessage.deserialize("""
             <aqua>#%d</aqua> <yellow>%s</yellow> - <green>%d</green> <gold>Head(s)</gold>
            """.formatted(i + 1, entry.getKey(), entry.getValue()));
            sender.sendMessage(message);
        }

        sender.sendMessage(miniMessage.deserialize("<gray>----------------------------</gray>"));

    }

    private void generateDebugFile(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            try {
                File debugFile = new File(HeadDrop.getInstance().getDataFolder().getAbsolutePath() + File.separator + "debug.txt");
                if (debugFile.exists()) {
                    debugFile.delete();
                }
                debugFile.createNewFile();

                try (FileWriter writer = new FileWriter(debugFile)) {
                    writer.write("Server Name: " + Bukkit.getServer().getName() + "\n");
                    writer.write("Server Version: " + Bukkit.getServer().getVersion() + "\n");
                    writer.write("Plugin Version: " + HeadDrop.getInstance().getDescription().getVersion() + "\n");
                    writer.write("Java Version: " + System.getProperty("java.version") + "\n");
                    writer.write("Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n");
                    writer.write("\n");
                    writer.write("Require-Killer-Player: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Require-Killer-Player") + "\n");
                    writer.write("Killer-Require-Permission: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Killer-Require-Permission") + "\n");
                    writer.write("Enable-Looting: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Enable-Looting") + "\n");
                    writer.write("Enable-Perm-Chance: " + HeadDrop.getInstance().getConfiguration().getBoolean("Config.Enable-Perm-Chance") + "\n");
                    writer.write("Database: " + HeadDrop.getInstance().getConfiguration().getBoolean("Database.Online") + "\n");
                    writer.write("Premium: " + "True" + "\n");
                }
                Bukkit.getLogger().info("[HeadDrop-Debug] debug.txt file created!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openGUI(CommandSender sender) {
        if (sender instanceof Player player) {
            if (player.hasPermission("headdrop.gui.view")) {
                HeadGUI gui = new HeadGUI();
                player.openInventory(gui.getInventory());
            } else {
                lang.pcmd();
            }
        }
    }

    private void handleAdminCommand(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("headdrop.admin") && !sender.isOp()) {
            sender.sendMessage(miniMessage.deserialize("<red>[HeadDrop]</red> <reset>You don't have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize("<gold>[HeadDrop Admin]</gold> <reset>Usage: /headdrop admin <subcommand>"));
            sender.sendMessage(miniMessage.deserialize("<aqua>  - forcemode <admin|all|off></aqua> <gray>- Set 100% drop rate mode</gray>"));
            sender.sendMessage(miniMessage.deserialize("<aqua>  - holiday <check|force|clear></aqua> <gray>- Manage holiday system</gray>"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "forcemode":
                handleForceModeCommand(sender, args);
                break;
            case "holiday":
                handleHolidayCommand(sender, args);
                break;
            default:
                sender.sendMessage(miniMessage.deserialize("<red>[HeadDrop]</red> <reset>Unknown admin subcommand."));
                break;
        }
    }

    private void handleHolidayCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize("<gold>[HeadDrop Holiday]</gold> <reset>Current Status:"));

            me.rrs.headdrop.util.HolidayManager holidayManager = HeadDrop.getInstance().getHolidayManager();
            if (holidayManager == null) {
                sender.sendMessage(miniMessage.deserialize("<red>  Holiday Manager: NULL (not initialized!)</red>"));
                return;
            }

            // Force a fresh check
            holidayManager.checkActiveHoliday();

            boolean enabled = HeadDrop.getInstance().getConfiguration().getBoolean("Holidays.Enable", false);
            sender.sendMessage(miniMessage.deserialize("  <yellow>Enabled:</yellow> " + (enabled ? "<green>YES" : "<red>NO")));

            boolean isActive = holidayManager.isHolidayActive();
            String activeHoliday = holidayManager.getActiveHoliday();
            sender.sendMessage(miniMessage.deserialize("  <yellow>Active Holiday:</yellow> " +
                (isActive ? "<green>" + activeHoliday : "<gray>None")));

            sender.sendMessage(miniMessage.deserialize("  <yellow>Server Date:</yellow> <aqua>" + java.time.LocalDate.now()));

            sender.sendMessage(miniMessage.deserialize("\n<yellow>Commands:</yellow>"));
            sender.sendMessage(miniMessage.deserialize("  <white>/headdrop admin holiday check</white> <gray>- Show this status"));
            sender.sendMessage(miniMessage.deserialize("  <white>/headdrop admin holiday recheck</white> <gray>- Force fresh date check"));
            sender.sendMessage(miniMessage.deserialize("  <white>/headdrop admin holiday force <name></white> <gray>- Force activate a holiday"));
            sender.sendMessage(miniMessage.deserialize("  <white>/headdrop admin holiday clear</white> <gray>- Clear forced holiday"));
            return;
        }

        String subCommand = args[2].toLowerCase();
        me.rrs.headdrop.util.HolidayManager holidayManager = HeadDrop.getInstance().getHolidayManager();

        switch (subCommand) {
            case "check":
                handleHolidayCommand(sender, new String[]{args[0], args[1]}); // Recurse with no subcommand
                break;
            case "recheck":
                sender.sendMessage(miniMessage.deserialize("<yellow>[HeadDrop]</yellow> <reset>Forcing fresh holiday check..."));
                holidayManager.checkActiveHoliday(true); // Force recheck
                sender.sendMessage(miniMessage.deserialize("<green>[HeadDrop]</green> <reset>Holiday check complete! Check console for detailed logs."));
                // Show the result
                handleHolidayCommand(sender, new String[]{args[0], args[1]});
                break;
            case "force":
                if (args.length < 4) {
                    sender.sendMessage(miniMessage.deserialize("<red>[HeadDrop]</red> <reset>Usage: /headdrop admin holiday force <holiday-name>"));
                    sender.sendMessage(miniMessage.deserialize("<gray>Example: /headdrop admin holiday force Halloween</gray>"));
                    return;
                }
                String holidayName = args[3];
                holidayManager.forceHoliday(holidayName);
                sender.sendMessage(miniMessage.deserialize("<green>[HeadDrop]</green> <reset>Forced holiday: <gold>" + holidayName + "</gold>"));
                sender.sendMessage(miniMessage.deserialize("<yellow>Note:</yellow> <gray>This overrides date checking for testing purposes.</gray>"));
                break;
            case "clear":
                holidayManager.clearForcedHoliday();
                sender.sendMessage(miniMessage.deserialize("<green>[HeadDrop]</green> <reset>Cleared forced holiday. Using date-based detection."));
                break;
            default:
                sender.sendMessage(miniMessage.deserialize("<red>[HeadDrop]</red> <reset>Unknown holiday subcommand. Use: check, recheck, force, or clear"));
                break;
        }
    }

    private void handleForceModeCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            HeadDrop.ForceMode currentMode = HeadDrop.getInstance().getForceMode();
            sender.sendMessage(miniMessage.deserialize("<gold>[HeadDrop]</gold> <reset>Current force mode: <aqua>" + currentMode + "</aqua>"));
            sender.sendMessage(miniMessage.deserialize("<yellow>Usage:</yellow> <white>/headdrop admin forcemode <admin|all|off></white>"));
            sender.sendMessage(miniMessage.deserialize("  <aqua>admin</aqua> <gray>- 100% drop rate for ops/admins only</gray>"));
            sender.sendMessage(miniMessage.deserialize("  <aqua>all</aqua> <gray>- 100% drop rate for everyone</gray>"));
            sender.sendMessage(miniMessage.deserialize("  <aqua>off</aqua> <gray>- Normal drop rates</gray>"));
            return;
        }

        String modeArg = args[2].toLowerCase();
        HeadDrop.ForceMode newMode;

        switch (modeArg) {
            case "admin":
                newMode = HeadDrop.ForceMode.ADMIN;
                HeadDrop.getInstance().setForceMode(newMode);
                sender.sendMessage(miniMessage.deserialize("<green>[HeadDrop]</green> <reset>Force mode set to <gold>ADMIN</gold>. Ops/admins now have 100% drop rate!"));
                break;
            case "all":
                newMode = HeadDrop.ForceMode.ALL;
                HeadDrop.getInstance().setForceMode(newMode);
                sender.sendMessage(miniMessage.deserialize("<green>[HeadDrop]</green> <reset>Force mode set to <gold>ALL</gold>. Everyone now has 100% drop rate!"));
                break;
            case "off":
                newMode = HeadDrop.ForceMode.OFF;
                HeadDrop.getInstance().setForceMode(newMode);
                sender.sendMessage(miniMessage.deserialize("<green>[HeadDrop]</green> <reset>Force mode <gold>disabled</gold>. Normal drop rates restored."));
                break;
            default:
                sender.sendMessage(miniMessage.deserialize("<red>[HeadDrop]</red> <reset>Invalid mode. Use: admin, all, or off"));
                break;
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!cmd.getName().equals("headdrop")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("help", "reload", "leaderboard", "gui"));
            if (sender.hasPermission("headdrop.admin") || sender.isOp()) {
                completions.add("admin");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("headdrop.admin") || sender.isOp()) {
                return Arrays.asList("forcemode", "holiday");
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("headdrop.admin") || sender.isOp()) {
                if (args[1].equalsIgnoreCase("forcemode")) {
                    return Arrays.asList("admin", "all", "off");
                } else if (args[1].equalsIgnoreCase("holiday")) {
                    return Arrays.asList("check", "recheck", "force", "clear");
                }
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("holiday") && args[2].equalsIgnoreCase("force")) {
            if (sender.hasPermission("headdrop.admin") || sender.isOp()) {
                return Collections.singletonList("Halloween");
            }
        }

        return Collections.emptyList();
    }
}