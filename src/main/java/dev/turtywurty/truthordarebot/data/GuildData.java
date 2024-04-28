package dev.turtywurty.truthordarebot.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuildData implements JsonSerializable {
    public static final GuildData NO_DATA = new GuildData(-1L);

    private final long guildId;
    private final List<String> usedTruths = new ArrayList<>();
    private final List<String> usedDares = new ArrayList<>();
    private final List<TODPack> packs = new ArrayList<>();

    private TODPack currentPack = TODPack.DEFAULT;

    public GuildData(long guildId) {
        this.guildId = guildId;
        this.packs.add(TODPack.DEFAULT);
    }

    public boolean hasPack(String name) {
        return this.packs.stream().anyMatch(pack -> pack.getName().equals(name));
    }

    public boolean addPack(TODPack pack) {
        if (hasPack(pack.getName()))
            return false;

        return this.packs.add(pack);
    }

    public boolean removePack(String name) {
        if (name.equals(TODPack.DEFAULT.getName()))
            return false;

        boolean removed = this.packs.removeIf(pack -> pack.getName().equals(name));
        if (removed && this.currentPack.getName().equals(name)) {
            this.currentPack = TODPack.DEFAULT;
            this.usedTruths.clear();
            this.usedDares.clear();
        }

        return removed;
    }

    public TODPack getPack(String packName) {
        return this.packs.stream().filter(pack -> pack.getName().equals(packName)).findFirst().orElse(null);
    }

    public boolean usePack(String packName) {
        TODPack pack = getPack(packName);
        if (pack == null)
            return false;

        if(this.currentPack != pack) {
            this.usedTruths.clear();
            this.usedDares.clear();
        }

        this.currentPack = pack;
        return true;
    }

    public List<String> getUsedTruths() {
        return this.usedTruths;
    }

    public List<String> getUsedDares() {
        return this.usedDares;
    }

    public List<TODPack> getPacks() {
        return this.packs;
    }

    public TODPack getCurrentPack() {
        return this.currentPack;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public boolean isNoData() {
        return this == NO_DATA;
    }

    public Response getResponse(QuestionType questionType) {
        return switch (questionType) {
            case TRUTH -> {
                Pair<String, String> truth = this.currentPack.getRandomTruth(getUsedTruths());
                yield new Response(truth.getLeft(), truth.getRight(), QuestionType.TRUTH);
            }
            case DARE -> {
                Pair<String, String> dare = this.currentPack.getRandomDare(getUsedDares());
                yield new Response(dare.getLeft(), dare.getRight(), QuestionType.DARE);
            }
            case RANDOM -> Math.random() < 0.5 ? getResponse(QuestionType.TRUTH) : getResponse(QuestionType.DARE);
        };
    }

    @Override
    public JsonElement toJson() {
        var json = new JsonObject();

        json.addProperty("GuildID", this.guildId);

        var usedTruths = new JsonArray();
        this.usedTruths.forEach(usedTruths::add);
        json.add("UsedTruths", usedTruths);

        var usedDares = new JsonArray();
        this.usedDares.forEach(usedDares::add);
        json.add("UsedDares", usedDares);

        var packs = new JsonArray();
        this.packs.forEach(pack -> {
            if (pack == TODPack.DEFAULT)
                return;

            packs.add(pack.toJson());
        });
        json.add("Packs", packs);

        json.addProperty("CurrentPack", this.currentPack.getName());

        return json;
    }

    @Override
    public void fromJson(JsonElement json) {
        var jsonObject = json.getAsJsonObject();

        var usedTruths = jsonObject.getAsJsonArray("UsedTruths");
        usedTruths.forEach(truth -> this.usedTruths.add(truth.getAsString()));

        var usedDares = jsonObject.getAsJsonArray("UsedDares");
        usedDares.forEach(dare -> this.usedDares.add(dare.getAsString()));

        var packs = jsonObject.getAsJsonArray("Packs");
        packs.forEach(pack -> {
            var packObject = pack.getAsJsonObject();

            String name = packObject.get("Name").getAsString();
            if (name.equals(TODPack.DEFAULT.getName())) {
                return;
            }

            var newPack = new TODPack(name);
            newPack.fromJson(pack);
            this.packs.add(newPack);
        });

        Optional<TODPack> foundOpt = this.packs.stream()
                .filter(pack -> pack.getName().equals(jsonObject.get("CurrentPack").getAsString()))
                .findFirst();

        if (foundOpt.isPresent()) {
            TODPack pack = foundOpt.get();
            if (this.currentPack != TODPack.DEFAULT && pack == TODPack.DEFAULT) {
                this.usedTruths.clear();
                this.usedDares.clear();
            }

            this.currentPack = pack;
        } else if (this.currentPack != TODPack.DEFAULT) {
            this.usedTruths.clear();
            this.usedDares.clear();
            this.currentPack = TODPack.DEFAULT;
        }
    }

    public record Response(String id, String question, QuestionType type) {
    }
}
