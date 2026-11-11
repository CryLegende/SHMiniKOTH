package org.Cry_Legende.sHMiniKOTH;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Locale;


public class SHMiniKOTH extends JavaPlugin {

    private static SHMiniKOTH instance;
    private KingManager kingManager;
    private ZoneManager zoneManager;
    private BukkitTask rewardTask;
    private BukkitTask countdownSoundTask;
    private BukkitTask reignTask;
    private volatile long lastRewardTimeMillis;
    private KingPlaceholderExpansion expansion;
    private Messages messages;
    private EventListener eventListener;

    /** Fichier de SHLogs où sont écrites les récompenses du roi. */
    private static final String LOG_FILE = "recompenses";

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // S'assure que les nouvelles options de config sont présentes
        messages = new Messages(this);
        kingManager = new KingManager(this);
        zoneManager = new ZoneManager(this); // Doit être initialisé avant de l'utiliser dans les tâches

        eventListener = new EventListener(this, kingManager, zoneManager);
        getServer().getPluginManager().registerEvents(eventListener, this);
        CommandSHMiniKOTH command = new CommandSHMiniKOTH(this, zoneManager);
        PluginCommand shminikoth = getCommand("shminikoth");
        shminikoth.setExecutor(command);
        shminikoth.setTabCompleter(command);

        startRewardTask();
        startCountdownSoundTask();
        startReignTask();

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
        cancelTasks();
        // L'expansion persiste chez PlaceholderAPI : on la retire pour ne pas garder l'ancienne instance du plugin
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
        instance = null;
    }

    private void cancelTasks() {
        if (rewardTask != null) rewardTask.cancel();
        if (countdownSoundTask != null) countdownSoundTask.cancel();
        if (reignTask != null) reignTask.cancel();
        rewardTask = null;
        countdownSoundTask = null;
        reignTask = null;
    }

    /**
     * Relit config.yml et messages.yml, puis réapplique ce qui en dépend : zone, plaque, règles de la zone et les trois
     * tâches (récompense, son du décompte, règne). Si l'un des fichiers est invalide, rien n'est appliqué : l'ancienne
     * configuration reste en place et l'expéditeur reçoit le fichier et l'erreur. Le roi en cours n'est pas touché ;
     * le décompte de la prochaine récompense repart de zéro.
     *
     * @return vrai si tout a été rechargé
     */
    public boolean reload(CommandSender sender) {
        YamlConfiguration newMessages = parse(sender, "messages.yml");
        YamlConfiguration newConfig = parse(sender, "config.yml");
        if (newMessages == null || newConfig == null) {
            return false;
        }
        reloadConfig();
        messages.apply(newMessages);
        zoneManager.loadZone();
        eventListener.reload();
        cancelTasks();
        startRewardTask();
        startCountdownSoundTask();
        startReignTask();
        return true;
    }

    private YamlConfiguration parse(CommandSender sender, String name) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(new File(getDataFolder(), name));
            return yaml;
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Rechargement annulé, " + name + " est invalide : " + e.getMessage());
            String text = String.valueOf(e.getMessage()).strip();
            int end = text.indexOf('\n');
            String error = end < 0 ? text : text.substring(0, end).strip();
            sender.sendMessage(messages.get("reload-failed",
                "&7&l« &c&lSH &8&l» &7Rechargement &cannulé&7 : &c%file% &7est invalide (&c%error%&7). L'ancienne configuration reste active.",
                "%file%", name, "%error%", error.length() > 160 ? error.substring(0, 160) + "..." : error));
            return null;
        }
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

    public Messages getMessages() {
        return messages;
    }

    public long getLastRewardTimeMillis() {
        return lastRewardTimeMillis;
    }

    public void startRewardTask() {
        int intervalSeconds = getConfig().getInt("rewards.interval", 60);
        int intervalTicks = Math.max(1, intervalSeconds) * 20;
        this.lastRewardTimeMillis = System.currentTimeMillis();

        rewardTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            Player king = kingManager.getCurrentKing();
            if (king != null) {
                boolean given = false;
                for (String cmd : getConfig().getStringList("rewards.commands")) {
                    String command = cmd.replace("%player%", king.getName());
                    if (Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                        given = true;
                    }
                }
                if (given) {
                    ActionLog.write(this, LOG_FILE, getConfig()
                            .getString("rewards.log_format", "%player% -> récompense du roi du spawn")
                            .replace("%player%", king.getName()));
                }
                this.lastRewardTimeMillis = System.currentTimeMillis();
            }
            // Si pas de roi, lastRewardTimeMillis n'est pas mis à jour,
            // donc le prochain "cycle" de récompense attendra qu'un roi soit là
            // et que l'intervalle complet soit écoulé depuis la *dernière récompense effective*.
        }, intervalTicks, intervalTicks);
    }

    /**
     * Commandes console exécutées à chaque seconde de règne (section {@code reign}), par exemple pour compter
     * les secondes du mini-tournoi « Roi du spawn ». Avec {@code require-in-zone}, une seconde ne compte que si
     * le roi est dans la zone : il garde la couronne en partant, pas le compteur.
     */
    private void startReignTask() {
        java.util.List<String> commands = getConfig().getStringList("reign.commands");
        if (commands.isEmpty()) {
            return;
        }
        boolean requireInZone = getConfig().getBoolean("reign.require-in-zone", true);
        reignTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            org.bukkit.entity.Player king = kingManager.getCurrentKing();
            if (king == null || !king.isOnline()) {
                return;
            }
            if (requireInZone && !zoneManager.isInZone(king.getLocation())) {
                return;
            }
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", king.getName()));
            }
        }, 20L, 20L);
    }

    /** Joue un son au centre de la zone pendant les dernières secondes avant la récompense. */
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