package dev.turtywurty.truthordarebot;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;
import java.util.Optional;

public class Environment {
    public static final Environment INSTANCE = new Environment();
    private Dotenv env;

    private Environment() {}

    public Optional<String> botToken() {
        return getString("BOT_TOKEN");
    }

    public Optional<Double> getDouble(String key){
        try {
            return getString(key).map(Double::parseDouble);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<Float> getFloat(String key) {
        try {
            return getString(key).map(Float::parseFloat);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<Integer> getInteger(String key) {
        try {
            return getString(key).map(Integer::parseInt);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<Long> getLong(String key) {
        try {
            return getString(key).map(Long::parseLong);
        } catch (final NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public Optional<String> getString(String key) {
        try {
            return Optional.ofNullable(this.env.get(key));
        } catch (final NullPointerException exception) {
            return Optional.empty();
        }
    }

    public boolean isDevelopment() {
        return getString("ENVIRONMENT").map(env -> env.equalsIgnoreCase("development")).orElse(false);
    }

    public void print() {
        this.env.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
                .forEach(entry -> TruthOrDareBot.LOGGER.debug("{}={}", entry.getKey(), entry.getValue()));
    }

    public void load(Path environment) {
        if(this.env != null)
            throw new IllegalStateException("Environment already loaded!");

        this.env = Dotenv.configure().directory(environment.toString()).load();
    }
}
