package cn.academy.client.sound;

import cn.academy.AcademyCraft;
import cn.academy.config.Property;
import cn.academy.event.ConfigModifyEvent;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class ACSoundVolume {

    public static final String CATEGORY = "generic", KEY = "soundVolume";

    private static Property prop;

    private static float scale = 1.0f;

    private ACSoundVolume() {}

    public static void init() {
        prop = AcademyCraft.config.get(CATEGORY, KEY, 100,
                "Volume of AcademyCraft's own sounds, in percent (0 = mute, 100 = unchanged). "
                        + "Applies on top of Minecraft's own category volumes.");
        refresh();
        MinecraftForge.EVENT_BUS.register(new ACSoundVolume());
    }

    public static float scale() {
        return scale;
    }

    private static void refresh() {
        scale = prop == null ? 1.0f : Math.max(0, Math.min(100, prop.getInt())) / 100f;
    }

    @SubscribeEvent
    public void onConfigModify(ConfigModifyEvent event) {
        refresh();
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (scale >= 1.0f) {
            return;
        }
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }
        ResourceLocation loc = sound.getLocation();
        if (loc == null || !AcademyCraft.MODID.equals(loc.getNamespace())) {
            return;
        }
        event.setSound(sound instanceof net.minecraft.client.resources.sounds.TickableSoundInstance t
                ? new Tickable(t) : new Scaled(sound));
    }

    private static class Scaled implements SoundInstance {

        final SoundInstance inner;

        Scaled(SoundInstance inner) {
            this.inner = inner;
        }

        @Override
        public float getVolume() {
            return inner.getVolume() * scale;
        }

        @Override
        public ResourceLocation getLocation() {
            return inner.getLocation();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            return inner.resolve(manager);
        }

        @Override
        public Sound getSound() {
            return inner.getSound();
        }

        @Override
        public SoundSource getSource() {
            return inner.getSource();
        }

        @Override
        public boolean isLooping() {
            return inner.isLooping();
        }

        @Override
        public boolean isRelative() {
            return inner.isRelative();
        }

        @Override
        public int getDelay() {
            return inner.getDelay();
        }

        @Override
        public float getPitch() {
            return inner.getPitch();
        }

        @Override
        public double getX() {
            return inner.getX();
        }

        @Override
        public double getY() {
            return inner.getY();
        }

        @Override
        public double getZ() {
            return inner.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return inner.getAttenuation();
        }

        @Override
        public boolean canStartSilent() {
            return inner.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return inner.canPlaySound();
        }

        @Override
        public java.util.concurrent.CompletableFuture<net.minecraft.client.sounds.AudioStream> getStream(
                net.minecraft.client.sounds.SoundBufferLibrary library, Sound sound, boolean looping) {
            return inner.getStream(library, sound, looping);
        }
    }

    private static final class Tickable extends Scaled
            implements net.minecraft.client.resources.sounds.TickableSoundInstance {

        Tickable(net.minecraft.client.resources.sounds.TickableSoundInstance inner) {
            super(inner);
        }

        @Override
        public boolean isStopped() {
            return ((net.minecraft.client.resources.sounds.TickableSoundInstance) inner).isStopped();
        }

        @Override
        public void tick() {
            ((net.minecraft.client.resources.sounds.TickableSoundInstance) inner).tick();
        }
    }

}
