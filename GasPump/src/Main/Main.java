package Main;

import Server.CommPort;
import Server.Message;

import java.io.IOException;
import java.util.Scanner;

// Main.Main
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        Thread mainServerThread = new Thread(() -> {
            CommPort commPort = new CommPort(20000);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (scanner.hasNext()) {
                    Message msg = new Message(scanner.next());
                    commPort.send(msg);
                }
            }
        });

        mainServerThread.start();
    }
}
