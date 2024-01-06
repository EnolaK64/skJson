package cz.coffee.skjson.api;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
/**
 * Copyright coffeeRequired nd contributors
 * <p>
 * Created: středa (12.07.2023)
 */
@SuppressWarnings("ALL")
public class SkJsonCommand implements CommandExecutor {
    String formatDesc(String desc) {
        if (desc.contains("%nl%")) {
            var builder = new StringBuilder();
            var parts = desc.split("%nl%");
            for (String part : parts) {
                builder.append("\n").append("\t\t  ").append("- ").append(part);
            }
            return builder.toString();
        }
        return desc;
    }

    private void sendAbout(CommandSender sender) {
        sender.sendMessage(ColorWrapper.translate("&7SkJson revision version: &a" + Config.pluginYaml.get("revision-version")));
        sender.sendMessage(ColorWrapper
                .translate("&7Description: &f" +
                        formatDesc(Config.getConfig().plugin.getPluginMeta().getDescription()))
        );
        sender.sendMessage(ColorWrapper
                .translate("&7SkJson version: &f" + Config.getConfig().plugin.getPluginMeta().getVersion()));
        sender.sendMessage(ColorWrapper.translate("&7Author: &a" + Config.getConfig().plugin.getPluginMeta().getAuthors()));
        sender.sendMessage(ColorWrapper.translate("&7API-version: &6" + Config.getConfig().plugin.getPluginMeta().getAPIVersion()));
        sender.sendMessage(ColorWrapper.translate("&7Website: &f" + Config.getConfig().plugin.getPluginMeta().getWebsite()));
        sender.sendMessage(ColorWrapper.translate("&7GitHub: &f" + "https://www.github.com/SkJsonTeam/SkJson"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("skjson")) {
            if (args.length == 0) {
                sender.sendMessage(ColorWrapper.translate("&7Usage: &a/skjson reload"));
                sender.sendMessage(ColorWrapper.translate("&7Usage: &a/skjson run-tests"));
                sender.sendMessage(ColorWrapper.translate("&7Usage: &a/skjson about"));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(ColorWrapper.translate(Config.PLUGIN_PREFIX + "⚠ &econfig reloading..."));
                try {
                    Config.getConfig().loadConfigFile(false);
                    sender.sendMessage(ColorWrapper.translate(
                            Config.PLUGIN_PREFIX + "&7New path delimiter: &e" + Config.PATH_VARIABLE_DELIMITER));
                    sender.sendMessage(ColorWrapper.translate(Config.PLUGIN_PREFIX + "&7reload &asuccessfully."));
                    return true;
                } catch (Exception ex) {
                    sender.sendMessage(ColorWrapper.translate(Config.PLUGIN_PREFIX + "&7reload &cunsuccessfully."));
                    return false;
                }
            } else if (args[0].equalsIgnoreCase("about")) {
                sendAbout(sender);
                return true;
            }
            return false;
        }
        sender.sendMessage(ColorWrapper.translate("&7Usage: &a/skjson reload"));
        sender.sendMessage(ColorWrapper.translate("&7Usage: &a/skjson run-tests"));
        sender.sendMessage(ColorWrapper.translate("&7Usage: &a/skjson about"));
        return true;
    }
}
