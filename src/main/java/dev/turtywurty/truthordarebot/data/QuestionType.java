package dev.turtywurty.truthordarebot.data;

public enum QuestionType {
    TRUTH("Truth"),
    DARE("Dare"),
    RANDOM("Random");

    private final String name;

    QuestionType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
