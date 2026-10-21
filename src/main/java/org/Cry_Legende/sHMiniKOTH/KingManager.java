package org.Cry_Legende.sHMiniKOTH;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
        String message = plugin.getConfig().getString("messages.new_king", "&a%player% est le nouveau roi !");
        message = message.replace("%player%", newKing.getName());
        Component compMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        plugin.getServer().getOnlinePlayers().stream()
            .filter(p -> plugin.getZoneManager().isInZone(p.getLocation()))
            .forEach(p -> p.sendMessage(compMessage));
        // Envoyer le message king_lost à l'ancien roi seulement s'il n'est pas dans la zone
        if (oldKing != null && oldKing.isOnline() && !plugin.getZoneManager().isInZone(oldKing.getLocation())) {
            String lostMessage = plugin.getConfig().getString("messages.king_lost", "&cVous n'êtes plus le roi, %player% vous a volé votre place !");
            lostMessage = lostMessage.replace("%player%", newKing.getName());
            oldKing.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(lostMessage));
        }
    }

    public void clearKing() {
        currentKing = null;
        kingStartTime = 0;
    }
}
