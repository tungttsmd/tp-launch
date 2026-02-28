package launch.app.helpers;

import java.io.File;
import java.io.IOException;

public class SimpleProcess {

    public static int run(String... command) throws IOException, InterruptedException {

        // Blocking-simple-process-spawn
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO(); // In thẳng ra console
        Process p  = pb.start();
        return p.waitFor();
    }

    public static Process start(String... command) throws IOException {

        // Non-blocking-with-handle-process-object-return
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        return pb.start();
    }
}
