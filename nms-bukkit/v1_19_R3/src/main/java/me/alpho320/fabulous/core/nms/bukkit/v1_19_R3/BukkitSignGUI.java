package me.alpho320.fabulous.core.nms.bukkit.v1_19_R3;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import me.alpho320.fabulous.core.api.manager.impl.sign.SignGUI;
import me.alpho320.fabulous.core.api.manager.impl.sign.SignManager;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayInUpdateSign;
import net.minecraft.network.protocol.game.PacketPlayOutBlockChange;
import net.minecraft.network.protocol.game.PacketPlayOutOpenSignEditor;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.block.entity.TileEntitySign;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftSign;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class BukkitSignGUI extends SignGUI {

    public BukkitSignGUI(@NotNull SignManager manager) {
        super(manager);
    }
    
    public Material getMaterial() {
        if (type().equals(SignType.OAK)) return Material.OAK_SIGN;
        else if (type().equals(SignType.ACACIA)) return Material.ACACIA_SIGN;
        else if (type().equals(SignType.BIRCH)) return Material.BIRCH_SIGN;
        else if (type().equals(SignType.DARK_OAK)) return Material.DARK_OAK_SIGN;
        else if (type().equals(SignType.JUNGLE)) return Material.JUNGLE_SIGN;
        else if (type().equals(SignType.SPRUCE)) return Material.SPRUCE_SIGN;
        else if (type().equals(SignType.CRIMSON)) return Material.CRIMSON_SIGN;
        else return Material.OAK_SIGN;
    }
    
    @Override
    public @NotNull SignGUI open(@NotNull Object p, @NotNull SignType signType, @Nullable Consumer<String[]> whenOpen, @Nullable Consumer<String[]> whenClose) {
        Player player = (Player) p;

        PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().b;
        BlockPosition blockPosition = new BlockPosition(player.getLocation().getBlockX(), -64 + 1, player.getLocation().getBlockZ());
        IBlockData data = CraftMagicNumbers.getBlock(getMaterial()).o();

        PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(blockPosition, data);
        playerConnection.a(packet);

        IChatBaseComponent[] components = CraftSign.sanitizeLines(lines());
        TileEntitySign sign = new TileEntitySign(blockPosition, data);
        System.arraycopy(components, 0, sign.f, 0, sign.f.length);
        playerConnection.a(sign.c());

        PacketPlayOutOpenSignEditor outOpenSignEditor = new PacketPlayOutOpenSignEditor(blockPosition);
        playerConnection.a(outOpenSignEditor);

        setChannelID(blockPosition + player.getName());
        if (whenOpen != null) whenOpen.accept(lines());

        ChannelDuplexHandler channelHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object packet) throws Exception {
                if (packet instanceof PacketPlayInUpdateSign) {
                    PacketPlayInUpdateSign packetSign = (PacketPlayInUpdateSign) packet;

                    Bukkit.getScheduler().runTask((Plugin) manager().core().plugin(), () -> {
                        BlockPosition position = packetSign.a();
                        String id = position + player.getName();

                        if (channelID() != null && channelID().equals(id)) {
                            //Block block = player.getWorld().getBlockAt(position.u(), position.v(), position.w());
                            //block.setType(block.getType());
                            playerConnection.a(new PacketPlayOutBlockChange(position, data));

                            if (whenClose != null) whenClose.accept(Arrays.copyOf(packetSign.c(), packetSign.c().length));
                            manager().remove(id());

                        }
                    });

                    super.channelRead(ctx, packet);
                }
            }
        };

        return this;
    }

}