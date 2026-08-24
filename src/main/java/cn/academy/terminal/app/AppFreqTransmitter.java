package cn.academy.terminal.app;

import cn.academy.client.auxgui.FreqTransmitterUI;
import cn.academy.client.auxgui.TerminalUI;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AppFreqTransmitter extends App {

    public static AppFreqTransmitter instance = new AppFreqTransmitter();

    private AppFreqTransmitter() {
        super("freq_transmitter");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void onStart() {
                TerminalUI.passOn(new FreqTransmitterUI());
            }
        };
    }
}
