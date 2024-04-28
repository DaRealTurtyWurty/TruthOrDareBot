package dev.turtywurty.truthordarebot.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.truthordarebot.TruthOrDareBot;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TODPack implements JsonSerializable {
    public static final TODPack DEFAULT = createDefault();

    private final String name;
    private final Map<String, String> truthMap = new HashMap<>();
    private final Map<String, String> dareMap = new HashMap<>();
    private String description = "";

    public TODPack(String name, String description) {
        this(name);
        this.description = description;
    }

    public TODPack(String name) {
        this.name = name;
    }

    public static TODPack constructFromJson(JsonElement json) {
        var jsonObject = json.getAsJsonObject();
        var name = jsonObject.get("Name").getAsString();

        var pack = new TODPack(name);
        pack.fromJson(json);
        return pack;
    }

    private static TODPack createDefault() {
        var pack = new TODPack("Default", "The default pack for Truth or Dare!");

        try (InputStream truthsStream = TruthOrDareBot.loadResource("truths.txt")) {
            if (truthsStream != null) {
                var reader = new BufferedReader(new InputStreamReader(truthsStream));
                reader.lines().forEach(pack::addTruth);

                TruthOrDareBot.LOGGER.info("Loaded default truths!");
            } else {
                throw new IOException("Failed to load truths.txt!");
            }
        } catch (IOException exception) {
            TruthOrDareBot.LOGGER.error("Failed to load default truths!", exception);
            System.exit(1);
        }

        try (InputStream daresStream = TruthOrDareBot.loadResource("dares.txt")) {
            if (daresStream != null) {
                var reader = new BufferedReader(new InputStreamReader(daresStream));
                reader.lines().forEach(pack::addDare);

                TruthOrDareBot.LOGGER.info("Loaded default dares!");
            } else {
                throw new IOException("Failed to load dares.txt!");
            }
        } catch (IOException exception) {
            TruthOrDareBot.LOGGER.error("Failed to load default dares!", exception);
            System.exit(1);
        }

        return pack;
    }

    private static Pair<String, String> getRandom(Map<String, String> map, List<String> ignore) {
        if (map.isEmpty())
            return Pair.of(null, "No truths or dares found!");

        List<String> keys = map.keySet().stream()
                .filter(key -> !ignore.contains(key))
                .toList();

        if (keys.isEmpty())
            return Pair.of(null, "No truths or dares found!");

        String key = keys.get(TruthOrDareBot.RANDOM.nextInt(keys.size()));
        return Pair.of(key, map.get(key));
    }

    private static String generateId(String question) {
        return Integer.toString(question.hashCode(), 36);
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isCustom() {
        return this != DEFAULT;
    }

    public String addTruth(String truth) {
        String id = generateId(truth);
        this.truthMap.put(id, truth);
        return id;
    }

    public String addDare(String dare) {
        String id = generateId(dare);
        this.dareMap.put(id, dare);
        return id;
    }

    public String getTruth(String id) {
        return this.truthMap.get(id);
    }

    public String getDare(String id) {
        return this.dareMap.get(id);
    }

    public void removeTruth(String id) {
        this.truthMap.remove(id);
    }

    public void removeDare(String id) {
        this.dareMap.remove(id);
    }

    public Map<String, String> getTruthMap() {
        return Map.copyOf(this.truthMap);
    }

    public Map<String, String> getDareMap() {
        return Map.copyOf(this.dareMap);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public JsonElement toJson() {
        var json = new JsonObject();

        json.addProperty("Name", this.name);
        json.addProperty("Description", this.description);

        var truths = new JsonObject();
        this.truthMap.forEach(truths::addProperty);
        json.add("Truths", truths);

        var dares = new JsonObject();
        this.dareMap.forEach(dares::addProperty);
        json.add("Dares", dares);

        return json;
    }

    @Override
    public void fromJson(JsonElement json) {
        var jsonObject = json.getAsJsonObject();

        this.description = jsonObject.get("Description").getAsString();

        var truths = jsonObject.getAsJsonObject("Truths");
        truths.entrySet().forEach(entry -> this.truthMap.put(entry.getKey(), entry.getValue().getAsString()));

        var dares = jsonObject.getAsJsonObject("Dares");
        dares.entrySet().forEach(entry -> this.dareMap.put(entry.getKey(), entry.getValue().getAsString()));
    }

    public Pair<String, String> getRandomTruth(List<String> usedTruths) {
        return getRandom(this.truthMap, usedTruths);
    }

    public Pair<String, String> getRandomDare(List<String> usedDares) {
        return getRandom(this.dareMap, usedDares);
    }
}
