package me.zerith.zerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtiene la skin de un jugador de Minecraft de forma asincrona.
 *
 * Flujo optimizado:
 *  - Si el jugador esta online en este servidor → usamos su UUID directamente (paso 1 omitido)
 *  - Si esta offline → GET api.mojang.com/users/profiles/minecraft/{name} para obtener UUID
 *  - Luego → GET sessionserver.mojang.com/session/minecraft/profile/{uuid}?unsigned=false
 *
 * Incluye cache LRU de hasta 200 entradas para evitar llamadas repetidas.
 * Todos los callbacks se ejecutan en el hilo principal del servidor.
 */
public class SkinFetcher {

    public record SkinData(String value, String signature) {}

    private static final int     CACHE_SIZE = 200;
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile(
            "\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SIG_PATTERN = Pattern.compile(
            "\"signature\"\\s*:\\s*\"([^\"]+)\"");

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

    /** Obtiene la skin por nombre. Primero revisa cache, luego jugadores online, luego API Mojang. */
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

        // Comprobar si el jugador esta online para obtener UUID sin llamada extra a Mojang
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        String knownUuid    = onlinePlayer != null
                ? onlinePlayer.getUniqueId().toString().replace("-", "")
                : null;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String rawUuid = knownUuid;

                // Paso 1: nombre → UUID (solo si no esta online)
                if (rawUuid == null) {
                    String uuidJson = httpGet("https://api.mojang.com/users/profiles/minecraft/" + playerName);
                    if (uuidJson == null || uuidJson.isBlank()) {
                        fail(onError, "Jugador '" + playerName + "' no encontrado en Mojang.");
                        return;
                    }
                    rawUuid = extractFirst(UUID_PATTERN, uuidJson);
                    if (rawUuid == null) {
                        fail(onError, "UUID no encontrado para '" + playerName + "'. Respuesta: "
                                + uuidJson.substring(0, Math.min(80, uuidJson.length())));
                        return;
                    }
                }

                // Paso 2: UUID → perfil con textura
                String profileJson = httpGet(
                        "https://sessionserver.mojang.com/session/minecraft/profile/"
                        + rawUuid + "?unsigned=false");

                if (profileJson == null || profileJson.isBlank()) {
                    fail(onError, "No se pudo obtener el perfil de '" + playerName + "'.");
                    return;
                }

                String value     = extractFirst(VALUE_PATTERN, profileJson);
                String signature = extractFirst(SIG_PATTERN,   profileJson);

                if (value == null) {
                    fail(onError, "Textura no encontrada en el perfil de '" + playerName + "'.");
                    return;
                }

                SkinData data = new SkinData(value, signature != null ? signature : "");
                synchronized (cache) {
                    cache.put(key, data);
                }
                Bukkit.getScheduler().runTask(plugin, () -> onSuccess.accept(data));

            } catch (Exception ex) {
                fail(onError, "Error de red al obtener skin de '" + playerName + "': " + ex.getMessage());
            }
        });
    }

    /** Retorna la SkinData del caché si existe, o null si no está cacheada. Llamado desde hilo principal. */
    public SkinData getCached(String playerName) {
        synchronized (cache) {
            return cache.get(playerName.toLowerCase());
        }
    }

    public void invalidateCache(String playerName) {
        synchronized (cache) {
            cache.remove(playerName.toLowerCase());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    private void fail(Consumer<String> onError, String msg) {
        Bukkit.getScheduler().runTask(plugin, () -> onError.accept(msg));
    }

    /**
     * Realiza un HTTP GET. Retorna el cuerpo solo si la respuesta es 2xx.
     * Para otros codigos retorna null (404, 204) o lanza excepcion (5xx, red).
     */
    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "ZerDisguise/2.1 (+github.com/Zerinho23/ZerDisguise)");
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();

        if (code == 204 || code == 404) {
            conn.disconnect();
            return null;
        }

        if (code == 429) {
            conn.disconnect();
            throw new RuntimeException("Mojang API: demasiadas solicitudes (429). Intentalo en unos segundos.");
        }

        if (code < 200 || code >= 300) {
            String err = readStream(conn.getErrorStream());
            conn.disconnect();
            throw new RuntimeException("HTTP " + code + ": " + (err != null ? err.substring(0, Math.min(120, err.length())) : "sin cuerpo"));
        }

        try {
            return readStream(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private static String readStream(InputStream is) {
        if (is == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Extrae el primer grupo de un patron regex sobre un texto. */
    private static String extractFirst(Pattern p, String text) {
        if (text == null) return null;
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
