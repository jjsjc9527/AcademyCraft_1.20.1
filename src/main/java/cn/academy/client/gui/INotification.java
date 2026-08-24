package cn.academy.client.gui;

import net.minecraft.resources.ResourceLocation;

public interface INotification {

    ResourceLocation getIcon();

    String getTitle();

    String getContent();

}
