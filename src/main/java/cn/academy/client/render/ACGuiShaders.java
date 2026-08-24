package cn.academy.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public final class ACGuiShaders {

    private static ShaderInstance skillProgBar;

    private static ShaderInstance mono;

    private static ShaderInstance guiCutout;

    private ACGuiShaders() {}

    public static void register(IEventBus modBus) {
        modBus.register(ACGuiShaders.class);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent e) throws IOException {

        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation("academy", "ac_skill_progbar"), DefaultVertexFormat.POSITION_TEX),
                s -> skillProgBar = s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation("academy", "ac_mono"), DefaultVertexFormat.POSITION_TEX),
                s -> mono = s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                new ResourceLocation("academy", "ac_gui_cutout"), DefaultVertexFormat.POSITION_TEX),
                s -> guiCutout = s);
    }

    public static ShaderInstance skillProgBar() { return skillProgBar; }

    public static ShaderInstance mono() { return mono; }

    public static ShaderInstance guiCutout() { return guiCutout; }

    public static boolean ready() {
        return skillProgBar != null && mono != null && guiCutout != null;
    }
}
