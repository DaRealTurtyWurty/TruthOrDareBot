package dev.turtywurty.truthordarebot;

import com.google.gson.JsonElement;
import dev.turtywurty.truthordarebot.data.GuildData;
import dev.turtywurty.truthordarebot.data.QuestionType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataHandler {
    public static GuildData getGuildData(long guildId) {
        if (guildId == -1L)
            return GuildData.NO_DATA;

        try {
            Path path = Path.of("data", guildId + ".json");
            Files.createDirectories(path.getParent());
            var data = new GuildData(guildId);
            if (Files.notExists(path)) {
                Files.writeString(path, TruthOrDareBot.GSON.toJson(data.toJson()), StandardCharsets.UTF_8);
            } else {
                data.fromJson(TruthOrDareBot.GSON.fromJson(Files.readString(path), JsonElement.class));
            }

            return data;
        } catch (IOException exception) {
            TruthOrDareBot.LOGGER.error("Failed to get GuildData for guild with ID: {}", guildId, exception);
            return GuildData.NO_DATA;
        }
    }

    public static GuildData.Response getTruthOrDare(long guildId, QuestionType type) {
        GuildData data = getGuildData(guildId);
        GuildData.Response response = data.getResponse(type);
        if (data.isNoData() || response.id() == null)
            return response;

        switch (response.type()) {
            case TRUTH -> data.getUsedTruths().add(response.id());
            case DARE -> data.getUsedDares().add(response.id());
            default -> throw new UnsupportedOperationException("Unknown QuestionType: " + response.type());
        }

        saveGuildData(data);

        return response;
    }

    public static void saveGuildData(GuildData data) {
        try {
            Path path = Path.of("data", data.getGuildId() + ".json");
            Files.writeString(path, TruthOrDareBot.GSON.toJson(data.toJson()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            TruthOrDareBot.LOGGER.error("Failed to save GuildData for guild with ID: {}", data.getGuildId(), exception);
        }
    }
}
