package me.Fupery.ArtMap.IO.Protocol;

import com.github.retrooper.packetevents.PacketEvents;

import org.bukkit.Bukkit;

import me.Fupery.ArtMap.IO.Protocol.In.PacketEventsReceiver;
import me.Fupery.ArtMap.IO.Protocol.In.PacketReceiver;
import me.Fupery.ArtMap.api.Exception.ArtMapException;


public class ProtocolHandler {

    private final PacketReceiver packetReceiver;

    public ProtocolHandler() throws ArtMapException {
        try {
            if (PacketEvents.getAPI() == null || !PacketEvents.getAPI().isInitialized())
                throw new IllegalStateException("PacketEvents is not initialized");
            packetReceiver = new PacketEventsReceiver();
            Bukkit.getLogger().info("[ArtMap] Using PacketEvents PacketReciever.");
        } catch (Exception | NoClassDefFoundError e) {
            throw new ArtMapException("PacketEvents could not be hooked! Please install a compatible version of PacketEvents.");
        }
    }

    public PacketReceiver getPacketReceiver() {
        return this.packetReceiver;
    }
}
