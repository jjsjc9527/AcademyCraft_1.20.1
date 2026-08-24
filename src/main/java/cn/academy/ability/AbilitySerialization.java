package cn.academy.ability;

import cn.academy.datapart.PresetData;
import cn.lambdalib2.s11n.network.NetS11nAdaptor;
import cn.lambdalib2.s11n.network.NetworkS11n;
import net.minecraft.network.FriendlyByteBuf;

public final class AbilitySerialization {

    private AbilitySerialization() {}

    public static void register() {
        NetworkS11n.addDirect(Skill.class, new NetS11nAdaptor<Skill>() {
            @Override
            public void write(FriendlyByteBuf buf, Skill skill) {
                buf.writeByte(skill.getCategory().getCategoryID());
                buf.writeByte(skill.getID());
            }

            @Override
            public Skill read(FriendlyByteBuf buf) {
                int catID = buf.readByte();
                int skillID = buf.readByte();
                Category c = CategoryManager.INSTANCE.getCategory(catID);
                return c.getSkill(skillID);
            }
        });

        NetworkS11n.addDirect(Controllable.class, new NetS11nAdaptor<Controllable>() {
            @Override
            public void write(FriendlyByteBuf buf, Controllable obj) {
                buf.writeByte(obj.getCategory().getCategoryID());
                buf.writeByte(obj.getControlID());
            }

            @Override
            public Controllable read(FriendlyByteBuf buf) {
                int catID = buf.readByte();
                int ctrlID = buf.readByte();
                Category c = CategoryManager.INSTANCE.getCategory(catID);
                return c == null ? null : c.getControllable(ctrlID);
            }
        });

        NetworkS11n.addDirect(PresetData.Preset.class, new NetS11nAdaptor<PresetData.Preset>() {
            @Override
            public void write(FriendlyByteBuf buf, PresetData.Preset obj) {
                PresetData.writePresetBuf(buf, obj);
            }

            @Override
            public PresetData.Preset read(FriendlyByteBuf buf) {
                return PresetData.readPresetBuf(buf);
            }
        });
    }
}
