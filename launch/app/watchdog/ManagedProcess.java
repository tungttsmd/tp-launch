package launch.app.watchdog;

import launch.app.helpers.SimpleProcess;
import java.io.IOException;

public class ManagedProcess {

    public final String name;
    private final String[] command;

    private Process process;

    public ManagedProcess(String name, String... command) {

        this.name = name;
        this.command = command;

    }

    public void start() throws IOException {

        process = SimpleProcess.start(command);

    }

    public boolean isAlive() {

        return process != null && process.isAlive();

    }

    public int getExitCode() {

        return process.exitValue();

    }

    public void kill() {

        if (process != null) {
            process.destroyForcibly();
        }

    }

    public void restart() throws IOException {

        kill();
        start();

    }
}
