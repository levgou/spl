package bgu.spl.a2.Bank;

import bgu.spl.a2.Action;

public class Confirmation extends Action<Boolean> {


    private String sender;
    private String receiver;
    private String receiverBank;
    private BankStates bankStates;

    public Confirmation(String sender, String receiver , String receiverBank, BankStates bankStates) {
        super();
        this.sender = sender;
        this.receiver = receiver;
        this.receiverBank = receiverBank;
        this.bankStates = bankStates;
    }

    @Override
    protected void start() {
        complete(Math.random() > 0.5);
    }
}
