package swiftly.controllers;

public interface Action<T> {
    void perform(T actionData);
}
