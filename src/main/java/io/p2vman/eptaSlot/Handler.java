package io.p2vman.eptaSlot;

public class Handler {
    String permission;
    String logic;

    @Override
    public String toString() {
        return  permission + ":" + logic;
    }
}
