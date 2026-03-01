package launch.app.watchdog;

import launch.app.config.Config;

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

        int maxRestarts  = Config.getInt("watchdog.maxRestarts",  5);
        int restartDelay = Config.getInt("watchdog.restartDelay", 3000);
        int restarts = 0;

        try {
            p.start();

            System.out.println("[INFO] [ " + p.name + " ] is started");

        } catch (Exception e) {

            System.out.println("[ERROR] [ " + p.name + " ] is crashed: " + e.getMessage());
            e.printStackTrace();
            fatalCrash = true;
            fatalMessage = e.getMessage();

        }
        
        while (!fatalCrash) {

            try {

                p.waitFor();

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

                    fatalCrash = true;

                    fatalMessage = "Retried (max " + maxRestarts + " times), timeout for retry, app will be relaunch";

                }

                Thread.sleep(restartDelay);


            } catch (Exception e) {

                System.out.println("[ERROR] [ " + p.name + " ] is crashed: " + e.getMessage());
                e.printStackTrace();
                fatalCrash = true;
                fatalMessage = e.getMessage();

            }
        }

        System.out.println("\n[INFO] Fatal crash detected on [ " + p.name + " ]. App is relaunching...");
        System.out.println("[ERROR] [ " + p.name + " ] Fatal detail: \n[ERROR] " + fatalMessage + "\n");
        System.out.println("[INFO] [ " + p.name + " ] Reset fatal and restarts state...");
        
        // Reset trạng thái
        fatalCrash = false;
        fatalMessage = "";

        System.out.println("[INFO] [ " + p.name + " ] Done.");

    }

    public int mainThreadWaitForFatalCrash() throws InterruptedException {

        int waitForFatalCrash = Config.getInt("watchdog.waitForFatalCrash", 1000);

        System.out.println("[INFO] Waiting for fatal...");

        int ticks = 0;

        while (!fatalCrash) {
            
            System.out.println("[INFO] Watchdog ticking..." + ticks++);

            Thread.sleep(waitForFatalCrash);
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
