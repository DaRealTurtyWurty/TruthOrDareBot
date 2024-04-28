package dev.turtywurty.truthordarebot.data;

import com.google.gson.JsonElement;

public interface JsonSerializable {
    JsonElement toJson();
    void fromJson(JsonElement json);
}
