package top.tasaed.aoh2de.llm.playing;

import com.alibaba.fastjson2.JSON;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import team.rainfall.finality.FinalityLogger;

public class FileUtil {
    private static final String CONFIG_FILE_NAME = "LP_Config.json";

    public static LPConfig loadConfig() {
        File configFile = new File(CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            FinalityLogger.info("[LP] Config file not found, using default configuration");
            return new LPConfig();
        }

        try {
            String jsonContent = readString_UTF8(configFile);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                FinalityLogger.warn("[LP] Config file is empty, using default configuration");
                return new LPConfig();
            }

            LPConfig config = JSON.parseObject(jsonContent, LPConfig.class);
            if (config == null) {
                FinalityLogger.warn("[LP] Failed to parse config file, using default configuration");
                return new LPConfig();
            }

            FinalityLogger.info("[LP] Loaded configuration from " + CONFIG_FILE_NAME);
            return config;
        } catch (Exception e) {
            FinalityLogger.error("[LP] Failed to load config file, using default configuration", e);
            return new LPConfig();
        }
    }

    private static String readString_UTF8(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            FinalityLogger.error("[LP] Failed while readString_UTF8(File)", e);
            return null;
        }
    }
}
