package net.epconsortium.cryptomarket.finances;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.epconsortium.cryptomarket.CryptoMarket;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CachedRates {

    private final CryptoMarket plugin;
    private final File cacheFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();
    private static final Type MAP_TYPE = TypeToken.getParameterized(Map.class, LocalDate.class, CachedExchangeRate.class).getType();


    public CachedRates(CryptoMarket plugin) {
        this.plugin = plugin;
        cacheFolder = new File(plugin.getDataFolder(), "cache");
        //noinspection ResultOfMethodCallIgnored
        cacheFolder.mkdir();
    }

    public boolean isCached(String coin) {
        return getCacheFile(coin).exists();
    }

    public Map<LocalDate, CachedExchangeRate> getRates(String coin) {
        if (isCached(coin)) {
            try (JsonReader reader = new JsonReader(new FileReader(getCacheFile(coin)))) {
                Map<LocalDate, CachedExchangeRate> map = gson.fromJson(reader, MAP_TYPE);
                if (map != null) {
                    return map;
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "There was an error while reading the cache", ex);
            }
        }
        return new HashMap<>();
    }

    public @Nullable CachedExchangeRate getCachedExchangeRate(String coin, LocalDate date) {
        return getRates(coin).get(date);
    }

    public void saveRates(String coin, Map<LocalDate, BigDecimal> rates) {
        Map<LocalDate, CachedExchangeRate> currentCache = getRates(coin);
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<LocalDate, BigDecimal> entry : rates.entrySet()) {
            currentCache.put(entry.getKey(), new CachedExchangeRate(entry.getValue(), now));
        }
        try (FileWriter fw = new FileWriter(getCacheFile(coin))) {
            gson.toJson(currentCache, MAP_TYPE, fw);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "There was an error while saving the cache", ex);
        }
    }

    private File getCacheFile(String coin) {
        return new File(cacheFolder, String.format("%s.json", coin.toLowerCase()));
    }

    public static class CachedExchangeRate {

        private final BigDecimal value;
        private final LocalDateTime lastUpdated;

        public CachedExchangeRate(BigDecimal value, LocalDateTime lastUpdated) {
            this.value = value;
            this.lastUpdated = lastUpdated;
        }

        public boolean isFresh(int updateInterval) {
            return lastUpdated.plusMinutes(updateInterval).isAfter(LocalDateTime.now());
        }

        public BigDecimal getCoinValue() {
            return value;
        }
    }

    static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            if (localDateTime == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDateTime.toString());
            }
        }

        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return LocalDateTime.parse(jsonReader.nextString());
        }
    }

    static class LocalDateAdapter extends TypeAdapter<LocalDate> {

        @Override
        public void write(JsonWriter jsonWriter, LocalDate localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
            } else {
                jsonWriter.value(localDate.toString());
            }
        }

        @Override
        public LocalDate read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return LocalDate.parse(jsonReader.nextString());
        }
    }
}
