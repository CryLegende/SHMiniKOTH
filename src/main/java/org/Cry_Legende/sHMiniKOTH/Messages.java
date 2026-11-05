package org.Cry_Legende.sHMiniKOTH;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/** Textes envoyés aux joueurs, lus dans messages.yml. */
public class Messages {

    private final FileConfiguration config;

    public Messages(SHMiniKOTH plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * @param key          clé dans messages.yml
     * @param fallback     texte utilisé si la clé est absente
     * @param replacements paires variable / valeur, par exemple "%player%", "Nina"
     */
    public Component get(String key, String fallback, String... replacements) {
        String text = config.getString(key, fallback);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
