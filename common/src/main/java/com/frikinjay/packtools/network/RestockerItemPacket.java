package com.frikinjay.packtools.network;

import com.frikinjay.packtools.PackTools;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record RestockerItemPacket(
        int handSlot,
        int sourceSlot,
        boolean returnItem,
        int returnSlot,
        ItemStack returnStack
) implements CustomPacketPayload {

    public static final Identifier RESTOCK_PAYLOAD_ID = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "restock_item");
    public static final Type<@NotNull RestockerItemPacket> TYPE = new Type<>(RESTOCK_PAYLOAD_ID);

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull RestockerItemPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, RestockerItemPacket packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.handSlot);
            ByteBufCodecs.VAR_INT.encode(buf, packet.sourceSlot);

            boolean actuallyHasReturn = packet.returnItem && !packet.returnStack.isEmpty();
            ByteBufCodecs.BOOL.encode(buf, actuallyHasReturn);
            ByteBufCodecs.VAR_INT.encode(buf, packet.returnSlot);

            if (actuallyHasReturn) {
                ItemStack.STREAM_CODEC.encode(buf, packet.returnStack);
            }
        }

        @Override
        public RestockerItemPacket decode(RegistryFriendlyByteBuf buf) {
            int handSlot = ByteBufCodecs.VAR_INT.decode(buf);
            int sourceSlot = ByteBufCodecs.VAR_INT.decode(buf);
            boolean returnItem = ByteBufCodecs.BOOL.decode(buf);
            int returnSlot = ByteBufCodecs.VAR_INT.decode(buf);

            ItemStack returnStack = returnItem ? ItemStack.STREAM_CODEC.decode(buf) : ItemStack.EMPTY;

            return new RestockerItemPacket(handSlot, sourceSlot, returnItem, returnSlot, returnStack);
        }
    };

    @Override
    public @NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return TYPE;
    }
}