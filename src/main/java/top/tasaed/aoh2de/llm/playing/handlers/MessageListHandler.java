package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Messages.Message;
import age.of.civilizations2.jakowski.lukasz.Messages.MessageBox_GameData;
import age.of.civilizations2.jakowski.lukasz.Messages.MessageType;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class MessageListHandler extends GameRequestHandler {
    public MessageListHandler() {
        super("GET_MESSAGE_LIST_FAILED", "Failed to get the message list.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Civilization player =
                CFG.core.getCiv(CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId());

        List<JSONObject> messages = new ArrayList<>();
        MessageBox_GameData messageBox = player.getCivDiploGD().messageBox;

        for (int i = 0; i < messageBox.getMessagesSize(); i++) {
            Message message = messageBox.getMessage(i);

            MessageType type = message.messageType;

            JSONObject information = new JSONObject();

            information.put("messageId", i);
            information.put("fromCivId", message.fromCivID);
            information.put("messageType", type.name());
            information.put("numOfTurnsLeft", message.numOfTurnsLeft);
            information.put("iValue", message.iValue);
            information.put("iValue2", message.iValue2);
            information.put("TAG", message.TAG);
            information.put("requestsResponse", message.requestsResponse);
            information.put("willPauseTheGame", message.willPauseTheGame);

            messages.add(information);
        }

        JSONObject result = new JSONObject();
        result.put("messages", messages);
        return HttpResponses.success(result);
    }
}
