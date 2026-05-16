package me.zerith.zerdisguise;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer SECTION_HEX =
            LegacyComponentSerializer.legacySection().toBuilder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private final ZerDisguise plugin;

    public ConfigManager(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    // ── General ──────────────────────────────────────────────────────────────

    public Component getPrefix() {
        return component(plugin.getConfig().getString("prefix", "&8[&dZerDisguise&8] &r"));
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    public String getMenuTitle() {
        return plugin.getConfig().getString("menu.title", "&8» &dZerDisguise &8«");
    }

    public int getMenuSize() {
        int s = plugin.getConfig().getInt("menu.size", 27);
        return (s % 9 == 0 && s >= 9 && s <= 54) ? s : 27;
    }

    // ── Prompt ───────────────────────────────────────────────────────────────

    public String getPromptTitle()    { return plugin.getConfig().getString("prompt.title",    "&dEscribe tu disfraz"); }
    public String getPromptSubtitle() { return plugin.getConfig().getString("prompt.subtitle", "&7Escribe el nombre en el chat..."); }
    public int    getPromptFadeIn()   { return plugin.getConfig().getInt("prompt.fade-in",  10); }
    public int    getPromptStay()     { return plugin.getConfig().getInt("prompt.stay",     60); }
    public int    getPromptFadeOut()  { return plugin.getConfig().getInt("prompt.fade-out", 10); }

    // ── Ranks ────────────────────────────────────────────────────────────────

    public List<RankEntry> getRanks() {
        List<RankEntry> list = new ArrayList<>();
        var section = plugin.getConfig().getConfigurationSection("ranks");
        if (section == null) return list;

        for (String key : section.getKeys(false)) {
            String name       = section.getString(key + ".name",       key);
            String color      = section.getString(key + ".color",      "&7");
            String prefix     = section.getString(key + ".prefix",     "");
            String permission = section.getString(key + ".permission", "zerdisguise.rank." + key);
            String matStr     = section.getString(key + ".material",   "STONE");
            Material mat;
            try { mat = Material.valueOf(matStr.toUpperCase()); }
            catch (IllegalArgumentException e) { mat = Material.STONE; }
            list.add(new RankEntry(key, name, color, prefix, permission, mat));
        }
        return list;
    }

    public record RankEntry(String id, String name, String color,
                            String prefix, String permission, Material material) {}

    // ── Messages ─────────────────────────────────────────────────────────────

    public String getMsgNoPermission()  { return plugin.getConfig().getString("messages.no-permission", "&cNo tienes permiso."); }
    public String getMsgPlayerOnly()    { return plugin.getConfig().getString("messages.player-only",   "&cSolo jugadores."); }
    public String getMsgApplied()       { return plugin.getConfig().getString("messages.disguise-applied", "&aDisfratz aplicado: &d{disguise} &a| Rango: &f{rank}"); }
    public String getMsgRemoved()       { return plugin.getConfig().getString("messages.disguise-removed", "&aDisfratz removido."); }
    public String getMsgNotFound()      { return plugin.getConfig().getString("messages.player-not-found", "&cJugador no encontrado."); }
    public String getMsgWriteDisguise() { return plugin.getConfig().getString("messages.write-disguise", "&7Escribe el nombre del disfraz en el chat..."); }
    public String getMsgCancelled()     { return plugin.getConfig().getString("messages.cancelled", "&cCancelado."); }
    public String getMsgReload()        { return plugin.getConfig().getString("messages.reload", "&aConfiguración recargada."); }

    // ── Color parsing ─────────────────────────────────────────────────────────

    public Component component(String text) {
        return SECTION_HEX.deserialize(toSectionFormat(text));
    }

    public String colorize(String text) {
        return LegacyComponentSerializer.legacySection().serialize(component(text));
    }

    private static String toSectionFormat(String text) {
        Matcher hex = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 32);
        while (hex.find()) {
            StringBuilder rep = new StringBuilder("\u00A7x");
            for (char c : hex.group(1).toUpperCase().toCharArray()) rep.append('\u00A7').append(c);
            hex.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        hex.appendTail(sb);

        String raw = sb.toString();
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '&' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(next) >= 0) {
                    out.append('\u00A7').append(next);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
