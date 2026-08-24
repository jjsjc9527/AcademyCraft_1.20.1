package cn.academy.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.IOException;

@OnlyIn(Dist.CLIENT)
public final class ACEffectShaders {

    private static ShaderInstance simple;

    private static ShaderInstance cpbarCp;

    private static ShaderInstance cpbarOverload;

    private static ShaderInstance plasmaBody;

    private static ShaderInstance sonicWave;

    private static ShaderInstance gravityLens;

    private ACEffectShaders() {}

    public static void register(IEventBus modBus) {
        modBus.register(ACEffectShaders.class);
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent e) throws IOException {

        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                        new ResourceLocation("academy", "ac_simple"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> simple = s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                        new ResourceLocation("academy", "ac_cpbar_cp"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> cpbarCp = s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                        new ResourceLocation("academy", "ac_cpbar_overload"), DefaultVertexFormat.POSITION_TEX_COLOR),
                s -> cpbarOverload = s);

        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                        new ResourceLocation("academy", "ac_plasma_body"), DefaultVertexFormat.POSITION_TEX),
                s -> plasmaBody = s);

        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                        new ResourceLocation("academy", "ac_sonic_wave"), DefaultVertexFormat.PARTICLE),
                s -> sonicWave = s);

        e.registerShader(new ShaderInstance(e.getResourceProvider(),
                        new ResourceLocation("academy", "ac_gravity_lens"), DefaultVertexFormat.POSITION),
                s -> gravityLens = s);
    }

    public static ShaderInstance simple() {
        return simple;
    }

    public static ShaderInstance simpleForWorld() {
        return simple;
    }

    public static ShaderInstance cpbarCp() {
        return cpbarCp;
    }

    public static ShaderInstance cpbarOverload() {
        return cpbarOverload;
    }

    public static ShaderInstance plasmaBody() {
        return plasmaBody;
    }

    public static ShaderInstance sonicWave() {
        return sonicWave;
    }

    public static ShaderInstance gravityLens() {
        return gravityLens;
    }
}
