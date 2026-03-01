package launch.app.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private static final String CONFIG_FILE = "launch.properties";
    private static Properties props;

    private static Properties load() {

        if (props != null) return props;

        props = new Properties();

        File f = new File(CONFIG_FILE);

        if (f.isFile()) {

            try (FileInputStream in = new FileInputStream(f)) {

                props.load(in);

                System.out.println("[INFO] Loaded config: " + f.getAbsolutePath());

            } catch (IOException e) {

                System.out.println("[ERROR] Could not load config: " + e.getMessage());
            }

        } else {

            System.out.println("[ERROR] Config file not found: " + CONFIG_FILE + ", using defaults");

        }
        return props;
    }

    public static String get(String key, String defaultValue) {

        return load().getProperty(key, defaultValue);

    }

    public static int getInt(String key, int defaultValue) {

        String val = load().getProperty(key);

        if (val == null) return defaultValue;

        try {

            return Integer.parseInt(val.trim());

        } catch (NumberFormatException e) {

            System.out.println("[ERROR] Invalid int for key '" + key + "': " + val + ", using default " + defaultValue);

            return defaultValue;

        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {

        String val = load().getProperty(key);

        if (val == null) return defaultValue;

        return Boolean.parseBoolean(val.trim());

    }
}
