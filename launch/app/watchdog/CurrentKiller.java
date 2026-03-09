package launch.app.watchdog;

import java.io.File;
import java.nio.file.Files;

public class CurrentKiller {

    public static void kill() throws InterruptedException {

        // Kill 1: theo path current/ (cho .exe chạy trực tiếp)
        String currentAbsPath = new File("current")
            .getAbsolutePath()
            .replace("\\", "/")
            .toLowerCase();

        System.out.println("[INFO] Killing processes running from: \n"
        + "[INFO] Kill path: " + currentAbsPath);

        ProcessHandle.allProcesses()
            .filter(ph -> ph.info().command()
                .map(cmd -> cmd.replace("\\", "/").toLowerCase().contains(currentAbsPath))
                .orElse(false))
            .forEach(ph -> {
                System.out.println("[INFO] Killing process: " + ph.info().command().orElse("unknown") + " (pid: " + ph.pid() + ")");
                try {
                    new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(ph.pid()))
                        .start()
                        .waitFor();
                } catch (Exception e) {
                    ph.destroyForcibly();
                }
            });

        // Kill 2: theo pids file (cho .bat và child process như cloudflared)
        File pidsDir = new File("launch/app/watchdog/pids");

        if (pidsDir.isDirectory()) {

            File[] pidFiles = pidsDir.listFiles(f -> f.getName().endsWith(".pid") && !f.getName().equals("staging.pid"));

            if (pidFiles != null) {

                for (File pidFile : pidFiles) {
                    try {
                        long pid = Long.parseLong(new String(Files.readAllBytes(pidFile.toPath())).trim());
                        System.out.println("[INFO] Killing process tree for pid: " + pid + " (" + pidFile.getName() + ")");
                        new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid))
                            .start()
                            .waitFor();
                        pidFile.delete();
                    } catch (Exception e) {
                        System.out.println("[WARN] Failed to kill pid from " + pidFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        Thread.sleep(2000);
    }
}
