package dev.turtywurty.truthordarebot;

import dev.turtywurty.truthordarebot.data.GuildConfig;
import dev.turtywurty.truthordarebot.data.GuildData;
import dev.turtywurty.truthordarebot.data.QuestionType;
import dev.turtywurty.truthordarebot.data.TODPack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandListener extends ListenerAdapter {
    private static final List<Button> BUTTONS = List.of(
            Button.primary("truth", "Truth"),
            Button.primary("dare", "Dare"),
            Button.primary("random", "Random")
    );

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

                if(isGuild) {
                    GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                    GuildConfig config = data.getConfig();
                    if(!config.isMemberAllowed(event.getMember())) {
                        event.reply("❌ You are not allowed to use this command!").setEphemeral(true).queue();
                        return;
                    }

                    if (!config.isChannelAllowed(event.getChannel().getIdLong())) {
                        event.reply("❌ You are not allowed to use this command in this channel!").setEphemeral(true).queue();
                        return;
                    }
                }

                GuildData.Response response = DataHandler.getTruthOrDare(isGuild ? event.getGuild().getIdLong() : -1L, type);
                if (response.id() == null) {
                    event.reply("❌ No " + response.type().getName().toLowerCase(Locale.ROOT) + "s found!").setEphemeral(true).queue();
                    return;
                }

                var embed = new EmbedBuilder()
                        .setTitle(response.type().getName() + ": " + response.question())
                        .setDescription("ID: " + response.id())
                        .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(response.type() == QuestionType.TRUTH ? 0x0000FF : 0xFF0000)
                        .build();

                event.replyEmbeds(embed).addActionRow(BUTTONS).queue();
            }
            case "pack" -> {
                Member member = event.getMember();
                if (!isGuild || member == null) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                String subcommand = event.getSubcommandName();
                if (subcommand == null) {
                    event.reply("❌ You must specify a subcommand!").setEphemeral(true).queue();
                    return;
                }

                if (!member.hasPermission(Permission.MANAGE_SERVER) && !"list".equals(subcommand)) {
                    event.reply("❌ You need the `MANAGE_SERVER` permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                switch (subcommand) {
                    case "view" -> {
                        String type = event.getOption("type", "truths", OptionMapping::getAsString);
                        if (!type.equals("truths") && !type.equals("dares")) {
                            event.reply("❌ You must specify a type (truths/dares)!").setEphemeral(true).queue();
                            return;
                        }

                        String packName = event.getOption("name", null, OptionMapping::getAsString);
                        if (packName == null) {
                            event.reply("❌ You must specify a pack name!").setEphemeral(true).queue();
                            return;
                        }

                        if (packName.equals(TODPack.DEFAULT.getName())) {
                            event.reply("❌ You cannot view the default pack!").setEphemeral(true).queue();
                            return;
                        }

                        GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                        TODPack pack = data.getPack(packName);
                        if (pack == null) {
                            event.reply("❌ Pack does not exist: " + packName).setEphemeral(true).queue();
                            return;
                        }

                        var embed = new EmbedBuilder()
                                .setTitle(type.substring(0, 1).toUpperCase() + type.substring(1) + " for pack: " + pack.getName())
                                .setDescription(pack.getDescription())
                                .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                                .setTimestamp(Instant.now())
                                .setColor(0x00FF00);

                        List<String> items = type.equals("truths") ?
                                new ArrayList<>(pack.getTruthMap().values()) :
                                new ArrayList<>(pack.getDareMap().values());
                        var paginatableEmbed = new PaginatableEmbed(event.getJDA(), 10, embed, items.toArray(new String[0]));
                        paginatableEmbed.respondToEvent(event);
                    }
                    case "use" -> {
                        String packName = event.getOption("name", null, OptionMapping::getAsString);
                        if (packName == null) {
                            event.reply("❌ You must specify a pack name!").setEphemeral(true).queue();
                            return;
                        }

                        GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                        if (data.usePack(packName)) {
                            event.reply("✅ Successfully set pack to: " + packName).queue();
                            DataHandler.saveGuildData(data);
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
                        GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                        if (data.addPack(new TODPack(packName, description))) {
                            event.reply("✅ Successfully added pack: " + packName).queue();
                            DataHandler.saveGuildData(data);
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

                        GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                        if (data.removePack(packName)) {
                            event.reply("✅ Successfully removed pack: " + packName).queue();
                            DataHandler.saveGuildData(data);
                        } else {
                            event.reply("❌ Pack does not exist: " + packName).setEphemeral(true).queue();
                        }
                    }
                    case "list" -> {
                        GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
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
                Member member = event.getMember();
                if (!isGuild || member == null) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                if (!member.hasPermission(Permission.MANAGE_SERVER)) {
                    event.reply("❌ You need the `MANAGE_SERVER` permission to use this command!").setEphemeral(true).queue();
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

                GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
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
                    DataHandler.saveGuildData(data);
                }
            }
            case "add-config" -> {
                Member member = event.getMember();
                if (!isGuild || member == null) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                if (!member.hasPermission(Permission.MANAGE_SERVER)) {
                    event.reply("❌ You need the `MANAGE_SERVER` permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                String subcommand = event.getSubcommandName();
                if (subcommand == null) {
                    event.reply("❌ You must specify a subcommand!").setEphemeral(true).queue();
                    return;
                }

                GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                GuildConfig config = data.getConfig();
                boolean updated = false;
                switch (subcommand) {
                    case "whitelist-channel" -> {
                        GuildChannelUnion channel = event.getOption("channel", null, OptionMapping::getAsChannel);
                        if (channel == null) {
                            event.reply("❌ You must specify a channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (!channel.getType().isMessage()) {
                            event.reply("❌ You cannot whitelist a category or voice channel!").setEphemeral(true).queue();
                            return;
                        }

                        GuildMessageChannel messageChannel = channel.asGuildMessageChannel();
                        if (!messageChannel.canTalk(member)) {
                            event.reply("❌ You do not have permission to whitelist this channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (config.getWhitelistedChannels().contains(channel.getIdLong())) {
                            event.reply("❌ Channel is already whitelisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeBlacklistedChannel(channel.getIdLong());
                        config.addWhitelistedChannel(channel.getIdLong());

                        event.reply("✅ Successfully whitelisted channel: " + channel.getAsMention()).queue();

                        updated = true;
                    }
                    case "blacklist-channel" -> {
                        GuildChannelUnion channel = event.getOption("channel", null, OptionMapping::getAsChannel);
                        if (channel == null) {
                            event.reply("❌ You must specify a channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (!channel.getType().isMessage()) {
                            event.reply("❌ You cannot blacklist a category or voice channel!").setEphemeral(true).queue();
                            return;
                        }

                        GuildMessageChannel messageChannel = channel.asGuildMessageChannel();
                        if (!messageChannel.canTalk(member)) {
                            event.reply("❌ You do not have permission to blacklist this channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (config.getBlacklistedChannels().contains(channel.getIdLong())) {
                            event.reply("❌ Channel is already blacklisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeWhitelistedChannel(channel.getIdLong());
                        config.addBlacklistedChannel(channel.getIdLong());

                        event.reply("✅ Successfully blacklisted channel: " + channel.getAsMention()).queue();

                        updated = true;
                    }
                    case "whitelist-role" -> {
                        Role role = event.getOption("role", null, OptionMapping::getAsRole);
                        if (role == null) {
                            event.reply("❌ You must specify a role!").setEphemeral(true).queue();
                            return;
                        }

                        if (!member.canInteract(role)) {
                            event.reply("❌ You do not have permission to whitelist this role!").setEphemeral(true).queue();
                            return;
                        }

                        if (role.isPublicRole()) {
                            event.reply("❌ You cannot whitelist the public role!").setEphemeral(true).queue();
                            return;
                        }

                        if (config.getWhitelistedRoles().contains(role.getIdLong())) {
                            event.reply("❌ Role is already whitelisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeBlacklistedChannel(role.getIdLong());
                        config.addWhitelistedChannel(role.getIdLong());

                        event.reply("✅ Successfully whitelisted role: " + role.getAsMention()).setAllowedMentions(List.of()).queue();

                        updated = true;
                    }
                    case "blacklist-role" -> {
                        Role role = event.getOption("role", null, OptionMapping::getAsRole);
                        if (role == null) {
                            event.reply("❌ You must specify a role!").setEphemeral(true).queue();
                            return;
                        }

                        if (!member.canInteract(role)) {
                            event.reply("❌ You do not have permission to blacklist this role!").setEphemeral(true).queue();
                            return;
                        }

                        if (role.isPublicRole()) {
                            event.reply("❌ You cannot blacklist the public role!").setEphemeral(true).queue();
                            return;
                        }

                        if (config.getBlacklistedRoles().contains(role.getIdLong())) {
                            event.reply("❌ Role is already blacklisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeWhitelistedRole(role.getIdLong());
                        config.addBlacklistedRole(role.getIdLong());

                        event.reply("✅ Successfully blacklisted role: " + role.getAsMention()).setAllowedMentions(List.of()).queue();

                        updated = true;
                    }
                }

                if (updated)
                    DataHandler.saveGuildData(data);
            }
            case "remove-config" -> {
                Member member = event.getMember();
                if (!isGuild || member == null) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                if (!member.hasPermission(Permission.MANAGE_SERVER)) {
                    event.reply("❌ You need the `MANAGE_SERVER` permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                GuildConfig config = data.getConfig();

                String subcommand = event.getSubcommandName();
                if (subcommand == null) {
                    event.reply("❌ You must specify a subcommand!").setEphemeral(true).queue();
                    return;
                }

                boolean updated = false;
                switch (subcommand) {
                    case "whitelist-channel" -> {
                        GuildChannelUnion channel = event.getOption("channel", null, OptionMapping::getAsChannel);
                        if (channel == null) {
                            event.reply("❌ You must specify a channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (!channel.getType().isMessage()) {
                            event.reply("❌ You cannot whitelist a category or voice channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (!channel.asGuildMessageChannel().canTalk(member)) {
                            event.reply("❌ You do not have permission to remove this channel from the whitelist!").setEphemeral(true).queue();
                            return;
                        }

                        if (!config.getWhitelistedChannels().contains(channel.getIdLong())) {
                            event.reply("❌ Channel is not whitelisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeWhitelistedChannel(channel.getIdLong());
                        event.reply("✅ Successfully removed channel from whitelist: " + channel.getAsMention()).queue();

                        updated = true;
                    }
                    case "blacklist-channel" -> {
                        GuildChannelUnion channel = event.getOption("channel", null, OptionMapping::getAsChannel);
                        if (channel == null) {
                            event.reply("❌ You must specify a channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (!channel.getType().isMessage()) {
                            event.reply("❌ You cannot blacklist a category or voice channel!").setEphemeral(true).queue();
                            return;
                        }

                        if (!channel.asGuildMessageChannel().canTalk(member)) {
                            event.reply("❌ You do not have permission to remove this channel from the blacklist!").setEphemeral(true).queue();
                            return;
                        }

                        if (!config.getBlacklistedChannels().contains(channel.getIdLong())) {
                            event.reply("❌ Channel is not blacklisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeBlacklistedChannel(channel.getIdLong());
                        event.reply("✅ Successfully removed channel from blacklist: " + channel.getAsMention()).queue();

                        updated = true;
                    }
                    case "whitelist-role" -> {
                        Role role = event.getOption("role", null, OptionMapping::getAsRole);
                        if (role == null) {
                            event.reply("❌ You must specify a role!").setEphemeral(true).queue();
                            return;
                        }

                        if (!member.canInteract(role)) {
                            event.reply("❌ You do not have permission to remove this role from the whitelist!").setEphemeral(true).queue();
                            return;
                        }

                        if (!config.getWhitelistedRoles().contains(role.getIdLong())) {
                            event.reply("❌ Role is not whitelisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeWhitelistedRole(role.getIdLong());
                        event.reply("✅ Successfully removed role from whitelist: " + role.getAsMention()).setAllowedMentions(List.of()).queue();

                        updated = true;
                    }
                    case "blacklist-role" -> {
                        Role role = event.getOption("role", null, OptionMapping::getAsRole);
                        if (role == null) {
                            event.reply("❌ You must specify a role!").setEphemeral(true).queue();
                            return;
                        }

                        if (!member.canInteract(role)) {
                            event.reply("❌ You do not have permission to remove this role from the blacklist!").setEphemeral(true).queue();
                            return;
                        }

                        if (!config.getBlacklistedRoles().contains(role.getIdLong())) {
                            event.reply("❌ Role is not blacklisted!").setEphemeral(true).queue();
                            return;
                        }

                        config.removeBlacklistedRole(role.getIdLong());
                        event.reply("✅ Successfully removed role from blacklist: " + role.getAsMention()).setAllowedMentions(List.of()).queue();

                        updated = true;
                    }
                }

                if (updated)
                    DataHandler.saveGuildData(data);
            }
            case "reset-config" -> {
                if (!isGuild) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                    event.reply("❌ You need the `MANAGE_SERVER` permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                GuildConfig config = data.getConfig();

                ResetType type = switch (event.getOption("type", null, OptionMapping::getAsString)) {
                    case "channel_whitelist" -> ResetType.CHANNEL_WHITELIST;
                    case "channel_blacklist" -> ResetType.CHANNEL_BLACKLIST;
                    case "role_whitelist" -> ResetType.ROLE_WHITELIST;
                    case "role_blacklist" -> ResetType.ROLE_BLACKLIST;
                    case null, default -> ResetType.ALL;
                };

                config.reset(type);
                event.reply("✅ Successfully reset configuration!").queue();
                DataHandler.saveGuildData(data);
            }
            case "get-config" -> {
                if (!isGuild) {
                    event.reply("❌ This command can only be used in a server!").setEphemeral(true).queue();
                    return;
                }

                if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                    event.reply("❌ You need the `MANAGE_SERVER` permission to use this command!").setEphemeral(true).queue();
                    return;
                }

                GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                GuildConfig config = data.getConfig();
                var embed = new EmbedBuilder()
                        .setTitle("Configuration")
                        .addField("Whitelisted Channels", config.getWhitelistedChannelsAsString(), false)
                        .addField("Blacklisted Channels", config.getBlacklistedChannelsAsString(), false)
                        .addField("Whitelisted Roles", config.getWhitelistedRolesAsString(), false)
                        .addField("Blacklisted Roles", config.getBlacklistedRolesAsString(), false)
                        .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(0x00FF00);

                event.replyEmbeds(embed.build()).queue();
            }
            case "help" -> {
                var embed = new EmbedBuilder()
                        .setTitle("Help")
                        .setDescription("This bot is used to play Truth or Dare!")
                        .addField("Commands", """
                                `/truth` - Get a random truth
                                `/dare` - Get a random dare
                                `/random` - Get a random truth or dare
                                `/pack` - Manage packs
                                `/edit-pack` - Edit a pack
                                `/add-config` - Add to the bot's configuration (whitelist/blacklist channels/roles)
                                `/remove-config` - Remove from the bot's configuration (whitelist/blacklist channels/roles)
                                `/reset-config` - Reset the bot's configuration (all/whitelist/blacklist channels/roles)
                                `/get-config` - Get the bot's current configuration
                                `/help` - Get this help message
                                """, false)
                        .addField("Buttons", """
                                `Truth` - Get a random truth
                                `Dare` - Get a random dare
                                `Random` - Get a random truth or dare
                                """, false)
                        .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(0x00FF00)
                        .build();

                event.replyEmbeds(embed).queue();
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

                GuildData.Response response = DataHandler.getTruthOrDare(event.getGuild().getIdLong(), type);
                if (response.id() == null) {
                    event.getHook().sendMessage("❌ No " + response.type().getName().toLowerCase(Locale.ROOT) + "s found!").setEphemeral(true).queue();
                    return;
                }

                var embed = new EmbedBuilder()
                        .setTitle(response.type().getName() + ": " + response.question())
                        .setDescription("ID: " + response.id())
                        .setFooter("Requested by: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now())
                        .setColor(response.type() == QuestionType.TRUTH ? 0x0000FF : 0xFF0000)
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
            GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
            List<TODPack> packs = data.getPacks();
            if ("remove".equals(subcommand) || "view".equals(subcommand))
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
                GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
                List<TODPack> packs = data.getPacks();
                event.replyChoiceStrings(packs.stream().filter(TODPack::isCustom).map(TODPack::getName).limit(25).toList()).queue();
            } else if (event.getFocusedOption().getName().equals("id")) {
                String packName = event.getOption("name", null, OptionMapping::getAsString);
                if (packName == null) {
                    event.replyChoices().queue();
                    return;
                }

                GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
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

    public enum ResetType {
        ALL("all"),
        CHANNEL_WHITELIST("channel_whitelist"),
        CHANNEL_BLACKLIST("channel_blacklist"),
        ROLE_WHITELIST("role_whitelist"),
        ROLE_BLACKLIST("role_blacklist");

        private final String name;

        ResetType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
