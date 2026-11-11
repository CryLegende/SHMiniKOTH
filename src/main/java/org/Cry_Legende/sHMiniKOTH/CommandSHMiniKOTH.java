package org.Cry_Legende.sHMiniKOTH;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /shminikoth : setpos1, setpos2, setplate, reload et info, chacune avec sa permission. Les sous-commandes qui n'ont
 * pas besoin d'une position (setplate, reload, info) marchent depuis la console.
 */
public class CommandSHMiniKOTH implements TabExecutor {

    private static final String PERMISSION_SETPOS1 = "shminikoth.admin.setpos1";
    private static final String PERMISSION_SETPOS2 = "shminikoth.admin.setpos2";
    private static final String PERMISSION_SETPLATE = "shminikoth.admin.setplate";
    private static final String PERMISSION_RELOAD = "shminikoth.admin.reload";
    private static final String PERMISSION_INFO = "shminikoth.admin.info";

    private static final String NO_PERMISSION =
        "&7&l« &c&lSH &8&l» &7Vous n'avez pas la &cpermission &7d'utiliser cette commande.";

    private final SHMiniKOTH plugin;
    private final ZoneManager zoneManager;

    public CommandSHMiniKOTH(SHMiniKOTH plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "setpos1" -> {
                Player player = playerWith(sender, PERMISSION_SETPOS1);
                if (player != null) {
                    zoneManager.setPos1(player.getLocation());
                    player.sendMessage(plugin.getMessages().get("pos1_saved",
                        "&7&l« &a&lSH &8&l» &7Position &a1 &7sauvegardée."));
                }
            }
            case "setpos2" -> {
                Player player = playerWith(sender, PERMISSION_SETPOS2);
                if (player != null) {
                    zoneManager.setPos2(player.getLocation());
                    player.sendMessage(plugin.getMessages().get("pos2_saved",
                        "&7&l« &a&lSH &8&l» &7Position &a2 &7sauvegardée."));
                }
            }
            case "setplate" -> {
                if (!allowed(sender, PERMISSION_SETPLATE)) {
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessages().get("usage_setplate",
                        "&7&l« &e&lSH &8&l» &7Usage : &e/shminikoth setplate <type_de_plaque>&7."));
                    return true;
                }
                zoneManager.setPressurePlateType(args[1].toUpperCase(Locale.ROOT));
                sender.sendMessage(plugin.getMessages().get("plate_set",
                    "&7&l« &a&lSH &8&l» &7Type de plaque défini sur &a%plate%&7.", "%plate%", args[1].toUpperCase(Locale.ROOT)));
            }
            case "reload" -> {
                if (allowed(sender, PERMISSION_RELOAD) && plugin.reload(sender)) {
                    sender.sendMessage(plugin.getMessages().get("reload-done",
                        "&7&l« &a&lSH &8&l» &7Configuration &arechargée&7. Le décompte de la prochaine récompense repart de zéro."));
                }
            }
            case "info" -> {
                if (allowed(sender, PERMISSION_INFO)) {
                    info(sender);
                }
            }
            default -> usage(sender);
        }
        return true;
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(plugin.getMessages().get("no_permission", NO_PERMISSION));
        return false;
    }

    /** Le joueur qui peut poser une position, ou null après avoir répondu (permission refusée, console). */
    private Player playerWith(CommandSender sender, String permission) {
        if (!allowed(sender, permission)) {
            return null;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get("console_not_allowed",
                "&7&l« &c&lSH &8&l» &7Cette commande est uniquement utilisable par un &cjoueur&7."));
            return null;
        }
        return player;
    }

    /** Une seule ligne d'usage, limitée aux sous-commandes que l'expéditeur peut utiliser. */
    private void usage(CommandSender sender) {
        List<String> allowed = allowedSubcommands(sender);
        if (allowed.isEmpty()) {
            sender.sendMessage(plugin.getMessages().get("no_permission", NO_PERMISSION));
            return;
        }
        sender.sendMessage(plugin.getMessages().get("usage-list",
            "&7&l« &e&lSH &8&l» &7Usage : &e/shminikoth %subcommands%&7.", "%subcommands%", String.join("|", allowed)));
    }

    private List<String> allowedSubcommands(CommandSender sender) {
        List<String> allowed = new ArrayList<>();
        if (sender.hasPermission(PERMISSION_SETPOS1)) allowed.add("setpos1");
        if (sender.hasPermission(PERMISSION_SETPOS2)) allowed.add("setpos2");
        if (sender.hasPermission(PERMISSION_SETPLATE)) allowed.add("setplate");
        if (sender.hasPermission(PERMISSION_RELOAD)) allowed.add("reload");
        if (sender.hasPermission(PERMISSION_INFO)) allowed.add("info");
        return allowed;
    }

    /** Zone, plaque, roi en cours et prochaine récompense. */
    private void info(CommandSender sender) {
        Messages messages = plugin.getMessages();
        sender.sendMessage(messages.get("info-header", "&7&l« &e&lSH &8&l» &7Roi du spawn :"));
        if (zoneManager.isDefined()) {
            String world = zoneManager.getWorldName();
            String state = Bukkit.getWorld(world) != null ? "&achargé" : "&cnon chargé";
            sender.sendMessage(messages.get("info-zone", "&8• &7Zone : &e%pos1% &7à &e%pos2%",
                "%pos1%", zoneManager.describePos1(), "%pos2%", zoneManager.describePos2()));
            sender.sendMessage(messages.get("info-world", "&8• &7Monde : &e%world% &7(%state%&7)",
                "%world%", world, "%state%", state));
        } else {
            sender.sendMessage(messages.get("info-no-zone", "&8• &7Zone : &cnon définie &7(setpos1 et setpos2)"));
        }
        sender.sendMessage(messages.get("info-plate", "&8• &7Plaque : &e%plate%",
            "%plate%", zoneManager.getPressurePlateType()));

        Player king = plugin.getKingManager().getCurrentKing();
        if (king == null) {
            sender.sendMessage(messages.get("info-no-king", "&8• &7Roi : &eaucun"));
            return;
        }
        long reigning = System.currentTimeMillis() - plugin.getKingManager().getKingStartTime();
        sender.sendMessage(messages.get("info-king", "&8• &7Roi : &e%king% &7depuis &e%time%",
            "%king%", king.getName(), "%time%", duration(reigning)));
        int interval = plugin.getConfig().getInt("rewards.interval", 60);
        long remaining = plugin.getLastRewardTimeMillis() + interval * 1000L - System.currentTimeMillis();
        sender.sendMessage(messages.get("info-reward", "&8• &7Prochaine récompense : &e%time% &7(toutes les &e%interval% &7s)",
            "%time%", duration(Math.max(0, remaining)), "%interval%", String.valueOf(interval)));
    }

    private static String duration(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            completions.addAll(allowedSubcommands(sender));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setplate") && sender.hasPermission(PERMISSION_SETPLATE)) {
            for (Material material : Material.values()) {
                if (material.name().contains("PRESSURE_PLATE") && !material.name().startsWith("LEGACY_")) {
                    completions.add(material.name());
                }
            }
        }
        completions.removeIf(name -> !name.toLowerCase(Locale.ROOT).startsWith(typed));
        return completions;
    }
}
