package Control;
/* class to represent the Control Port specialization */

import Server.*;
public class ControlPort extends ioPort {
    public void send(Message msg) {
        super.sendMessage(msg);
    }
}