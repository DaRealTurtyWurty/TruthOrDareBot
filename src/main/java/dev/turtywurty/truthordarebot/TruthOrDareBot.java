package dev.turtywurty.truthordarebot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.ext.java7.PathArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Random;

public class TruthOrDareBot {
    public static final Logger LOGGER = LoggerFactory.getLogger(TruthOrDareBot.class);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Random RANDOM = new Random();

    public static void main(String[] args) {
        LOGGER.info("Starting Truth or Dare Bot...");

        ArgumentParser parser = ArgumentParsers.newFor("Truth or Dare Bot").addHelp(true)
                .build()
                .defaultHelp(true)
                .description("A Discord bot that plays Truth or Dare!");

        parser.addArgument("-env", "--environment")
                .type(new PathArgumentType().verifyExists().verifyIsFile().verifyCanRead())
                .setDefault(Path.of(".env"))
                .help("The path to the environment file.");

        Namespace namespace = parser.parseArgsOrFail(args);
        Environment.INSTANCE.load(namespace.get("environment"));
        LOGGER.info("Loaded environment file!");

        Environment.INSTANCE.botToken().ifPresentOrElse(token -> {
            LOGGER.info("Starting JDA...");

            JDABuilder builder = JDABuilder.createLight(token);
            builder.setAutoReconnect(true);
            builder.setActivity(Activity.playing("Truth or Dare!"));

            builder.addEventListeners(new CommandListener());
            builder.addEventListeners(new GenericListener());

            JDA jda = null;
            try {
                jda = builder.build().awaitReady();
            } catch (InterruptedException exception) {
                LOGGER.error("Failed to start JDA!", exception);
                System.exit(1);
            }

            jda.updateCommands().addCommands(
                    Commands.slash("truth", "Get a random truth!"),
                    Commands.slash("dare", "Get a random dare!"),
                    Commands.slash("random", "Get a random truth or dare question!"),
                    Commands.slash("help", "Get a list of commands!")
            ).queue();

            LOGGER.info("Started JDA!");
        }, () -> {
            LOGGER.error("Bot token not found! Exiting...");
            System.exit(1);
        });

        LOGGER.info("Finished starting Truth or Dare Bot!");
    }

    public static @Nullable InputStream loadResource(String name) {
        return TruthOrDareBot.class.getResourceAsStream("/" + name);
    }

    public static URL getResource(String name) {
        return TruthOrDareBot.class.getResource(name);
    }
}
