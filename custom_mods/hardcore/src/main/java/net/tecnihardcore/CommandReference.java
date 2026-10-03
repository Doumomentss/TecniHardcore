package net.tecnihardcore;

/** Public command aliases do not replace transaction or network UUIDs. */
final class CommandReference {
    private CommandReference() {}
    static boolean matches(String input, String uuid, int number) {
        return input != null && (input.equalsIgnoreCase(uuid) || input.equals(Integer.toString(number)));
    }
}
