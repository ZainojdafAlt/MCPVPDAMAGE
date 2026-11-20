package club.mcpvp.damage;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCPVPDamage extends JavaPlugin implements Listener {

    private boolean playersOnly;
    private boolean crystalEnabled, anchorEnabled;
    private double crystalMult, crystalMin, anchorMult, anchorMin;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void load() {
        reloadConfig();
        var c = getConfig();
        playersOnly = c.getBoolean("players-only", true);
        crystalEnabled = c.getBoolean("crystal.enabled", true);
        crystalMult = c.getDouble("crystal.multiplier", 2.0);
        crystalMin = c.getDouble("crystal.min-damage", 0.0);
        anchorEnabled = c.getBoolean("anchor.enabled", true);
        anchorMult = c.getDouble("anchor.multiplier", 2.0);
        anchorMin = c.getDouble("anchor.min-damage", 0.0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrystal(EntityDamageByEntityEvent e) {
        if (!crystalEnabled || !(e.getDamager() instanceof EnderCrystal)) return;
        if (e.getCause() != DamageCause.ENTITY_EXPLOSION) return;
        if (playersOnly && !(e.getEntity() instanceof Player)) return;
        e.setDamage(Math.max(e.getDamage() * crystalMult, crystalMin));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnchor(EntityDamageByBlockEvent e) {
        if (!anchorEnabled || e.getCause() != DamageCause.BLOCK_EXPLOSION) return;
        if (playersOnly && !(e.getEntity() instanceof Player)) return;

        // The anchor is already gone when the damage fires, so use the captured state.
        BlockState state = e.getDamagerBlockState();
        Material type = state != null ? state.getType()
                : (e.getDamager() != null ? e.getDamager().getType() : null);
        if (type != Material.RESPAWN_ANCHOR) return;

        e.setDamage(Math.max(e.getDamage() * anchorMult, anchorMin));
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacyAmpersand().deserialize(text));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                load();
                msg(sender, getConfig().getString("messages.reloaded", "&aReloaded."));
                return true;
            }
            case "get" -> {
                msg(sender, "&7Crystal multiplier: &e" + crystalMult + " &7| Anchor multiplier: &e" + anchorMult);
                return true;
            }
            case "set" -> {
                if (args.length != 3) return false;
                String key = args[1].toLowerCase();
                if (!key.equals("crystal") && !key.equals("anchor")) {
                    msg(sender, "&cUse crystal or anchor.");
                    return true;
                }
                double value;
                try {
                    value = Double.parseDouble(args[2]);
                } catch (NumberFormatException ex) {
                    msg(sender, "&c'" + args[2] + "' is not a number.");
                    return true;
                }
                if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                    msg(sender, "&cMultiplier must be 0 or higher.");
                    return true;
                }
                getConfig().set(key + ".multiplier", value);
                saveConfig();
                load();
                msg(sender, "&a" + key + " multiplier set to &e" + value);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.List.of("set", "get", "reload").stream()
                    .filter(x -> x.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return java.util.List.of("crystal", "anchor").stream()
                    .filter(x -> x.startsWith(args[1].toLowerCase())).toList();
        }
        return java.util.List.of();
    }
}
