package org.Cry_Legende.sHMiniKOTH;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
            sender.sendMessage("Cette commande est uniquement utilisable par un joueur.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("shminikoth.admin")) {
            player.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("Usage: /shminikoth setpos1|setpos2|setplate <valeur>"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "setpos1":
                zoneManager.setPos1(player.getLocation());
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&aPosition 1 sauvegardée."));
                break;
            case "setpos2":
                zoneManager.setPos2(player.getLocation());
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&aPosition 2 sauvegardée."));
                break;
            case "setplate":
                if (args.length < 2) {
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("Usage: /shminikoth setplate <type_de_plaque>"));
                    return true;
                }
                zoneManager.setPressurePlateType(args[1].toUpperCase());
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&aType de plaque défini sur " + args[1].toUpperCase()));
                break;
            default:
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cCommande inconnue."));
                break;
        }
        return true;
    }
}
