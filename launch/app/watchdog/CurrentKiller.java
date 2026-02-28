package launch.app.watchdog;

import java.io.File;

public class CurrentKiller {

    public static void kill() throws InterruptedException {

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
                ph.destroyForcibly();
            });

        Thread.sleep(1000);
    }
}
