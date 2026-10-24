package me.Fupery.ArtMap.IO.Protocol.In;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.IO.Protocol.In.Packet.ArtistPacket;
import me.Fupery.ArtMap.Painting.ArtistHandler;

import org.bukkit.entity.Player;

import static me.Fupery.ArtMap.IO.Protocol.In.Packet.ArtistPacket.PacketInteract;
import static me.Fupery.ArtMap.IO.Protocol.In.Packet.ArtistPacket.PacketInteract.InteractType;

public class PacketEventsReceiver extends PacketReceiver {

    private final PacketListenerCommon listener;

    public PacketEventsReceiver() {
        listener = PacketEvents.getAPI().getEventManager().registerListener(new ArtistPacketListener());
    }

    private static ArtistPacket getPacketType(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
            WrapperPlayClientPlayerRotation rotation = new WrapperPlayClientPlayerRotation(event);
            return new ArtistPacket.PacketLook(rotation.getYaw(), rotation.getPitch());

        } else if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            return new ArtistPacket.PacketArmSwing();

        } else if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            return new PacketInteract(InteractType.ATTACK);

        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            //Since 26.1 attacks have their own packet, so this is always an interaction
            return new PacketInteract(InteractType.INTERACT);
        }
        return null;
    }

    @Override
    public void close() {
        PacketEvents.getAPI().getEventManager().unregisterListener(listener);
    }

    private class ArtistPacketListener extends PacketListenerAbstract {
        ArtistPacketListener() {
            super(PacketListenerPriority.HIGH);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            Player player = event.getPlayer();
            //No Bukkit player exists yet during login/configuration
            if (player == null)
                return;
            ArtistHandler handler = ArtMap.instance().getArtistHandler();
            if (handler == null || !handler.containsPlayer(player))
                return;
            ArtistPacket packet = getPacketType(event);
            if (packet == null)
                return;
            if (!onPacketPlayIn(handler, player, packet))
                event.setCancelled(true);
        }
    }
}
