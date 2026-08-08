package top.tasaed.aoh2de.llm.playing.handlers;

import age.of.civilizations2.jakowski.lukasz.CFG;
import age.of.civilizations2.jakowski.lukasz.Civilization;
import age.of.civilizations2.jakowski.lukasz.Messages.Message;
import age.of.civilizations2.jakowski.lukasz.Messages.MessageBox_GameData;
import com.alibaba.fastjson2.JSONObject;
import top.tasaed.aoh2de.llm.playing.HttpResponses;

public final class MessageActionHandler extends GameRequestHandler {
    public MessageActionHandler() {
        super("ACTION_MESSAGE_FAILED", "Failed to action the message.");
    }

    @Override
    protected JSONObject handleOnGameThread(JSONObject request) {
        Integer messageId;
        String type;
        try {
            messageId = request.getInteger("messageId");
            type = request.getString("type");
        } catch (RuntimeException exception) {
            return HttpResponses.error("INVALID_PARAMETER", "messageId must be integers, and type must be a string.");
        }
        if (messageId == null || type == null) {
            return HttpResponses.error("MISSING_PARAMETER", "messageId and type are required.");
        }

        int playerId = CFG.core.getPlayer(CFG.PLAYER_TURN_ID).getCivId();
        Civilization player = CFG.core.getCiv(playerId);

        MessageBox_GameData messageBox = player.getCivDiploGD().messageBox;

        if (messageId < 0 || messageId > messageBox.getMessagesSize() - 1)
            return HttpResponses.error("INVALID_MESSAGE_ID", "The message ID is invalid.");

        Message message = messageBox.getMessage(messageId);

        switch (type) {
            case "accept":
                message.onAccept(playerId);
                messageBox.removeMessage(messageId);
                break;
            case "decline":
                message.onDecline(playerId);
                messageBox.removeMessage(messageId);
                break;
            default:
                return HttpResponses.error("UNKNOWN_TYPE", "The type must be accept or decline");
        }

        return HttpResponses.success();
    }
}
