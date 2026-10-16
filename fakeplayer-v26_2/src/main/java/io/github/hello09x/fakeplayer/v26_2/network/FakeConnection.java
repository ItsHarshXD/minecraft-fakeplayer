package io.github.hello09x.fakeplayer.v26_2.network;

import io.github.hello09x.fakeplayer.core.Main;
import io.github.hello09x.fakeplayer.core.manager.FakeplayerManager;
import io.github.hello09x.fakeplayer.core.network.FakeChannel;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.logging.Logger;

public class FakeConnection extends Connection {

    private final static Logger log = Main.getInstance().getLogger();
    private final FakeplayerManager manager = Main.getInjector().getInstance(FakeplayerManager.class);

    public FakeConnection(@NotNull InetAddress address) {
        super(PacketFlow.SERVERBOUND);
        this.channel = new ClosableFakeChannel(address);
        this.address = this.channel.remoteAddress();
        Connection.configureSerialization(this.channel.pipeline(), PacketFlow.SERVERBOUND, false, null);
    }

    @Override
    public boolean isConnected() {
        return this.channel.isOpen();
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelfuturelistener) {
        // Since 26.1 a kick only completes once the disconnect packet has been sent, there is no client
        // to send it to, so report it as sent right away.
        if (packet instanceof ClientboundDisconnectPacket && channelfuturelistener != null) {
            try {
                channelfuturelistener.operationComplete(this.channel.newSucceededFuture());
            } catch (Exception e) {
                log.warning("Failed to disconnect fake player: " + e.getMessage());
            }
        }
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener channelfuturelistener, boolean flag) {
        this.send(packet, channelfuturelistener);
    }

    @Override
    public void send(Packet<?> packet) {

    }

    @Override
    public void disconnect(@NotNull DisconnectionDetails details) {
        super.disconnect(details);
        ((ClosableFakeChannel) this.channel).closed = true;

        // The server only handles disconnections of the connections it ticks, which a fake one is not
        if (Bukkit.isPrimaryThread()) {
            this.handleDisconnection();
        } else {
            Bukkit.getScheduler().runTask(Main.getInstance(), this::handleDisconnection);
        }
    }

    private final static class ClosableFakeChannel extends FakeChannel {

        private volatile boolean closed;

        public ClosableFakeChannel(@NotNull InetAddress address) {
            super(null, address);
        }

        @Override
        public boolean isOpen() {
            return !this.closed;
        }

        @Override
        public boolean isActive() {
            return !this.closed;
        }

    }

}
