package bgu.spl.a2.Bank;

import bgu.spl.a2.Action;

import java.util.ArrayList;
import java.util.List;

public class Transmission extends Action<String> {

    int amount;
    String sender;
    String receiver;
    String receiverBank;
    String senderBank;

    public Transmission(int amount, String receiver, String sender, String receiverBank, String senderBank) {
        this.senderBank = senderBank;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.receiverBank = receiverBank;
    }

    @Override
    protected void start() {
        List<Action<Boolean>> actions = new ArrayList<>();
        Action<Boolean> confAction = new Confirmation(sender, receiver, receiverBank, new BankStates());
        actions.add(confAction);
        sendMessage(confAction, receiverBank, new BankStates());
        then(actions, () -> {
            Boolean result = actions.get(0).getResult().get();
            if (result == true) {
                complete("transmission succeed");
                System.out.println("transmission succeed");
            } else {
                complete("transmission failed");
                System.out.println("transmission failed");
            }

        });
    }


}

