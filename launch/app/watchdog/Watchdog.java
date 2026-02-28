package launch.app.watchdog;

public class Watchdog {

    private final ManagedProcess[] processes;
    private volatile boolean fatalCrash = false;
    private volatile String fatalMessage = "";

    public Watchdog(ManagedProcess... processes) {
        this.processes = processes;
    }

    public void startAll() throws InterruptedException {

        System.out.println("[INFO] Starting watchdog...");

        CurrentKiller.kill();

        for (ManagedProcess p : processes) {
            
            Thread t = new Thread(() -> monitorProcess(p));
            t.setDaemon(true);
            t.setName("watchdog-" + p.name);
            t.start();
        }
    }

    public void stopAll() {

        System.out.println("[INFO] Stopping watchdog...");

        for (ManagedProcess p : processes) {
            p.kill();
        }
    }

    // Block main thread đến khi có crash fatal
    public String waitForFatal() throws InterruptedException {

        System.out.println("[INFO] Waiting for fatal...");

        while (!fatalCrash) {
            Thread.sleep(1000);
        }

        System.out.println("\n[INFO] Fatal crash detected: \n[ERROR] " + fatalMessage + "\n");

        return fatalMessage;
    }

    private void monitorProcess(ManagedProcess p) {

        System.out.println("[INFO] Monitoring " + p.name + "...");

        int restarts = 0;

        while (!fatalCrash) {
            try {
                p.start();

                while (p.isAlive()) {
                    Thread.sleep(1000);
                }

                int code = p.getExitCode();
                restarts++;

                System.out.println("[INFO] " + p.name + " existed (code: " + code + ", restart #" + restarts + ")");

                if (restarts >= 5) {
                    fatalMessage = p.name + " crashed " + restarts + " times (last exit code: " + code + ")";
                    fatalCrash = true;
                    return;
                }

                Thread.sleep(3000); // chờ 3 giây trước khi restart

            } catch (Exception e) {

                fatalMessage = p.name + " threw exception: " + e.getMessage();
                fatalCrash = true;
                return;
                
            }
        }
    }
}
