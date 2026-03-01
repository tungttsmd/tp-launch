package launch.app.watchdog;

import launch.app.config.Config;

public class Watchdog {

    private final ManagedProcess[] processes;
    private volatile boolean fatalCrash = false;
    private volatile String fatalMessage = "";
    private volatile int returnExitcode = -1;  // -1 = chưa có signal, >= 0 = trả về exitcode này


    public Watchdog(ManagedProcess... processes) {

        this.processes = processes;
    
    }

    public void startAll() throws InterruptedException {

        System.out.println("[INFO] Starting watchdog...");

        CurrentKiller.kill();

        int watchdogLaunchDelay = Config.getInt("watchdog.launchDelay", 5000);

        for (ManagedProcess p : processes) {
            Thread t = new Thread(() -> superviseOnProcess(p));
            t.setDaemon(true);
            t.setName("watchdog-" + p.name);
            t.start();

            try {

                System.out.println("[INFO] Waiting for " + watchdogLaunchDelay + "ms before the next process launching...");

                Thread.sleep(watchdogLaunchDelay);

            } catch (InterruptedException e) {

                e.printStackTrace();

            }
        }
    }

    public void stopAll() {

        System.out.println("[INFO] Stopping watchdog...");

        for (ManagedProcess p : processes) {

            p.kill();

        }
    }

    private void superviseOnProcess(ManagedProcess p) {

        System.out.println("[INFO] Supervising [ " + p.name + " ]...");

        try {
            p.start();

            System.out.println("[INFO] [ " + p.name + " ] is started");

        } catch (Exception e) {

            System.out.println("[ERROR] [ " + p.name + " ] is crashed: " + e.getMessage());
            e.printStackTrace();
            fatalCrash = true;
            fatalMessage = e.getMessage();

        }

        Object[] fatal = recoveryProcess(p, fatalCrash, fatalMessage);

        fatalCrash = (boolean) fatal[0];
        fatalMessage = (String) fatal[1];

        if (returnExitcode < 0) {
            // Chỉ log fatal khi thực sự là crash, không phải signal bình thường từ process
            System.out.println("\n[INFO] Fatal crash detected on [ " + p.name + " ]. App is relaunching...");
            System.out.println("[ERROR] [ " + p.name + " ] Fatal detail: \n[ERROR] " + fatalMessage + "\n");
            System.out.println("[INFO] [ " + p.name + " ] Reset fatal and restarts state...");
        }

        fatalCrash = false;
        fatalMessage = "";

        System.out.println("[INFO] [ " + p.name + " ] Done.");

    }

    private Object[] recoveryProcess(ManagedProcess p, boolean fatalCrash, String fatalMessage) {

        int maxRestarts  = Config.getInt("watchdog.maxRestarts",  5);
        int restartDelay = Config.getInt("watchdog.restartDelay", 3000);
        int restarts = 0;

        while (!fatalCrash) {

            try {

                p.waitFor();

                handleProcessExitcode(p);

                if (returnExitcode >= 0) {
                    // Process gửi signal đặc biệt → không restart, trả quyền điều khiển về launcher
                    return new Object[]{false, ""};
                }

                if (p.getExitCode() != 0) {
                    System.out.println("[INFO] [ " + p.name + " ] " + p.name + " stopped unexpectedly (exitcode from process: " + p.getExitCode() + ")");
                } else {
                    System.out.println("[INFO] [ " + p.name + " ] " + p.name + " stopped by user (exitcode from process: " + p.getExitCode() + ")");
                }

                p.restart();

                System.out.println("[INFO] [ " + p.name + " ] is restarted");

                restarts++;

                System.out.println("[INFO] Waiting for [ " + p.name + " ] response..." + "(retried " + restarts + "/" + maxRestarts + " times)");

                if (restarts >= maxRestarts) {

                    restarts = 0;

                    System.out.println("[INFO] Maybe [ " + p.name + " ] got some problems. I'll restart it, hope it can help.");
                
                    for (int i = 0; i < 3; i++) {
                        
                        System.out.println("[INFO] A fatal crash will be thrown on [ " + p.name + " ] in " + (3 - i) + " seconds...");
                        
                        Thread.sleep(restartDelay);
                    }

                    return new Object[]{true, "Retried (max " + maxRestarts + " times), timeout for retry, app will be relaunch"};

                }

                Thread.sleep(restartDelay);


            } catch (Exception e) {

                System.out.println("[ERROR] [ " + p.name + " ] is crashed: " + e.getMessage());
                e.printStackTrace();
                return new Object[]{true, e.getMessage()};
                
            }
        }

        return new Object[]{false, ""};
    }

    private void handleProcessExitcode(ManagedProcess p) {

        int code = p.getExitCode();

        System.out.println("[INFO] [ " + p.name + " ] stopped (exitcode: " + code + ")");

        // Các exitcode đặc biệt từ process được forward thẳng lên launcher
        if (code == 80 || code == 81 || code == 99) {
            System.out.println("[INFO] [ " + p.name + " ] Forwarding exitcode " + code + " to launcher");
            returnExitcode = code;
        }
    }

    public int mainThreadWaitForProcessExitcode() throws InterruptedException {

        int waitForProcessExitcode = Config.getInt("watchdog.waitForProcessExitcode", 2000);
        int ticksOnSecond = waitForProcessExitcode/1000;

        System.out.println("[INFO] Waiting for any process exitcode...");

        int ticks = 0;

        while (!fatalCrash && returnExitcode < 0) {

            ticks += ticksOnSecond;

            System.out.println("[INFO] Watchdog ticking..." + ticks + "s");

            Thread.sleep(waitForProcessExitcode);
        }

        if (returnExitcode >= 0) {
            int code = returnExitcode;
            returnExitcode = -1;
            System.out.println("[INFO] Process exitcode signal received: " + code);
            return code;
        }

        System.out.println("\n[INFO] Fatal crash detected: \n[ERROR] " + fatalMessage + "\n");

        stopAll();
        
        try {
            System.out.println("[INFO] Waiting for 10 seconds before relaunch...");
            
            for (int i = 0; i < 10; i++) {
                System.out.println("[INFO] Waiting reinstall app for " + (10 - i) + " seconds...");
                Thread.sleep(1000);
            }
            
            return 96;

        } catch (Exception e) {
            
            System.out.println("[ERROR] Watchdog is crashed: " + e.getMessage());
            e.printStackTrace();
            return 281;
        }
    }

}
