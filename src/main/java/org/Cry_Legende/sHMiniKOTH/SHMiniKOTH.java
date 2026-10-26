package org.Cry_Legende.sHMiniKOTH;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound; // AJOUT
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;


public class SHMiniKOTH extends JavaPlugin {

    private static SHMiniKOTH instance;
    private KingManager kingManager;
    private ZoneManager zoneManager;
    private BukkitTask rewardTask;
    private BukkitTask countdownSoundTask; // AJOUT
    private volatile long lastRewardTimeMillis;
    private KingPlaceholderExpansion expansion;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // S'assure que les nouvelles options de config sont présentes
        kingManager = new KingManager(this);
        zoneManager = new ZoneManager(this); // Doit être initialisé avant de l'utiliser dans les tâches

        getServer().getPluginManager().registerEvents(new EventListener(this, kingManager, zoneManager), this);
        getCommand("shminikoth").setExecutor(new CommandSHMiniKOTH(this, zoneManager));

        startRewardTask();
        startCountdownSoundTask(); // AJOUT

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new KingPlaceholderExpansion(this, kingManager);
            expansion.register();
            getLogger().info("PlaceholderAPI détecté, expansion enregistrée.");
        } else {
            getLogger().warning("PlaceholderAPI non trouvé. Les placeholders ne fonctionneront pas.");
        }
    }

    @Override
    public void onDisable() {
        if (rewardTask != null) rewardTask.cancel();
        if (countdownSoundTask != null) countdownSoundTask.cancel(); // AJOUT
        // L'expansion persiste chez PlaceholderAPI : on la retire pour ne pas garder l'ancienne instance du plugin
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
        instance = null;
    }

    public static SHMiniKOTH getInstance() {
        return instance;
    }

    public KingManager getKingManager() {
        return kingManager;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public long getLastRewardTimeMillis() {
        return lastRewardTimeMillis;
    }

    public void startRewardTask() {
        int intervalSeconds = getConfig().getInt("rewards.interval", 60);
        int intervalTicks = Math.max(1, intervalSeconds) * 20;
        this.lastRewardTimeMillis = System.currentTimeMillis();

        rewardTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (kingManager.getCurrentKing() != null) {
                for (String cmd : getConfig().getStringList("rewards.commands")) {
                    String command = cmd.replace("%player%", kingManager.getCurrentKing().getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
                this.lastRewardTimeMillis = System.currentTimeMillis();
            }
            // Si pas de roi, lastRewardTimeMillis n'est pas mis à jour,
            // donc le prochain "cycle" de récompense attendra qu'un roi soit là
            // et que l'intervalle complet soit écoulé depuis la *dernière récompense effective*.
        }, intervalTicks, intervalTicks);
    }

    // NOUVELLE MÉTHODE POUR LA TÂCHE DU SON
    public void startCountdownSoundTask() {
        if (!getConfig().getBoolean("rewards.countdown_sound.enabled", false)) {
            return; // Ne pas démarrer la tâche si la fonctionnalité est désactivée
        }

        // Son lu une fois : un nom invalide était signalé à chaque seconde du compte à rebours
        String soundName = getConfig().getString("rewards.countdown_sound.sound", "BLOCK_NOTE_BLOCK_HAT");
        Sound sound = sound(soundName);
        if (sound == null) {
            getLogger().warning("Son configuré pour le compte à rebours KOTH invalide: " + soundName);
            return;
        }
        float volume = (float) getConfig().getDouble("rewards.countdown_sound.volume", 1.0);
        float pitch = (float) getConfig().getDouble("rewards.countdown_sound.pitch", 1.0);

        countdownSoundTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (kingManager.getCurrentKing() == null) {
                return; // Pas de roi, pas de son
            }

            Location center = zoneManager.getCenterLocation();
            if (center == null) {
                //getLogger().warning("La zone KOTH n'est pas définie, impossible de jouer le son du compte à rebours.");
                return; // Zone non définie
            }

            long now = System.currentTimeMillis();
            int rewardIntervalSeconds = getConfig().getInt("rewards.interval", 60);
            long nextRewardTime = lastRewardTimeMillis + (rewardIntervalSeconds * 1000L);
            long remainingMillis = nextRewardTime - now;
            int remainingSeconds = (int) (remainingMillis / 1000);

            int triggerSeconds = getConfig().getInt("rewards.countdown_sound.seconds_trigger", 3);

            if (remainingSeconds <= triggerSeconds && remainingSeconds > 0) {
                // Jouer le son au centre de la zone
                // Les joueurs proches l'entendront en fonction de la distance d'audition par défaut du son.
                center.getWorld().playSound(center, sound, volume, pitch);
            }
        }, 0L, 20L); // Exécuter toutes les secondes (20 ticks)
    }

    /**
     * Son lu en config : ancien nom (ENTITY_PLAYER_LEVELUP) ou clé (minecraft:entity.player.levelup).
     */
    static Sound sound(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (s.indexOf(':') >= 0 || s.indexOf('.') >= 0) {
            NamespacedKey key = NamespacedKey.fromString(s.toLowerCase(Locale.ROOT));
            return key == null ? null : Registry.SOUND_EVENT.get(key);
        }
        try {
            return (Sound) Sound.class.getField(s.toUpperCase(Locale.ROOT)).get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}