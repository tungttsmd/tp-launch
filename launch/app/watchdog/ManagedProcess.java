package launch.app.watchdog;

import launch.app.helpers.SimpleProcess;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ManagedProcess {

    public final String name;
    private final String[] command;

    private static final File PIDS_DIR = new File("launch/app/watchdog/pids");

    private Process process;

    public ManagedProcess(String name, String... command) {

        this.name = name;
        this.command = command;

    }

    public void start() throws IOException {

        process = SimpleProcess.start(command);

        writePid();

    }

    private void writePid() throws IOException {

        PIDS_DIR.mkdirs();

        Files.write(
            new File(PIDS_DIR, name + ".pid").toPath(),
            String.valueOf(process.pid()).getBytes()
        );

    }

    public void waitFor() throws InterruptedException {

        if (process != null) process.waitFor();

    }

    public boolean isAlive() {

        return process != null && process.isAlive();

    }

    public int getExitCode() {

        return process.exitValue();

    }

    public void kill() {

        if (process != null) {
            try {
                new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(process.pid()))
                    .start()
                    .waitFor();
            } catch (Exception e) {
                process.destroyForcibly();
            }
            new File(PIDS_DIR, name + ".pid").delete();
        }

    }

    public void restart() throws IOException {

        kill();
        start();

    }
}
