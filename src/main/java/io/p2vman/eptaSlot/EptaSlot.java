package io.p2vman.eptaSlot;

import com.google.common.collect.ImmutableList;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class EptaSlot extends JavaPlugin implements Listener {
    private List<Permission> permissions;
    private List<Handler> handlers = new ArrayList<>();
    public static Map<String, Object> convertSectionToMap(ConfigurationSection section) {
        Map<String, Object> result = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                result.put(key, convertSectionToMap((ConfigurationSection) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private boolean PlaceholderAPI = false;

    @Override
    public void onLoad() {
        System.setProperty("bstats.relocatecheck", "false");
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        permissions = Permission.loadPermissions(convertSectionToMap(getConfig().getConfigurationSection("permissions")), "hah...", PermissionDefault.OP);
        load();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            PlaceholderAPI = true;
            new SlotExpansion(this).register();
        }
        Metrics metrics = new Metrics(this, 24800);

        metrics.addCustomChart(new SimplePie("reserve", () -> String.valueOf(slots_reserve)));
    }

    private final String[] w1 = new String[] {
            "reload"
    };

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(args.length > 0)) {
            sender.sendMessage(String.format("/%s (%s)", getName(), String.join("/", w1)));
            return false;
        }
        switch (args[0]) {
            case "reload": {
                load();
                sender.sendMessage("Config reload.");
                break;
            }
        }
        return false;
    }

    public int slots_reserve = 0;
    private boolean server_players_change = false;
    public int slots = 0;
    private boolean enable = false;
    private String logic = "1";

    private void load() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration configuration = getConfig();
        enable = configuration.getBoolean("enable");
        slots_reserve = configuration.getInt("slots.reserve");
        server_players_change = configuration.getBoolean("server.players_change");
        slots = Math.max(Bukkit.getMaxPlayers() - slots_reserve, 0);
        handlers.clear();
        List<?> handlersList = getConfig().getList("handlers");
        if (handlersList != null) {
            for (Object obj : handlersList) {
                if (obj instanceof Map) {
                    Map<?, ?> section = (Map<?, ?>) obj;
                    Handler handler = new Handler();
                    handler.logic = (String) section.get("logic");
                    handler.permission = (String) section.get("permission");
                    handlers.add(handler);
                }
            }
        } else {
            getLogger().warning("Handlers list is empty or null!");
        }
        logic = configuration.getString("logic");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            return ImmutableList.of();
        } else if (args.length == 1) {
            String lastWord = args[args.length - 1];
            ArrayList<String> matchedPlayers = new ArrayList();
            Iterator var7 = Arrays.stream(w1).iterator();
            while(var7.hasNext()) {
                String name = (String)var7.next();
                if (StringUtil.startsWithIgnoreCase(name, lastWord)) {
                    matchedPlayers.add(name);
                }
            }

            Collections.sort(matchedPlayers, String.CASE_INSENSITIVE_ORDER);
            return matchedPlayers;
        } else {
            return ImmutableList.of();
        }
    }

    @Override
    public void onDisable() {

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (enable) {
            Player player = event.getPlayer();
            for (Handler handler : handlers) if (player.hasPermission(handler.permission)) {
                Map<String, Double> vars = new HashMap<>();
                vars.put("online", (double) Bukkit.getOnlinePlayers().size());
                vars.put("slots", (double) slots);
                vars.put("reserve", (double) slots_reserve);
                if (eval(handler.logic, vars) > 0) {
                    return;
                } else {
                    event.disallow(PlayerLoginEvent.Result.KICK_FULL, getMessage(player));
                }
            }
            Map<String, Double> vars = new HashMap<>();
            vars.put("online", (double) Bukkit.getOnlinePlayers().size());
            vars.put("slots", (double) slots);
            vars.put("reserve", (double) slots_reserve);
            if (eval(logic, vars) > 0) {
                return;
            } else {
                event.disallow(PlayerLoginEvent.Result.KICK_FULL, getMessage(player));
            }
        }
    }

    public String getMessage(OfflinePlayer player) {
        if (PlaceholderAPI) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', getConfig().getString("text.kick")));
        } else {
            return ChatColor.translateAlternateColorCodes('&', getConfig().getString("text.kick"));
        }
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (enable && server_players_change) {
            event.setMaxPlayers(slots);
        }
    }

    public static double eval(String expr, Map<String, Double> vars) {
        expr = expr.replaceAll("\\s+", "");
        for (String var : vars.keySet()) expr = expr.replace(var, vars.get(var).toString());

        if (expr.contains("^")) {
            String[] parts = expr.split("\\^", 2);
            return Math.pow(eval(parts[0], vars), eval(parts[1], vars));
        } else if (expr.contains("*")) {
            String[] parts = expr.split("\\*", 2);
            return eval(parts[0], vars) * eval(parts[1], vars);
        } else if (expr.contains("/")) {
            String[] parts = expr.split("/", 2);
            return eval(parts[0], vars) / eval(parts[1], vars);
        } else if (expr.contains("+")) {
            String[] parts = expr.split("\\+", 2);
            return eval(parts[0], vars) + eval(parts[1], vars);
        } else if (expr.contains("-")) {
            String[] parts = expr.split("-", 2);
            return eval(parts[0], vars) - eval(parts[1], vars);
        }

        if (expr.contains("AND")) {
            String[] parts = expr.split("AND", 2);
            return (eval(parts[0], vars) != 0 && eval(parts[1], vars) != 0) ? 1 : 0;
        }
        if (expr.contains("OR")) {
            String[] parts = expr.split("OR", 2);
            return (eval(parts[0], vars) != 0 || eval(parts[1], vars) != 0) ? 1 : 0;
        }
        if (expr.startsWith("NOT")) {
            return (eval(expr.substring(3), vars) == 0) ? 1 : 0;
        }
        if (expr.contains(">")) {
            String[] parts = expr.split(">", 2);
            return (eval(parts[0], vars) > eval(parts[1], vars)) ? 1 : 0;
        }
        if (expr.contains("<")) {
            String[] parts = expr.split("<", 2);
            return (eval(parts[0], vars) < eval(parts[1], vars)) ? 1 : 0;
        }
        if (expr.contains(">=")) {
            String[] parts = expr.split(">=", 2);
            return (eval(parts[0], vars) >= eval(parts[1], vars)) ? 1 : 0;
        }
        if (expr.contains("<=")) {
            String[] parts = expr.split("<=", 2);
            return (eval(parts[0], vars) <= eval(parts[1], vars)) ? 1 : 0;
        }
        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            return (eval(parts[0], vars) == eval(parts[1], vars)) ? 1 : 0;
        }
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            return (eval(parts[0], vars) != eval(parts[1], vars)) ? 1 : 0;
        }

        return Double.parseDouble(expr);
    }
}
