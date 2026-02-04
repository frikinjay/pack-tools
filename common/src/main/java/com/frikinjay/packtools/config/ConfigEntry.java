package com.frikinjay.packtools.config;

public class ConfigEntry<T> {
    private final String name;
    private final String comment;
    private final String category;
    private final T defaultValue;
    private T value;
    private final Class<T> type;
    private final T minValue;
    private final T maxValue;

    public ConfigEntry(String name, String comment, String category, T defaultValue, Class<T> type) {
        this(name, comment, category, defaultValue, type, null, null);
    }

    public ConfigEntry(String name, String comment, String category, T defaultValue, Class<T> type, T minValue, T maxValue) {
        this.name = name;
        this.comment = comment;
        this.category = category;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getName() { return name; }
    public String getComment() { return comment; }
    public String getCategory() { return category; }
    public T getDefaultValue() { return defaultValue; }
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    public Class<T> getType() { return type; }
    public T getMinValue() { return minValue; }
    public T getMaxValue() { return maxValue; }

    public boolean hasRange() {
        return minValue != null && maxValue != null;
    }
}