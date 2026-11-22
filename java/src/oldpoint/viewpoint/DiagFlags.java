package oldpoint.viewpoint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Temporary diagnostics: switches read from Zomboid/Lua/OldPoint_diag.txt, rechecked twice a second. */
public final class DiagFlags {
    private static volatile String flags = "";
    private static volatile long checked;
    private static Path file;

    private DiagFlags() {}

    public static boolean on(String name) {
        long now = System.currentTimeMillis();
        if (now - checked > 500) {
            checked = now;
            try {
                if (file == null) {
                    file = Paths.get(zombie.ZomboidFileSystem.instance.getCacheDir(), "Lua", "OldPoint_diag.txt");
                }
                String read = Files.exists(file) ? Files.readString(file) : "";
                if (!read.equals(flags)) {
                    System.out.println("[OldPoint] diag flags now: " + read.trim());
                }
                flags = read;
            } catch (Exception error) {
                flags = "";
            }
        }
        return flags.contains(name);
    }
}
