package dev.turtywurty.truthordarebot;

import dev.turtywurty.truthordarebot.data.GuildConfig;
import dev.turtywurty.truthordarebot.data.GuildData;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class GenericListener extends ListenerAdapter {
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(
                Commands.slash("pack", "Use, add or remove a pack, or list all packs!").addSubcommands(
                        new SubcommandData("use", "Use a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true, true),
                        new SubcommandData("add", "Add a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true)
                                .addOption(OptionType.STRING, "description", "The description of the pack!", false),
                        new SubcommandData("remove", "Remove a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true, true),
                        new SubcommandData("list", "List all packs!")),
                Commands.slash("edit-pack", "Edit a pack!").addSubcommands(
                        new SubcommandData("description", "Edit the description of a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true, true)
                                .addOption(OptionType.STRING, "description", "The new description of the pack!", true),
                        new SubcommandData("add-truth", "Add a truth to a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true, true)
                                .addOption(OptionType.STRING, "truth", "The truth to add to the pack!", true),
                        new SubcommandData("add-dare", "Add a dare to a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true, true)
                                .addOption(OptionType.STRING, "dare", "The dare to add to the pack!", true),
                        new SubcommandData("remove-truth", "Remove a truth from a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true, true)
                                .addOption(OptionType.STRING, "id", "The id of the truth to remove from the pack!", true, true),
                        new SubcommandData("remove-dare", "Remove a dare from a pack!")
                                .addOption(OptionType.STRING, "name", "The name of the pack!", true, true)
                                .addOption(OptionType.STRING, "id", "The id of the dare to remove from the pack!", true, true)),
                Commands.slash("add-config", "Add to the configuration!").addSubcommands(
                        new SubcommandData("whitelist-channel", "Whitelist a channel!")
                                .addOption(OptionType.CHANNEL, "channel", "The channel to whitelist!", true),
                        new SubcommandData("blacklist-channel", "Blacklist a channel!")
                                .addOption(OptionType.CHANNEL, "channel", "The channel to blacklist!", true),
                        new SubcommandData("whitelist-role", "Whitelist a role!")
                                .addOption(OptionType.ROLE, "role", "The role to whitelist!", true),
                        new SubcommandData("blacklist-role", "Blacklist a role!")
                                .addOption(OptionType.ROLE, "role", "The role to blacklist!", true)),
                Commands.slash("remove-config", "Remove from the configuration!").addSubcommands(
                        new SubcommandData("whitelist-channel", "Remove a channel from the whitelist!")
                                .addOption(OptionType.CHANNEL, "channel", "The channel to remove from the whitelist!", true),
                        new SubcommandData("blacklist-channel", "Remove a channel from the blacklist!")
                                .addOption(OptionType.CHANNEL, "channel", "The channel to remove from the blacklist!", true),
                        new SubcommandData("whitelist-role", "Remove a role from the whitelist!")
                                .addOption(OptionType.ROLE, "role", "The role to remove from the whitelist!", true),
                        new SubcommandData("blacklist-role", "Remove a role from the blacklist!")
                                .addOption(OptionType.ROLE, "role", "The role to remove from the blacklist!", true)),
                Commands.slash("get-config", "Get the current configuration!"),
                Commands.slash("reset-config", "Reset the configuration!")
                        .addOptions(new OptionData(OptionType.STRING, "type", "The type of configuration to reset!", false)
                                .addChoice("all", "all")
                                .addChoice("Channel Whitelist", "channel_whitelist")
                                .addChoice("Channel Blacklist", "channel_blacklist")
                                .addChoice("Role Whitelist", "role_whitelist")
                                .addChoice("Role Blacklist", "role_blacklist"))
        ).queue();
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) return;

        GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
        GuildConfig config = data.getConfig();

        if(config.getWhitelistedChannels().isEmpty() && config.getBlacklistedChannels().isEmpty())
            return;

        boolean removed = false;
        if (config.getWhitelistedChannels().contains(event.getChannel().getIdLong())) {
            config.removeWhitelistedChannel(event.getChannel().getIdLong());
            removed = true;
        }

        if (config.getBlacklistedChannels().contains(event.getChannel().getIdLong())) {
            config.removeBlacklistedChannel(event.getChannel().getIdLong());
            removed = true;
        }

        if (removed)
            DataHandler.saveGuildData(data);
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        GuildData data = DataHandler.getGuildData(event.getGuild().getIdLong());
        GuildConfig config = data.getConfig();

        if(config.getWhitelistedRoles().isEmpty() && config.getBlacklistedRoles().isEmpty())
            return;

        boolean removed = false;
        if (config.getWhitelistedRoles().contains(event.getRole().getIdLong())) {
            config.removeWhitelistedRole(event.getRole().getIdLong());
            removed = true;
        }

        if (config.getBlacklistedRoles().contains(event.getRole().getIdLong())) {
            config.removeBlacklistedRole(event.getRole().getIdLong());
            removed = true;
        }

        if (removed)
            DataHandler.saveGuildData(data);
    }
}
