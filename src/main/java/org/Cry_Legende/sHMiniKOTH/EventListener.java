package org.Cry_Legende.sHMiniKOTH;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;

public class EventListener implements Listener {

    private final SHMiniKOTH plugin;
    private final KingManager kingManager;
    private final ZoneManager zoneManager;
    // Lus une fois : ces événements (dégâts, durabilité) sont fréquents
    private final boolean loseHealth;
    private final boolean pvpEnabled;
    private final boolean loseDurability;

    public EventListener(SHMiniKOTH plugin, KingManager kingManager, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.kingManager = kingManager;
        this.zoneManager = zoneManager;
        this.loseHealth = plugin.getConfig().getBoolean("features.lose_health", true);
        this.pvpEnabled = plugin.getConfig().getBoolean("features.pvp", true);
        this.loseDurability = plugin.getConfig().getBoolean("features.lose_durability", true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        // Vérifier si le type de la plaque correspond à celle configurée
        if (block.getType() != zoneManager.getPressurePlateMaterial()) return;
        // Vérifier que le bloc est dans la zone définie
        if (!zoneManager.isInZone(block.getLocation())) return;

        Player player = event.getPlayer();
        // Si le joueur n'est pas déjà roi, il devient le roi
        if (kingManager.getCurrentKing() == null || !kingManager.getCurrentKing().equals(player)) {
            kingManager.setNewKing(player);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Gérer les dégâts non-PvP
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!zoneManager.isInZone(player.getLocation())) return;
        // Si lose_health est désactivé, annuler tous les dégâts (non-PvP)
        if (!loseHealth) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        if (!zoneManager.isInZone(victim.getLocation())) return;
        if (!pvpEnabled) {
            event.setCancelled(true);
        } else if (!loseHealth) {
            // Les joueurs peuvent se taper, mais leur vie ne descend pas
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!zoneManager.isInZone(player.getLocation())) return;
        if (!loseDurability) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Si le roi se déconnecte, effacer son statut
        if (kingManager.getCurrentKing() != null && kingManager.getCurrentKing().equals(event.getPlayer())) {
            kingManager.clearKing();
        }
    }
}
