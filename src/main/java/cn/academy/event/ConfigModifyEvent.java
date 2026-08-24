package cn.academy.event;

import cn.academy.config.Property;
import net.minecraftforge.eventbus.api.Event;

public class ConfigModifyEvent extends Event {

    public final Property property;

    public ConfigModifyEvent(Property _prop) {
        property = _prop;
    }
}
