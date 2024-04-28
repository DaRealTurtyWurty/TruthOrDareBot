package dev.turtywurty.truthordarebot;

import com.google.gson.JsonElement;
import dev.turtywurty.truthordarebot.data.GuildData;
import dev.turtywurty.truthordarebot.data.QuestionType;
import dev.turtywurty.truthordarebot.data.TODPack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class CommandListener extends ListenerAdapter {
    private static final List<Button> BUTTONS = List.of(
            Button.primary("truth", "Truth"),
            Button.primary("dare", "Dare"),
            Button.primary("random", "Random")
    );

    private static GuildData getGuildData(long guildId) {
        if (guildId == -1L) {
            return GuildData.NO_DATA;
        }

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

    private static GuildData.Response getTruthOrDare(long guildId, QuestionType type) {
        GuildData data = getGuildData(guildId);
        GuildData.Response response = data.getResponse(type);
        if (data.isNoData() || response.id() == null)
            return response;

        switch (response.type()) {
            case TRUTH -> data.getUsedTruths().add(response.id());
            case DARE -> data.getUsedDares().add(response.id());
            default -> throw new UnsupportedOperationException("Unknown QuestionType: " + response.type());
        }

        updateGuildData(data);

        return response;
    }

    private static void updateGuildData(GuildData data) {
        try {
            Path path = Path.of("data", data.getGuildId() + ".json");
            Files.writeString(path, TruthOrDareBot.GSON.toJson(data.toJson()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            TruthOrDareBot.LOGGER.error("Failed to save GuildData for guild with ID: {}", data.getGuildId(), exception);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        boolean isGuild = event.isFromGuild();
        switch (event.getName()) {
            case "truth", "dare", "random" -> {
                QuestionType type = switch (event.getName()) {
                    case "truth" -> QuestionType.TRUTH;
                    case "dare" -> QuestionType.DARE;
                    default -> QuestionType.RANDOM;
                };

                GuildData.Response response = getTruthOrDare(isGuild ? event.getGuild().getIdLong() : -1L, type);
                if (response.id() == null) {
                    event.reply("❌ No " + response.type().getName().toLowerCase(Locale.ROOT) + "s found!").setEphemeral(true).queue();
                    return;
                }

                var embed = new EmbedBuilder()
                        .setTitle(response.type().getName() + ": " + response.question())
                        .setDescription("ID: " + response.id())
                        .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(0x00FF00)
                        .build();

                event.replyEmbeds(embed).addActionRow(BUTTONS).queue();
            }
            case "pack" -> {
                if (!isGuild) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                String subcommand = event.getSubcommandName();
                if (subcommand == null) {
                    event.reply("❌ You must specify a subcommand!").setEphemeral(true).queue();
                    return;
                }

                switch (subcommand) {
                    case "use" -> {
                        String packName = event.getOption("name", null, OptionMapping::getAsString);
                        if (packName == null) {
                            event.reply("❌ You must specify a pack name!").setEphemeral(true).queue();
                            return;
                        }

                        GuildData data = getGuildData(event.getGuild().getIdLong());
                        if (data.usePack(packName)) {
                            event.reply("✅ Successfully set pack to: " + packName).queue();
                            updateGuildData(data);
                        } else {
                            event.reply("❌ Pack does not exist: " + packName).setEphemeral(true).queue();
                        }
                    }
                    case "add" -> {
                        String packName = event.getOption("name", null, OptionMapping::getAsString);
                        if (packName == null) {
                            event.reply("❌ You must specify a pack name!").setEphemeral(true).queue();
                            return;
                        }

                        String description = event.getOption("description", "", OptionMapping::getAsString);
                        GuildData data = getGuildData(event.getGuild().getIdLong());
                        if (data.addPack(new TODPack(packName, description))) {
                            event.reply("✅ Successfully added pack: " + packName).queue();
                            updateGuildData(data);
                        } else {
                            event.reply("❌ Pack already exists: " + packName).setEphemeral(true).queue();
                        }
                    }
                    case "remove" -> {
                        String packName = event.getOption("name", null, OptionMapping::getAsString);
                        if (packName == null) {
                            event.reply("❌ You must specify a pack name!").setEphemeral(true).queue();
                            return;
                        }

                        if (packName.equals(TODPack.DEFAULT.getName())) {
                            event.reply("❌ You cannot remove the default pack!").setEphemeral(true).queue();
                            return;
                        }

                        GuildData data = getGuildData(event.getGuild().getIdLong());
                        if (data.removePack(packName)) {
                            event.reply("✅ Successfully removed pack: " + packName).queue();
                            updateGuildData(data);
                        } else {
                            event.reply("❌ Pack does not exist: " + packName).setEphemeral(true).queue();
                        }
                    }
                    case "list" -> {
                        GuildData data = getGuildData(event.getGuild().getIdLong());
                        List<TODPack> packs = data.getPacks();
                        var embed = new EmbedBuilder()
                                .setTitle("Packs")
                                .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .setColor(0x00FF00);

                        if (packs.isEmpty()) {
                            embed.setDescription("No packs found!");
                        } else {
                            for (TODPack pack : packs) {
                                String name = pack.getName();
                                String description = pack.getDescription();

                                embed.appendDescription("**" + name + "**: " + description + "\n");
                            }

                            embed.getDescriptionBuilder().setLength(embed.getDescriptionBuilder().length() - 1);
                        }

                        event.replyEmbeds(embed.build()).queue();
                    }
                    default -> event.reply("❌ Unknown subcommand: " + subcommand).setEphemeral(true).queue();
                }
            }
            case "edit-pack" -> {
                if (!isGuild) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                String subcommand = event.getSubcommandName();
                if (subcommand == null) {
                    event.reply("❌ You must specify a subcommand!").setEphemeral(true).queue();
                    return;
                }

                String packName = event.getOption("name", null, OptionMapping::getAsString);
                if (packName == null) {
                    event.reply("❌ You must specify a pack name!").setEphemeral(true).queue();
                    return;
                }

                if (packName.equals(TODPack.DEFAULT.getName())) {
                    event.reply("❌ You cannot edit the default pack!").setEphemeral(true).queue();
                    return;
                }

                GuildData data = getGuildData(event.getGuild().getIdLong());
                TODPack pack = data.getPack(packName);
                if (pack == null) {
                    event.reply("❌ Pack does not exist: " + packName).setEphemeral(true).queue();
                    return;
                }

                boolean updated = false;
                switch (subcommand) {
                    case "description" -> {
                        String description = event.getOption("description", null, OptionMapping::getAsString);
                        if (description == null) {
                            event.reply("❌ You must specify a description!").setEphemeral(true).queue();
                            return;
                        }

                        pack.setDescription(description);
                        event.reply("✅ Successfully updated description for pack: " + packName).queue();

                        updated = true;
                    }
                    case "add-truth" -> {
                        String truth = event.getOption("truth", null, OptionMapping::getAsString);
                        if (truth == null) {
                            event.reply("❌ You must specify a truth!").setEphemeral(true).queue();
                            return;
                        }

                        String id = pack.addTruth(truth);
                        event.reply("✅ Successfully added truth to pack: **`" + packName + "`**\nID: " + id).queue();

                        updated = true;
                    }
                    case "add-dare" -> {
                        String dare = event.getOption("dare", null, OptionMapping::getAsString);
                        if (dare == null) {
                            event.reply("❌ You must specify a dare!").setEphemeral(true).queue();
                            return;
                        }

                        String id = pack.addDare(dare);
                        event.reply("✅ Successfully added dare to pack: **`" + packName + "`**\nID: " + id).queue();

                        updated = true;
                    }
                    case "remove-truth" -> {
                        String id = event.getOption("id", null, OptionMapping::getAsString);
                        if (id == null) {
                            event.reply("❌ You must specify an ID!").setEphemeral(true).queue();
                            return;
                        }

                        pack.removeTruth(id);
                        event.reply("✅ Successfully removed truth from pack: **`" + packName + "`**\nID: " + id).queue();

                        updated = true;
                    }
                    case "remove-dare" -> {
                        String id = event.getOption("id", null, OptionMapping::getAsString);
                        if (id == null) {
                            event.reply("❌ You must specify an ID!").setEphemeral(true).queue();
                            return;
                        }

                        pack.removeDare(id);
                        event.reply("✅ Successfully removed dare from pack: **`" + packName + "`**\nID: " + id).queue();

                        updated = true;
                    }
                    default -> event.reply("❌ Unknown subcommand: " + subcommand).setEphemeral(true).queue();
                }

                if (updated) {
                    updateGuildData(data);
                }
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();

        switch (event.getComponentId()) {
            case "truth", "dare", "random" -> {
                QuestionType type = switch (event.getComponentId()) {
                    case "truth" -> QuestionType.TRUTH;
                    case "dare" -> QuestionType.DARE;
                    default -> QuestionType.RANDOM;
                };

                GuildData.Response response = getTruthOrDare(event.getGuild().getIdLong(), type);
                if (response.id() == null) {
                    event.getHook().sendMessage("❌ No " + response.type().getName().toLowerCase(Locale.ROOT) + "s found!").setEphemeral(true).queue();
                    return;
                }

                var embed = new EmbedBuilder()
                        .setTitle(response.type().getName() + ": " + response.question())
                        .setDescription("ID: " + response.id())
                        .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(0x00FF00)
                        .build();

                event.getHook().sendMessageEmbeds(embed).addActionRow(BUTTONS).queue();
            }
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.replyChoices().queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (event.getOption("name", null, OptionMapping::getAsString).equals(TODPack.DEFAULT.getName())) {
            event.replyChoices().queue();
            return;
        }

        if ("pack".equals(event.getName()) && event.getFocusedOption().getName().equals("name")) {
            GuildData data = getGuildData(event.getGuild().getIdLong());
            List<TODPack> packs = data.getPacks();
            if ("remove".equals(subcommand))
                event.replyChoiceStrings(packs.stream()
                        .filter(TODPack::isCustom)
                        .map(TODPack::getName)
                        .limit(25)
                        .toList()
                ).queue();
            else if ("use".equals(subcommand))
                event.replyChoiceStrings(packs.stream()
                        .map(TODPack::getName)
                        .limit(25)
                        .toList()
                ).queue();
        } else if ("edit-pack".equals(event.getName())) {
            if (event.getFocusedOption().getName().equals("name")) {
                GuildData data = getGuildData(event.getGuild().getIdLong());
                List<TODPack> packs = data.getPacks();
                event.replyChoiceStrings(packs.stream().filter(TODPack::isCustom).map(TODPack::getName).limit(25).toList()).queue();
            } else if (event.getFocusedOption().getName().equals("id")) {
                String packName = event.getOption("name", null, OptionMapping::getAsString);
                if (packName == null) {
                    event.replyChoices().queue();
                    return;
                }

                GuildData data = getGuildData(event.getGuild().getIdLong());
                TODPack pack = data.getPack(packName);
                if (pack == null) {
                    event.replyChoices().queue();
                    return;
                }

                List<String> ids = switch (subcommand) {
                    case "remove-truth" -> pack.getTruthMap().keySet().stream().limit(25).toList();
                    case "remove-dare" -> pack.getDareMap().keySet().stream().limit(25).toList();
                    case null, default -> List.of();
                };

                event.replyChoiceStrings(ids).queue();
            }
        }
    }
}
