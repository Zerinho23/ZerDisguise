package me.zerith.zerdisguise;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
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

    public Component getPrefix() {
        return component(plugin.getConfig().getString("prefix", "&8[&#CC88FF&lZerDisguise&8] &r"));
    }

    public String getMenuTitle() {
        return plugin.getConfig().getString("menu.title", "&8≫ &#CC88FF&lZerDisguise &8≪");
    }

    public String getConfirmTitle() {
        return plugin.getConfig().getString("menu.confirm-title", "&8≫ &#CC88FF&lConfirmar disfraz &8≪");
    }

    public int getMenuSize() {
        int s = plugin.getConfig().getInt("menu.size", 54);
        return (s % 9 == 0 && s >= 9 && s <= 54) ? s : 54;
    }

    public String getPromptTitle()    { return plugin.getConfig().getString("prompt.title",    "&#CC88FF&lEscribe tu disfraz"); }
    public String getPromptSubtitle() { return plugin.getConfig().getString("prompt.subtitle", "&7Escribe el nombre en el chat..."); }
    public int    getPromptFadeIn()   { return plugin.getConfig().getInt("prompt.fade-in",  10); }
    public int    getPromptStay()     { return plugin.getConfig().getInt("prompt.stay",     80); }
    public int    getPromptFadeOut()  { return plugin.getConfig().getInt("prompt.fade-out", 10); }

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
            String glassStr   = section.getString(key + ".glass",      "PURPLE_STAINED_GLASS_PANE");
            Material mat;
            Material glass;
            try { mat = Material.valueOf(matStr.toUpperCase()); }
            catch (IllegalArgumentException e) { mat = Material.STONE; }
            try { glass = Material.valueOf(glassStr.toUpperCase()); }
            catch (IllegalArgumentException e) { glass = Material.PURPLE_STAINED_GLASS_PANE; }
            list.add(new RankEntry(key, name, color, prefix, permission, mat, glass));
        }
        return list;
    }

    public record RankEntry(String id, String name, String color,
                            String prefix, String permission,
                            Material material, Material glass) {}

    // ── Mensajes ──────────────────────────────────────────────────

    public String getMsgNoPermission()  { return plugin.getConfig().getString("messages.no-permission",   "&cNo tienes permiso para hacer eso."); }
    public String getMsgPlayerOnly()    { return plugin.getConfig().getString("messages.player-only",     "&cEste comando es solo para jugadores."); }
    public String getMsgApplied()       { return plugin.getConfig().getString("messages.disguise-applied","&7Disfraz aplicado&8: &#CC88FF{disguise} &8| &7Rango&8: &f{rank}"); }
    public String getMsgRemoved()       { return plugin.getConfig().getString("messages.disguise-removed","&aDisfraz removido correctamente."); }
    public String getMsgDeathRemoved()  { return plugin.getConfig().getString("messages.disguise-death",  "&7Tu disfraz fue eliminado al morir."); }
    public String getMsgNotFound()      { return plugin.getConfig().getString("messages.player-not-found","&cJugador &f{player} &cno encontrado."); }
    public String getMsgWriteDisguise() { return plugin.getConfig().getString("messages.write-disguise",  "&#CC88FF&lEscribe el nombre en el chat &8(&7cancel &8= cancelar&8)"); }
    public String getMsgCancelled()     { return plugin.getConfig().getString("messages.cancelled",       "&cCancelado."); }
    public String getMsgReload()        { return plugin.getConfig().getString("messages.reload",          "&aConfiguracion recargada correctamente."); }
    public String getMsgInvalidName()   { return plugin.getConfig().getString("messages.invalid-name",    "&cNombre invalido. Solo letras, numeros y _ (max 16)."); }
    public String getMsgBypass()        { return plugin.getConfig().getString("messages.bypass",          "&cEse jugador no puede ser disfrazado por otros."); }
    public String getMsgNotDisguised()  { return plugin.getConfig().getString("messages.not-disguised",   "&7No tienes ningun disfraz activo."); }
    public String getMsgRankApplied()    { return plugin.getConfig().getString("messages.rank-applied",     "&7Rango visual aplicado&8: &f{rank}"); }

    // ── Action Bar ─────────────────────────────────────────────

    /** ¿Mostrar barra de acción mientras el jugador está disfrazado? */
    public boolean isActionbarEnabled() {
        return plugin.getConfig().getBoolean("actionbar.enabled", true);
    }

    /**
     * Intervalo de refresco en ticks (20 = 1 segundo).
     * Leído una sola vez al iniciar el plugin — cambios requieren /zd reload + reinicio de tarea.
     */
    public int getActionbarInterval() {
        int v = plugin.getConfig().getInt("actionbar.interval", 20);
        return Math.max(1, v);
    }

    /**
     * Formato del mensaje de la barra de acción.
     * Placeholders: {nombre} {rango} {tiempo} {tiempo_min} {tiempo_seg}
     */
    public String getActionbarFormat() {
        return plugin.getConfig().getString(
            "actionbar.format",
            "&8[&d&l\u2697&8] &7Disfrazado como&8: &d{nombre} &8| &7Rango&8: {rango} &8| &7Tiempo&8: &a{tiempo}");
    }

    // ── Colores / Components ──────────────────────────────────────

    public Component component(String text) {
        if (text == null) return Component.empty();
        return SECTION_HEX.deserialize(toSectionFormat(text));
    }

    public Component componentAny(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (text.contains("\u00A7")) return SECTION_HEX.deserialize(text);
        return component(text);
    }

    public String colorize(String text) {
        return LegacyComponentSerializer.legacySection().serialize(component(text));
    }

    public String strip(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)[&§][0-9A-FK-ORX]", "")
                   .replaceAll("(?i)[&§]x([&§][0-9A-F]){6}", "")
                   .replaceAll("&#[0-9A-Fa-f]{6}", "");
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
