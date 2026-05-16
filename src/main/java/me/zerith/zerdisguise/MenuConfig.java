package me.zerith.zerdisguise;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee y expone toda la configuración visual del menú desde menu.yml.
 * Separado de config.yml para no mezclar la config del plugin con el diseño del menú.
 */
public class MenuConfig {

    private final ZerDisguise plugin;
    private FileConfiguration menu;

    public MenuConfig(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        menu = YamlConfiguration.loadConfiguration(file);

        // Fusionar defaults del JAR
        try (InputStream is = plugin.getResource("menu.yml")) {
            if (is != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(is, StandardCharsets.UTF_8));
                menu.setDefaults(defaults);
            }
        } catch (Exception ignored) {}
    }

    // ── Títulos ──────────────────────────────────────────────────

    public String getMainTitle()    { return str("titles.main",    "&8» &#CC88FFZerDisguise &8«"); }
    public String getConfirmTitle() { return str("titles.confirm", "&8» &#CC88FF&lConfirmar disfraz &8«"); }

    // ── Materiales del marco ──────────────────────────────────────

    public Material getBorderMaterial()  { return mat("design.border",  Material.GRAY_STAINED_GLASS_PANE); }
    public Material getFillerMaterial()  { return mat("design.filler",  Material.BLACK_STAINED_GLASS_PANE); }
    public Material getDividerMaterial() { return mat("design.divider", Material.PURPLE_STAINED_GLASS_PANE); }
    public Material getCornerMaterial()  { return mat("design.corner",  Material.CYAN_STAINED_GLASS_PANE); }

    // ── Botones del menú principal ────────────────────────────────

    public String   getWriteName()     { return str("buttons.write.name",     "&#CC88FF&l✦ Escribir nombre"); }
    public Material getWriteMaterial() { return mat("buttons.write.material", Material.NAME_TAG); }
    public int      getWriteSlot()     { return slot("buttons.write.slot",    13); }
    public List<String> getWriteLore() { return lore("buttons.write.lore"); }

    public String   getRemoveName()     { return str("buttons.remove.name",     "&c&l✖ Quitar disfraz"); }
    public Material getRemoveMaterial() { return mat("buttons.remove.material", Material.BARRIER); }
    public int      getRemoveSlot()     { return slot("buttons.remove.slot",    17); }
    public List<String> getRemoveLore() { return lore("buttons.remove.lore"); }

    public String   getLabelName()     { return str("buttons.label.name",     "&#CC88FF&l✦ Jugadores en línea"); }
    public Material getLabelMaterial() { return mat("buttons.label.material", Material.NETHER_STAR); }
    public int      getLabelSlot()     { return slot("buttons.label.slot",    22); }
    public List<String> getLabelLore() { return lore("buttons.label.lore"); }

    public String   getPrevName()     { return str("buttons.prev-page.name",     "&e&l◄ Página anterior"); }
    public Material getPrevMaterial() { return mat("buttons.prev-page.material", Material.SPECTRAL_ARROW); }
    public int      getPrevSlot()     { return slot("buttons.prev-page.slot",    46); }

    public String   getNextName()     { return str("buttons.next-page.name",     "&e&lPágina siguiente ►"); }
    public Material getNextMaterial() { return mat("buttons.next-page.material", Material.SPECTRAL_ARROW); }
    public int      getNextSlot()     { return slot("buttons.next-page.slot",    52); }

    public Material getPageInfoMaterial() { return mat("buttons.page-info.material", Material.PAPER); }
    public int      getPageInfoSlot()     { return slot("buttons.page-info.slot",    49); }

    // ── Botones del menú de confirmación ─────────────────────────

    public String   getConfirmBtnName()     { return str("confirm-menu.confirm.name",     "&a&l✔ Confirmar disfraz"); }
    public Material getConfirmBtnMaterial() { return mat("confirm-menu.confirm.material", Material.LIME_CONCRETE); }
    public int      getConfirmBtnSlot()     { return slot("confirm-menu.confirm.slot",    49); }

    public String   getRenameName()     { return str("confirm-menu.rename.name",     "&e&l✎ Cambiar nombre"); }
    public Material getRenameMaterial() { return mat("confirm-menu.rename.material", Material.NAME_TAG); }
    public int      getRenameSlot()     { return slot("confirm-menu.rename.slot",    46); }
    public List<String> getRenameLore() { return lore("confirm-menu.rename.lore"); }

    public String   getBackName()     { return str("confirm-menu.back.name",     "&c&l◄ Volver"); }
    public Material getBackMaterial() { return mat("confirm-menu.back.material", Material.ARROW); }
    public int      getBackSlot()     { return slot("confirm-menu.back.slot",    52); }
    public List<String> getBackLore() { return lore("confirm-menu.back.lore"); }

    // ── Glass de rangos (fallback) ────────────────────────────────

    public Material[] getRankGlassFallback() {
        List<String> list = menu.getStringList("rank-glass-fallback");
        if (list.isEmpty()) return defaultRankGlass();
        Material[] result = new Material[list.size()];
        for (int i = 0; i < list.size(); i++) {
            try { result[i] = Material.valueOf(list.get(i).toUpperCase()); }
            catch (IllegalArgumentException e) { result[i] = Material.PURPLE_STAINED_GLASS_PANE; }
        }
        return result;
    }

    // ── Helpers privados ──────────────────────────────────────────

    private String str(String path, String def) {
        return menu.getString(path, def);
    }

    private Material mat(String path, Material def) {
        String s = menu.getString(path, "");
        if (s == null || s.isBlank()) return def;
        try { return Material.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return def; }
    }

    private int slot(String path, int def) {
        int v = menu.getInt(path, def);
        return (v >= 0 && v < 54) ? v : def;
    }

    private List<String> lore(String path) {
        List<String> l = menu.getStringList(path);
        return l != null ? l : new ArrayList<>();
    }

    private static Material[] defaultRankGlass() {
        return new Material[]{
            Material.RED_STAINED_GLASS_PANE,    Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,  Material.CYAN_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE,   Material.WHITE_STAINED_GLASS_PANE,
            Material.BROWN_STAINED_GLASS_PANE,  Material.GRAY_STAINED_GLASS_PANE,
        };
    }
}
