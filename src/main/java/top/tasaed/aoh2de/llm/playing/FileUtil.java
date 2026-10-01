package top.tasaed.aoh2de.llm.playing;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import team.rainfall.finality.FinalityLogger;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import top.tasaed.aoh2de.llm.playing.core.LPConfig;

public class FileUtil {
    private static final String CONFIG_FILE_NAME = "LP_Config.json";
    private static final JsonMapper MAPPER = new JsonMapper();

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
            JsonNode root = MAPPER.readTree(jsonContent);
            if (!root.isObject()) {
                throw new IOException(CONFIG_FILE_NAME + " must contain a configuration object");
            }
            LPConfig config = MAPPER.treeToValue(root, LPConfig.class);
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
