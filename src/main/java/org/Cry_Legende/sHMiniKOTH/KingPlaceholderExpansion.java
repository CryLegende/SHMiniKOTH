package org.Cry_Legende.sHMiniKOTH;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class KingPlaceholderExpansion extends PlaceholderExpansion {

    private final SHMiniKOTH plugin;
    private final KingManager kingManager;

    public KingPlaceholderExpansion(SHMiniKOTH plugin, KingManager kingManager) {
        this.plugin = plugin;
        this.kingManager = kingManager;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "shminikoth";
    }

    @Override
    public String getAuthor() {
        return "Cry_Legende";
    }

    @Override
    public String getVersion() {
        // Vous pouvez incrémenter la version si vous le souhaitez
        return "1.2"; // Version incrémentée pour refléter le changement
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (identifier.equalsIgnoreCase("king")) {
            if (kingManager.getCurrentKing() != null) {
                return kingManager.getCurrentKing().getName();
            } else {
                return plugin.getConfig().getString("placeholder.default_king", "Aucun");
            }
        } else if (identifier.equalsIgnoreCase("time")) {
            if (kingManager.getCurrentKing() != null) {
                long elapsed = System.currentTimeMillis() - kingManager.getKingStartTime();
                return formatDuration(elapsed, plugin.getConfig().getString("placeholder.king_time_format", "HH:mm:ss"));
            } else {
                return formatDuration(0, plugin.getConfig().getString("placeholder.king_time_format", "HH:mm:ss"));
            }
        } else if (identifier.equalsIgnoreCase("reward_countdown")) {
            if (kingManager.getCurrentKing() != null) {
                long lastRewardTime = plugin.getLastRewardTimeMillis();
                long now = System.currentTimeMillis();
                int rewardIntervalSeconds = plugin.getConfig().getInt("rewards.interval", 60);
                long nextRewardTime = lastRewardTime + (rewardIntervalSeconds * 1000L);
                long remainingMillis = nextRewardTime - now;

                if (remainingMillis <= 0) {
                    return formatDuration(0, plugin.getConfig().getString("placeholder.reward_countdown_format", "mm:ss"));
                }
                return formatDuration(remainingMillis, plugin.getConfig().getString("placeholder.reward_countdown_format", "mm:ss"));
            } else {
                int rewardIntervalSeconds = plugin.getConfig().getInt("rewards.interval", 60);
                return plugin.getConfig().getString("placeholder.default_reward_countdown", "N/A");
            }
        }
        return null;
    }

    private String formatDuration(long millis, String format) {
        if (millis < 0) millis = 0;

        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String formattedString = format;
        formattedString = formattedString.replace("HH", String.format("%02d", hours));
        formattedString = formattedString.replace("H", String.valueOf(hours));
        formattedString = formattedString.replace("mm", String.format("%02d", minutes));
        formattedString = formattedString.replace("m", String.valueOf(minutes));
        formattedString = formattedString.replace("ss", String.format("%02d", seconds));
        formattedString = formattedString.replace("s", String.valueOf(seconds));

        return formattedString;
    }
}