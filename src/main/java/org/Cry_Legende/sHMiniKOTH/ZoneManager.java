package org.Cry_Legende.sHMiniKOTH;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ZoneManager {

    private final SHMiniKOTH plugin;
    private Location pos1;
    private Location pos2;
    private String pressurePlateTypeString;
    private Material pressurePlateMaterial;

    public ZoneManager(SHMiniKOTH plugin) {
        this.plugin = plugin;
        loadZone();
    }

    public void loadZone() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("zone.pos1") && config.contains("zone.pos2")) {
            pos1 = parseLocation(config.getString("zone.pos1"));
            pos2 = parseLocation(config.getString("zone.pos2"));
        }
        setPressurePlateType(config.getString("zone.pressure_plate", "STONE_PRESSURE_PLATE"));
    }

    public void saveZone() {
        FileConfiguration config = plugin.getConfig();
        if (pos1 != null && pos2 != null) {
            config.set("zone.pos1", serializeLocation(pos1));
            config.set("zone.pos2", serializeLocation(pos2));
        }
        config.set("zone.pressure_plate", pressurePlateTypeString);
        plugin.saveConfig();
    }

    private Location parseLocation(String s) {
        try {
            String[] parts = s.split(",");
            World world = Bukkit.getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur de parsing de la position: " + s);
            return null;
        }
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    public boolean isInZone(Location loc) {
        if (pos1 == null || pos2 == null || loc == null || loc.getWorld() == null || !loc.getWorld().equals(pos1.getWorld()))
            return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return (x >= minX && x <= maxX) && (y >= minY && y <= maxY) && (z >= minZ && z <= maxZ);
    }

    public Material getPressurePlateMaterial() {
        return pressurePlateMaterial;
    }

    public String getPressurePlateType() {
        return pressurePlateTypeString;
    }

    public void setPressurePlateType(String pressurePlateType) {
        this.pressurePlateTypeString = pressurePlateType.toUpperCase();
        Material mat = Material.matchMaterial(this.pressurePlateTypeString);
        if (mat != null && mat.name().contains("PRESSURE_PLATE")) {
            this.pressurePlateMaterial = mat;
            plugin.getLogger().info("Type de plaque de pression défini sur: " + this.pressurePlateMaterial.name());
        } else {
            this.pressurePlateMaterial = Material.STONE_PRESSURE_PLATE; // Fallback
            plugin.getLogger().warning("Type de plaque de pression invalide: " + pressurePlateType + ". Utilisation de STONE_PRESSURE_PLATE par défaut.");
            this.pressurePlateTypeString = "STONE_PRESSURE_PLATE";
        }
        if (plugin.isEnabled()) {
            saveZone();
        }
    }


    public void setPos1(Location loc) {
        this.pos1 = loc;
        saveZone();
    }

    public Location getPos1() { // AJOUT getter pour la logique du son
        return pos1;
    }

    public void setPos2(Location loc) {
        this.pos2 = loc;
        saveZone();
    }

    public Location getPos2() { // AJOUT getter pour la logique du son
        return pos2;
    }

    /**
     * Calcule et retourne la localisation centrale de la zone KOTH.
     *
     * @return La Location du centre, ou null si la zone n'est pas complètement définie.
     */
    public Location getCenterLocation() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        // S'assurer que les deux positions sont dans le même monde
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            plugin.getLogger().warning("Les positions pos1 et pos2 du KOTH ne sont pas dans le même monde !");
            return null;
        }

        double centerX = (pos1.getX() + pos2.getX()) / 2.0;
        double centerY = (pos1.getY() + pos2.getY()) / 2.0; // Le milieu vertical de la zone
        double centerZ = (pos1.getZ() + pos2.getZ()) / 2.0;

        return new Location(pos1.getWorld(), centerX, centerY, centerZ);
    }


    public boolean isPlayerOnValidKothPlate(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        Block blockUnderPlayer = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (blockUnderPlayer.getType() != this.pressurePlateMaterial) {
            return false;
        }
        return isInZone(blockUnderPlayer.getLocation());
    }
}
