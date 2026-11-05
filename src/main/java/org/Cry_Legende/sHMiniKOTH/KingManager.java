package org.Cry_Legende.sHMiniKOTH;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class KingManager {

    private final SHMiniKOTH plugin;
    private Player currentKing;
    private long kingStartTime; // en millisecondes

    public KingManager(SHMiniKOTH plugin) {
        this.plugin = plugin;
    }

    public Player getCurrentKing() {
        return currentKing;
    }

    public long getKingStartTime() {
        return kingStartTime;
    }

    public void setNewKing(Player newKing) {
        Player oldKing = currentKing;
        currentKing = newKing;
        kingStartTime = System.currentTimeMillis();
        // Diffuser le message dans la zone
        Component compMessage = plugin.getMessages().get("new_king",
                "&7&l« &e&lSH &8&l» &7&e%player% &7est le nouveau roi du spawn !", "%player%", newKing.getName());
        plugin.getServer().getOnlinePlayers().stream()
            .filter(p -> plugin.getZoneManager().isInZone(p.getLocation()))
            .forEach(p -> p.sendMessage(compMessage));
        // Envoyer le message king_lost à l'ancien roi seulement s'il n'est pas dans la zone
        if (oldKing != null && oldKing.isOnline() && !plugin.getZoneManager().isInZone(oldKing.getLocation())) {
            oldKing.sendMessage(plugin.getMessages().get("king_lost",
                    "&7&l« &6&lSH &8&l» &7Vous n'êtes plus le roi du spawn, &6%player% &7vous a volé votre place !",
                    "%player%", newKing.getName()));
        }
    }

    public void clearKing() {
        currentKing = null;
        kingStartTime = 0;
    }
}
