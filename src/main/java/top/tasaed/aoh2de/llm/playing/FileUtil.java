package top.tasaed.aoh2de.llm.playing;

import com.alibaba.fastjson2.JSON;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import team.rainfall.finality.FinalityLogger;
import top.tasaed.aoh2de.llm.playing.core.LPConfig;

public class FileUtil {
    private static final String CONFIG_FILE_NAME = "LP_Config.json";

    public static LPConfig loadConfig() throws IOException {
        File configFile = new File(CONFIG_FILE_NAME);
        if (Files.notExists(configFile.toPath())) {
            FinalityLogger.info("[LP] Config file not found, using default configuration");
            LPConfig config = new LPConfig();
            config.validate();
            return config;
        }

        try {
            String jsonContent = readString_UTF8(configFile);
            if (jsonContent.isBlank()) {
                throw new IOException(CONFIG_FILE_NAME + " must contain a configuration object");
            }
            LPConfig config = JSON.parseObject(jsonContent, LPConfig.class);
            if (config == null) {
                throw new IOException(CONFIG_FILE_NAME + " must contain a configuration object");
            }
            config.validate();
            FinalityLogger.info("[LP] Loaded configuration from " + CONFIG_FILE_NAME);
            return config;
        } catch (RuntimeException e) {
            throw new IOException("Invalid configuration in " + CONFIG_FILE_NAME, e);
        }
    }

    private static String readString_UTF8(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
