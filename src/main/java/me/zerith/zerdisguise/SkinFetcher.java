package me.zerith.zerdisguise;

import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Fetches Minecraft player skin data from Mojang's API asynchronously.
 *
 * Flow (name-based, no UUID needed):
 *  1. GET api.mojang.com/users/profiles/minecraft/{name}  -> UUID
 *  2. GET sessionserver.mojang.com/session/minecraft/profile/{uuid}?unsigned=false -> skin
 *
 * Includes an in-memory LRU cache to avoid repeated API calls for the same name.
 * Both callbacks are always invoked on the main server thread.
 */
public class SkinFetcher {

    public record SkinData(String value, String signature) {}

    private static final int CACHE_SIZE = 200;

    private final Map<String, SkinData> cache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SkinData> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private final ZerDisguise plugin;

    public SkinFetcher(ZerDisguise plugin) {
        this.plugin = plugin;
    }

    public void fetchSkin(String playerName,
                          Consumer<SkinData> onSuccess,
                          Consumer<String>   onError) {

        String key = playerName.toLowerCase();
        synchronized (cache) {
            SkinData cached = cache.get(key);
            if (cached != null) {
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(cached));
                return;
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String uuidJson = get("https://api.mojang.com/users/profiles/minecraft/" + playerName);

                if (uuidJson == null || uuidJson.isBlank()) {
                    fail(onError, "Jugador '" + playerName + "' no encontrado en Mojang.");
                    return;
                }

                String rawUuid = extractField(uuidJson, "id");
                if (rawUuid == null) {
                    fail(onError, "UUID no encontrado para '" + playerName + "'.");
                    return;
                }

                String uuid = rawUuid.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5");

                String profileJson = get(
                        "https://sessionserver.mojang.com/session/minecraft/profile/"
                        + uuid.replace("-", "") + "?unsigned=false");

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
                synchronized (cache) {
                    cache.put(key, data);
                }
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(data));

            } catch (Exception ex) {
                fail(onError, "Error de red: " + ex.getMessage());
            }
        });
    }

    public void invalidateCache(String playerName) {
        synchronized (cache) {
            cache.remove(playerName.toLowerCase());
        }
    }

    private void fail(Consumer<String> onError, String msg) {
        Bukkit.getScheduler().runTask(plugin, () -> onError.accept(msg));
    }

    private static String get(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(7000);
        conn.setRequestProperty("User-Agent", "ZerDisguise/2.0");

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

    private static String extractField(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private static String extractTextureField(String json, String field) {
        int idx = json.indexOf("\"textures\"");
        if (idx < 0) return extractField(json, field);
        return extractField(json.substring(idx), field);
    }
}
