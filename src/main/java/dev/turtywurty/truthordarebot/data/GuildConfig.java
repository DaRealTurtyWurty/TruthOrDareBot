package dev.turtywurty.truthordarebot.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.truthordarebot.CommandListener;

import java.util.ArrayList;
import java.util.List;

public class GuildConfig implements JsonSerializable {
    private final List<Long> whitelistedChannels = new ArrayList<>();
    private final List<Long> blacklistedChannels = new ArrayList<>();
    private final List<Long> whitelistedRoles = new ArrayList<>();
    private final List<Long> blacklistedRoles = new ArrayList<>();

    public String getWhitelistedChannelsAsString() {
        return this.whitelistedChannels.stream().map(id -> "<#" + id + ">").reduce((a, b) -> a + ", " + b).orElse("None");
    }

    public String getBlacklistedChannelsAsString() {
        return this.blacklistedChannels.stream().map(id -> "<#" + id + ">").reduce((a, b) -> a + ", " + b).orElse("None");
    }

    public String getWhitelistedRolesAsString() {
        return this.whitelistedRoles.stream().map(id -> "<@&" + id + ">").reduce((a, b) -> a + ", " + b).orElse("None");
    }

    public String getBlacklistedRolesAsString() {
        return this.blacklistedRoles.stream().map(id -> "<@&" + id + ">").reduce((a, b) -> a + ", " + b).orElse("None");
    }

    public List<Long> getWhitelistedChannels() {
        return List.copyOf(this.whitelistedChannels);
    }

    public List<Long> getBlacklistedChannels() {
        return List.copyOf(this.blacklistedChannels);
    }

    public List<Long> getWhitelistedRoles() {
        return List.copyOf(this.whitelistedRoles);
    }

    public List<Long> getBlacklistedRoles() {
        return List.copyOf(this.blacklistedRoles);
    }

    public boolean isChannelAllowed(long id) {
        return !this.blacklistedChannels.contains(id) && (this.whitelistedChannels.isEmpty() || this.whitelistedChannels.contains(id));
    }

    public boolean isRoleAllowed(long id) {
        return !this.blacklistedRoles.contains(id) && (this.whitelistedRoles.isEmpty() || this.whitelistedRoles.contains(id));
    }

    public void addWhitelistedChannel(long id) {
        this.whitelistedChannels.add(id);
    }

    public void addBlacklistedChannel(long id) {
        this.blacklistedChannels.add(id);
    }

    public void addWhitelistedRole(long id) {
        this.whitelistedRoles.add(id);
    }

    public void addBlacklistedRole(long id) {
        this.blacklistedRoles.add(id);
    }

    public void removeWhitelistedChannel(long id) {
        this.whitelistedChannels.remove(id);
    }

    public void removeBlacklistedChannel(long id) {
        this.blacklistedChannels.remove(id);
    }

    public void removeWhitelistedRole(long id) {
        this.whitelistedRoles.remove(id);
    }

    public void removeBlacklistedRole(long id) {
        this.blacklistedRoles.remove(id);
    }

    @Override
    public JsonElement toJson() {
        var json = new JsonObject();

        var whitelistedChannels = new JsonArray();
        this.whitelistedChannels.forEach(whitelistedChannels::add);
        json.add("WhitelistedChannels", whitelistedChannels);

        var blacklistedChannels = new JsonArray();
        this.blacklistedChannels.forEach(blacklistedChannels::add);
        json.add("BlacklistedChannels", blacklistedChannels);

        var whitelistedRoles = new JsonArray();
        this.whitelistedRoles.forEach(whitelistedRoles::add);
        json.add("WhitelistedRoles", whitelistedRoles);

        var blacklistedRoles = new JsonArray();
        this.blacklistedRoles.forEach(blacklistedRoles::add);
        json.add("BlacklistedRoles", blacklistedRoles);

        return json;
    }

    @Override
    public void fromJson(JsonElement json) {
        var jsonObject = json.getAsJsonObject();

        var whitelistedChannels = jsonObject.getAsJsonArray("WhitelistedChannels");
        whitelistedChannels.forEach(channel -> this.whitelistedChannels.add(channel.getAsLong()));

        var blacklistedChannels = jsonObject.getAsJsonArray("BlacklistedChannels");
        blacklistedChannels.forEach(channel -> this.blacklistedChannels.add(channel.getAsLong()));

        var whitelistedRoles = jsonObject.getAsJsonArray("WhitelistedRoles");
        whitelistedRoles.forEach(role -> this.whitelistedRoles.add(role.getAsLong()));

        var blacklistedRoles = jsonObject.getAsJsonArray("BlacklistedRoles");
        blacklistedRoles.forEach(role -> this.blacklistedRoles.add(role.getAsLong()));
    }

    public void reset(CommandListener.ResetType type) {
        switch (type) {
            case CHANNEL_WHITELIST -> this.whitelistedChannels.clear();
            case CHANNEL_BLACKLIST -> this.blacklistedChannels.clear();
            case ROLE_WHITELIST -> this.whitelistedRoles.clear();
            case ROLE_BLACKLIST -> this.blacklistedRoles.clear();
            case ALL -> {
                reset(CommandListener.ResetType.CHANNEL_WHITELIST);
                reset(CommandListener.ResetType.CHANNEL_BLACKLIST);
                reset(CommandListener.ResetType.ROLE_WHITELIST);
                reset(CommandListener.ResetType.ROLE_BLACKLIST);
            }
        }
    }
}
