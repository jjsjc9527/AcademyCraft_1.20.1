package cn.academy.terminal;

import cn.academy.terminal.app.AppFreqTransmitter;
import cn.academy.terminal.app.AppSkillTree;
import cn.academy.terminal.app.AppTutorial;
import cn.academy.terminal.app.settings.AppSettings;

public final class ACApps {

    private ACApps() {}

    public static void register() {
        AppRegistry.register(AppSkillTree.instance);

        AppRegistry.register(AppFreqTransmitter.instance);

        AppRegistry.register(AppTutorial.instance);

        AppRegistry.register(AppSettings.instance);

    }

}
