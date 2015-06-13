package gui;

/**
 * Created by Sorin Nutu on 6/13/2015.
 */
public class UpdateMessage {
    private UpdateType updateType;
    private Object data;

    public UpdateMessage(UpdateType updateType, Object data) {
        this.updateType = updateType;
        this.data = data;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public Object getData() {
        return data;
    }
}
