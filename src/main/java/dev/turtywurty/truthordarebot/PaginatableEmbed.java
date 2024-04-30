package dev.turtywurty.truthordarebot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaginatableEmbed extends ListenerAdapter {
    private static final List<Button> BUTTONS = List.of(
            Button.secondary("previous", Emoji.fromFormatted("⬅️")),
            Button.danger("destroy", Emoji.fromFormatted("🗑️")),
            Button.secondary("next", Emoji.fromFormatted("➡️"))
    );

    private final JDA jda;
    private final int maxPerPage;
    private final EmbedBuilder embedBuilder;
    private final List<String> items = new ArrayList<>();
    private final int maxPages;

    private long guildId, channelId, messageId, ownerId;
    private int page;

    public PaginatableEmbed(JDA jda, int maxPerPage, EmbedBuilder embedBuilder, String... items) {
        this.jda = jda;
        this.maxPerPage = maxPerPage;
        this.embedBuilder = embedBuilder;
        Collections.addAll(this.items, items);

        this.page = 0;
        this.maxPages = (int) Math.ceil((double) this.items.size() / this.maxPerPage);
        updateEmbed();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.isFromGuild() ||
                event.getGuild().getIdLong() != this.guildId ||
                event.getChannel().getIdLong() != this.channelId ||
                event.getMessageIdLong() != this.messageId ||
                event.getUser().getIdLong() != this.ownerId)
            return;

        switch (event.getComponentId()) {
            case "previous":
                previousPage(event.getHook());
                break;
            case "next":
                nextPage(event.getHook());
                break;
            case "destroy":
                event.getChannel().deleteMessageById(this.messageId).queue();
                event.getJDA().removeEventListener(this);
                break;
        }
    }

    public void nextPage(InteractionHook hook) {
        int currentPage = this.page;
        if (this.page + 1 < this.maxPages) {
            this.page++;
            updateMessage(hook);
        }

        if (currentPage != this.maxPages - 1 && this.page == this.maxPages - 1) {
            hook.retrieveOriginal()
                    .flatMap(message -> message.editMessageComponents(ActionRow.of(BUTTONS.subList(0, BUTTONS.size() - 1))))
                    .queue();
        }
    }

    public void previousPage(InteractionHook hook) {
        int currentPage = this.page;
        if (this.page - 1 >= 0) {
            this.page--;
            updateMessage(hook);
        }

        if (currentPage != 0 && this.page == 0) {
            hook.retrieveOriginal()
                    .flatMap(message -> message.editMessageComponents(ActionRow.of(BUTTONS.subList(1, BUTTONS.size()))))
                    .queue();
        }
    }

    public void updateEmbed() {
        int start = this.page * this.maxPerPage;
        int end = Math.min(start + this.maxPerPage, this.items.size());
        List<String> subList = this.items.subList(start, end);

        this.embedBuilder.getDescriptionBuilder().setLength(0);
        for (String item : subList) {
            this.embedBuilder.appendDescription("`" + item + "`").appendDescription("\n");
        }
    }

    public void updateMessage(InteractionHook hook) {
        updateEmbed();
        hook.editOriginalEmbeds(this.embedBuilder.build())
                .setActionRow(BUTTONS)
                .queue();
    }

    public void respondToEvent(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild())
            return;

        MessageChannelUnion channel = event.getChannel();
        event.replyEmbeds(this.embedBuilder.build())
                .setActionRow(BUTTONS.subList(1, this.maxPages == 1 ? BUTTONS.size() - 1 : BUTTONS.size()))
                .flatMap(InteractionHook::retrieveOriginal)
                .queue(message -> {
                    this.guildId = event.getGuild().getIdLong();
                    this.channelId = channel.getIdLong();
                    this.messageId = message.getIdLong();
                    this.ownerId = event.getUser().getIdLong();

                    this.jda.addEventListener(this);
                });
    }
}
