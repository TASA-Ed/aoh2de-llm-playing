package top.tasaed.aoh2de.llm.playing.mixins;

import top.tasaed.aoh2de.llm.playing.core.LP;
import top.tasaed.aoh2de.llm.playing.core.LPConfig;
import top.tasaed.aoh2de.llm.playing.FileUtil;

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
            FinalityLogger.info("[LP] LLM Playing " + LP.VERSION + " starting...");
            LPConfig config = FileUtil.loadConfig();
            LP.getInstance().start(config);
            String address = config.getAddress();
            FinalityLogger.info("[LP] LLM Playing " + config.getMode() + " started at " + address);
        } catch (IOException | RuntimeException e) {
            FinalityLogger.error("[LP] Failed to start the transport:", e);
        }
    }

    @Inject(methodName = "dispose")
    private static void preDispose(CallbackInfo callbackInfo) {
        LP.getInstance().stop();
        FinalityLogger.info("[LP] LLM Playing transport stopped.");
    }
}
