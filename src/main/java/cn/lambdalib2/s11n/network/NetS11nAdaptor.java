package cn.lambdalib2.s11n.network;

import net.minecraft.network.FriendlyByteBuf;

public interface NetS11nAdaptor<T> {
    void write(FriendlyByteBuf buf, T obj);

    T read(FriendlyByteBuf buf);
}
