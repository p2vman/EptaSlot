package io.p2vman.eptaSlot;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SlotExpansion extends PlaceholderExpansion {
    private EptaSlot eptaSlot;
    public SlotExpansion(EptaSlot eptaSlot) {
        this.eptaSlot = eptaSlot;
    }
    @Override
    @NotNull
    public String getAuthor() {
        return "p2vman";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "eptaslot";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }


    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("reserve")) {
            return String.valueOf(eptaSlot.slots_reserve);
        }


        if (params.equalsIgnoreCase("slots")) {
            return String.valueOf(eptaSlot.slots);
        }

        return null;
    }
}

