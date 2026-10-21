package org.Cry_Legende.sHMiniKOTH;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound; // AJOUT
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;


public class SHMiniKOTH extends JavaPlugin {

    private static SHMiniKOTH instance;
    private KingManager kingManager;
    private ZoneManager zoneManager;
    private BukkitTask rewardTask;
    private BukkitTask countdownSoundTask; // AJOUT
    private long lastRewardTimeMillis;

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
            new KingPlaceholderExpansion(this, kingManager).register();
            getLogger().info("PlaceholderAPI détecté, expansion enregistrée.");
        } else {
            getLogger().warning("PlaceholderAPI non trouvé. Les placeholders ne fonctionneront pas.");
        }
    }

    @Override
    public void onDisable() {
        if (rewardTask != null) rewardTask.cancel();
        if (countdownSoundTask != null) countdownSoundTask.cancel(); // AJOUT
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
        int intervalTicks = intervalSeconds * 20;
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
                try {
                    String soundName = getConfig().getString("rewards.countdown_sound.sound", "BLOCK_NOTE_BLOCK_HAT");
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    float volume = (float) getConfig().getDouble("rewards.countdown_sound.volume", 1.0);
                    float pitch = (float) getConfig().getDouble("rewards.countdown_sound.pitch", 1.0);

                    // Jouer le son au centre de la zone
                    // Les joueurs proches l'entendront en fonction de la distance d'audition par défaut du son.
                    center.getWorld().playSound(center, sound, volume, pitch);

                } catch (IllegalArgumentException e) {
                    getLogger().warning("Son configuré pour le compte à rebours KOTH invalide: " + getConfig().getString("rewards.countdown_sound.sound"));
                    // Optionnel : désactiver la tâche ou la fonctionnalité pour éviter les spams de logs
                    // if (countdownSoundTask != null) countdownSoundTask.cancel();
                    // getConfig().set("rewards.countdown_sound.enabled", false);
                }
            }
        }, 0L, 20L); // Exécuter toutes les secondes (20 ticks)
    }
}