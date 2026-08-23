package top.tasaed.aoh2de.llm.playing;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Z_Other.DialogType;
import team.rainfall.finality.FinalityLogger;
import team.rainfall.finality.luminosity2.CallbackInfo;
import team.rainfall.finality.luminosity2.annotations.Inject;
import team.rainfall.finality.luminosity2.annotations.Mixin;

@Mixin(mixinClass = "age.of.civilizations2.jakowski.lukasz.CFG")
public class MixinCFG {
    @Inject(methodName = "dialog_True")
    private static void postDialog_True(CallbackInfo callbackInfo) {
        if (CFG.dialogType == DialogType.EXIT_GAME && LP.getInstance().isRunning()) {
            LP.getInstance().stop();
            FinalityLogger.info("[LP] LLM Playing HTTP server stopped.");
        }
    }
}
