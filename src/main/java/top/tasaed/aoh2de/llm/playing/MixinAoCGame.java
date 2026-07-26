package top.tasaed.aoh2de.llm.playing;

import java.io.IOException;

import team.rainfall.finality.FinalityLogger;
import team.rainfall.finality.luminosity2.CallbackInfo;
import team.rainfall.finality.luminosity2.annotations.Inject;
import team.rainfall.finality.luminosity2.annotations.Mixin;

@Mixin(mixinClass = "age.of.civilizations2.jakowski.lukasz.AoCGame")
public class MixinAoCGame {
    @Inject(methodName = "create")
    private static void beforeCreate(CallbackInfo callbackInfo) {
        try {
            LP.getInstance().start();
            FinalityLogger.info("LLM Playing HTTP server started at http://127.0.0.1:" + LP.PORT);
        } catch (IOException exception) {
            FinalityLogger.error("Failed to start LLM Playing HTTP server", exception);
        }
    }
}
