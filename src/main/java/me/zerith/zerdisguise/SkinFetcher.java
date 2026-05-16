package me.zerith.zerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Fetches Minecraft player skin data from Mojang's API asynchronously.
 *
 * Flow:
 *  1. GET api.mojang.com/users/profiles/minecraft/{name}  → UUID
 *  2. GET sessionserver.mojang.com/session/minecraft/profile/{uuid}?unsigned=false
 *     → skin value (Base64) + signature
 *
 * Both callbacks are always invoked on the main server thread.
 */
public class SkinFetcher {

    public record SkinData(String value, String signature) {}

    private final ZerDisguise plugin;

    public SkinFetcher(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    /**
     * Asynchronously fetches skin for {@code playerName}.
     *
     * @param playerName Minecraft username to look up
     * @param onSuccess  called on main thread with SkinData
     * @param onError    called on main thread with an error message
     */
    public void fetchSkin(String playerName,
                          Consumer<SkinData> onSuccess,
                          Consumer<String>   onError) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // ── Step 1: username → UUID ───────────────────────────────
                String uuidJson = get(
                        "https://api.mojang.com/users/profiles/minecraft/" + playerName);

                if (uuidJson == null || uuidJson.isBlank()) {
                    fail(onError, "Jugador '" + playerName + "' no encontrado en Mojang.");
                    return;
                }
                String rawUuid = extractField(uuidJson, "id");
                if (rawUuid == null) {
                    fail(onError, "UUID no encontrado para '" + playerName + "'.");
                    return;
                }
                // Insert dashes: 8-4-4-4-12
                String uuid = rawUuid.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5");

                // ── Step 2: UUID → skin textures ──────────────────────────
                String profileJson = get(
                        "https://sessionserver.mojang.com/session/minecraft/profile/"
                        + uuid + "?unsigned=false");

                if (profileJson == null || profileJson.isBlank()) {
                    fail(onError, "No se pudo obtener el perfil de '" + playerName + "'.");
                    return;
                }

                String value     = extractTextureField(profileJson, "value");
                String signature = extractTextureField(profileJson, "signature");

                if (value == null) {
                    fail(onError, "No se pudo extraer la textura del perfil.");
                    return;
                }

                SkinData data = new SkinData(value, signature != null ? signature : "");
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(data));

            } catch (Exception ex) {
                fail(onError, "Error de red: " + ex.getMessage());
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fail(Consumer<String> onError, String msg) {
        Bukkit.getScheduler().runTask(plugin, () -> onError.accept(msg));
    }

    /** Performs a simple GET request and returns the body as a String. Returns null on 404/204. */
    private static String get(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);
        conn.setRequestProperty("User-Agent", "ZerDisguise/1.0");

        int code = conn.getResponseCode();
        if (code == 204 || code == 404) { conn.disconnect(); return null; }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    /** Extracts {"key":"value"} — naive but sufficient for Mojang's flat JSON. */
    private static String extractField(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    /**
     * Finds the first occurrence of the "textures" property block and extracts
     * the given field from it.
     */
    private static String extractTextureField(String json, String field) {
        int idx = json.indexOf("\"textures\"");
        if (idx < 0) return null;
        return extractField(json.substring(idx), field);
    }
}
