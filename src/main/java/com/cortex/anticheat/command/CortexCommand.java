package com.cortex.anticheat.command;

import com.cortex.anticheat.CortexAntiCheatPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class CortexCommand implements CommandExecutor {
    private final CortexAntiCheatPlugin plugin;

    public CortexCommand(CortexAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Cortex configuration reloaded.");
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Cortex " + ChatColor.GRAY + "server=" + plugin.serverId()
                + " role=" + plugin.serverRole()
                + " bans24h=" + plugin.punishmentService().bansLast24Hours());
        return true;
    }
}
