package cn.academy.tutorial.client;

import cn.academy.tutorial.TutorialData;
import cn.lambdalib2.util.markdown.GLMarkdownRenderer;
import cn.lambdalib2.util.markdown.MarkdownParser.Attribute;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;

public class ACMarkdownRenderer extends GLMarkdownRenderer {

    @Override
    public void onTag(String name, Map<String, String> attr) {
        super.onTag(name, attr);

        if (name.equals("key")) {
            renderRef(keyLookup(attr.get("id")));
        }

        if (name.equals("misakaname")) {
            Player p = Minecraft.getInstance().player;
            int id = p == null ? 0 : TutorialData.get(p).getMisakaID();
            String misaka = Component.translatable("ac.tutorial.misaka", id).getString();
            onTextContent(misaka, Set.of(Attribute.STRONG));
        }
    }

    private void renderRef(String keyName) {
        onTextContent(keyName, Set.of(Attribute.REFERENCE));
    }

    private String keyLookup(String keyid) {
        return keyid == null ? "???" : keyid;
    }

}
