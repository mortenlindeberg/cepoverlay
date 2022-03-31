package no.uio.ifi.dmms.cepoverlay.queries;

import no.uio.ifi.dmms.cepoverlay.Main;
import no.uio.ifi.dmms.cepoverlay.overlay.OverlayInstance;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    public static int readQueryId() {
        InputStream input = null;
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            Properties prop = new Properties();
            prop.load(input);
            if (prop.containsKey("queryId"))
                return Integer.parseInt(prop.getProperty("queryId"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int readMigrationSlowdown() {
        InputStream input = null;
        int migrationSlowDown = 0;
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            Properties prop = new Properties();
            prop.load(input);
            migrationSlowDown = Integer.parseInt(prop.getProperty("migrationSlowDown"));
            return migrationSlowDown;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean readWindowAware() {
        InputStream input = null;
        Properties prop = new Properties();
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            prop.load(input);
            return Boolean.parseBoolean(prop.getProperty("windowAware"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean readWindowWait() {
        InputStream input = null;
        Properties prop = new Properties();
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            prop.load(input);
            return Boolean.parseBoolean(prop.getProperty("windowWait"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int readMigrationTime() {
        InputStream input = null;
        int migrationTime = OverlayInstance.DEFAULT_MIGRATION_TIME; //
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            Properties prop = new Properties();
            prop.load(input);
            migrationTime = Integer.parseInt(prop.getProperty("migrationTime"));
            return migrationTime;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return migrationTime;
    }

    public static int readWindowSize() {
        InputStream input = null;
        int windowSize = OverlayInstance.DEFAULT_WINDOW_SIZE; //
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            Properties prop = new Properties();
            prop.load(input);
            windowSize = Integer.parseInt(prop.getProperty("windowSize"));
            return windowSize;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return windowSize;
    }

    public static boolean readWindowSmart() {
        InputStream input = null;
        Properties prop = new Properties();
        try {
            input = new FileInputStream(Main.CONFIG_PROPERTIES);
            prop.load(input);
            return Boolean.parseBoolean(prop.getProperty("windowSmart"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
