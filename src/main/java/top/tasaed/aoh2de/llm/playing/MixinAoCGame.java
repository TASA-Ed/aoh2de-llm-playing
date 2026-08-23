package top.tasaed.aoh2de.llm.playing;

import java.io.IOException;
import java.net.BindException;
import team.rainfall.finality.FinalityLogger;
import team.rainfall.finality.luminosity2.CallbackInfo;
import team.rainfall.finality.luminosity2.annotations.Inject;
import team.rainfall.finality.luminosity2.annotations.Mixin;

@Mixin(mixinClass = "age.of.civilizations2.jakowski.lukasz.AoCGame")
public class MixinAoCGame {
    @Inject(methodName = "create")
    private static void beforeCreate(CallbackInfo callbackInfo) {
        try {
            int port = 8080;
            for (int i = 0; i < 10; i++) {
                try {
                    LP.getInstance().start(port);
                    FinalityLogger.info("LLM Playing HTTP server started at http://127.0.0.1:" + port);
                    return;
                } catch (BindException e) {
                    FinalityLogger.warn("Port " + port + " is in use. Try the next one...");
                    port++;
                }
            }
            throw new IOException("no available port was found after 10 attempts");
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
