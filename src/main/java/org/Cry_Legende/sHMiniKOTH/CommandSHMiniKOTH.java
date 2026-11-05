package org.Cry_Legende.sHMiniKOTH;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSHMiniKOTH implements CommandExecutor {

    private final SHMiniKOTH plugin;
    private final ZoneManager zoneManager;

    public CommandSHMiniKOTH(SHMiniKOTH plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessages().get("console_not_allowed",
                    "&7&l« &c&lSH &8&l» &7Cette commande est uniquement utilisable par un &cjoueur&7."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("shminikoth.admin")) {
            player.sendMessage(plugin.getMessages().get("no_permission",
                    "&7&l« &c&lSH &8&l» &7Vous n'avez pas la &cpermission &7d'utiliser cette commande."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(plugin.getMessages().get("usage",
                    "&7&l« &e&lSH &8&l» &7Usage : &e/shminikoth setpos1|setpos2|setplate <valeur>&7."));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "setpos1":
                zoneManager.setPos1(player.getLocation());
                player.sendMessage(plugin.getMessages().get("pos1_saved",
                        "&7&l« &a&lSH &8&l» &7Position &a1 &7sauvegardée."));
                break;
            case "setpos2":
                zoneManager.setPos2(player.getLocation());
                player.sendMessage(plugin.getMessages().get("pos2_saved",
                        "&7&l« &a&lSH &8&l» &7Position &a2 &7sauvegardée."));
                break;
            case "setplate":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessages().get("usage_setplate",
                            "&7&l« &e&lSH &8&l» &7Usage : &e/shminikoth setplate <type_de_plaque>&7."));
                    return true;
                }
                zoneManager.setPressurePlateType(args[1].toUpperCase());
                player.sendMessage(plugin.getMessages().get("plate_set",
                        "&7&l« &a&lSH &8&l» &7Type de plaque défini sur &a%plate%&7.", "%plate%", args[1].toUpperCase()));
                break;
            default:
                player.sendMessage(plugin.getMessages().get("unknown_command",
                        "&7&l« &c&lSH &8&l» &7Commande &cinconnue&7."));
                break;
        }
        return true;
    }
}
