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

    /**
     * Position de la zone : le monde reste un nom, car il peut ne pas être encore chargé au démarrage
     * (PhantomWorlds). La résoudre dans onEnable donnait une zone morte, voire un plantage à la sauvegarde.
     */
    private record Pos(String world, double x, double y, double z) {
        static Pos of(Location loc) {
            return new Pos(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        }

        Location toLocation() {
            return new Location(Bukkit.getWorld(world), x, y, z);
        }

        String serialize() {
            return world + "," + x + "," + y + "," + z;
        }
    }

    private final SHMiniKOTH plugin;
    private Pos pos1;
    private Pos pos2;
    private String pressurePlateTypeString;
    private Material pressurePlateMaterial;

    public ZoneManager(SHMiniKOTH plugin) {
        this.plugin = plugin;
        loadZone();
    }

    public void loadZone() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("zone.pos1") && config.contains("zone.pos2")) {
            pos1 = parsePos(config.getString("zone.pos1"));
            pos2 = parsePos(config.getString("zone.pos2"));
        }
        // Lecture seule : la configuration n'est pas réécrite au démarrage
        applyPressurePlateType(config.getString("zone.pressure_plate", "STONE_PRESSURE_PLATE"));
    }

    public void saveZone() {
        FileConfiguration config = plugin.getConfig();
        if (pos1 != null && pos2 != null) {
            config.set("zone.pos1", pos1.serialize());
            config.set("zone.pos2", pos2.serialize());
        }
        config.set("zone.pressure_plate", pressurePlateTypeString);
        plugin.saveConfig();
    }

    private Pos parsePos(String s) {
        try {
            String[] parts = s.split(",");
            return new Pos(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        } catch (Exception e) {
            plugin.getLogger().severe("Erreur de parsing de la position: " + s);
            return null;
        }
    }

    public boolean isInZone(Location loc) {
        if (pos1 == null || pos2 == null || loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(pos1.world()))
            return false;
        double minX = Math.min(pos1.x(), pos2.x());
        double maxX = Math.max(pos1.x(), pos2.x());
        double minY = Math.min(pos1.y(), pos2.y());
        double maxY = Math.max(pos1.y(), pos2.y());
        double minZ = Math.min(pos1.z(), pos2.z());
        double maxZ = Math.max(pos1.z(), pos2.z());

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
        applyPressurePlateType(pressurePlateType);
        if (plugin.isEnabled()) {
            saveZone();
        }
    }

    private void applyPressurePlateType(String pressurePlateType) {
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
    }


    public void setPos1(Location loc) {
        this.pos1 = Pos.of(loc);
        saveZone();
    }

    public Location getPos1() { // AJOUT getter pour la logique du son
        return pos1 == null ? null : pos1.toLocation();
    }

    public void setPos2(Location loc) {
        this.pos2 = Pos.of(loc);
        saveZone();
    }

    public Location getPos2() { // AJOUT getter pour la logique du son
        return pos2 == null ? null : pos2.toLocation();
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
        if (!pos1.world().equals(pos2.world())) {
            plugin.getLogger().warning("Les positions pos1 et pos2 du KOTH ne sont pas dans le même monde !");
            return null;
        }
        World world = Bukkit.getWorld(pos1.world());
        if (world == null) {
            return null; // Monde pas encore chargé
        }

        double centerX = (pos1.x() + pos2.x()) / 2.0;
        double centerY = (pos1.y() + pos2.y()) / 2.0; // Le milieu vertical de la zone
        double centerZ = (pos1.z() + pos2.z()) / 2.0;

        return new Location(world, centerX, centerY, centerZ);
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
