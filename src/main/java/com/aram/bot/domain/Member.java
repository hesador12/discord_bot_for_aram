package com.aram.bot.domain;

public class Member {
    private final String name;
    private final int amount;

    public Member(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }
}