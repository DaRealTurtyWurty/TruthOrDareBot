package dev.turtywurty.truthordarebot;

import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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
                                .addOption(OptionType.STRING, "id", "The id of the dare to remove from the pack!", true, true))
        ).queue();
    }
}
