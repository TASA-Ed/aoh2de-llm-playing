package top.tasaed.aoh2de.llm.playing;

import java.io.IOException;
import team.rainfall.finality.FinalityLogger;
import team.rainfall.finality.luminosity2.CallbackInfo;
import team.rainfall.finality.luminosity2.annotations.Inject;
import team.rainfall.finality.luminosity2.annotations.Mixin;

@Mixin(mixinClass = "age.of.civilizations2.jakowski.lukasz.AoCGame")
public class MixinAoCGame {
    @Inject(methodName = "create")
    private static void preCreate(CallbackInfo callbackInfo) {
        try {
            LPConfig config = FileUtil.loadConfig();
            String host = config.getHost();
            int port = config.getPort();

            LP.getInstance().start(host, port);
            FinalityLogger.info("[LP] LLM Playing HTTP server started at http://" + host + ":" + port);
        } catch (IOException e) {
            FinalityLogger.error("[LP] Failed to start the server:", e);
        }
    }

    @Inject(methodName = "dispose")
    private static void preDispose(CallbackInfo callbackInfo) {
        if (LP.getInstance().isRunning()) {
            LP.getInstance().stop();
            FinalityLogger.info("[LP] LLM Playing HTTP server stopped.");
        }
    }
}
