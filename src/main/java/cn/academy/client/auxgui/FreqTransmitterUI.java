package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.block.block.ACMultiBlock;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.network.FreqTransmitterActionMessage;
import cn.academy.network.FreqTransmitterResultMessage;
import cn.academy.terminal.app.AppFreqTransmitter;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.Extent;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.ControlOverrider;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class FreqTransmitterUI extends AuxGui {

    private static final String OVERRIDE_GROUP = "AC_FreqTransmitter";

    private static final Color
            BG_COLOR = Colors.fromHexColor(0x77272727),
            GLOW_COLOR = Colors.fromHexColor(0xaaffffff);

    private static final double GLOW_SIZE = 1;

    private static final double REACH = 4.0;

    final IFont font = Resources.font();

    final Player player;
    final Level level;

    State current;

    private boolean completeOverriding;

    public FreqTransmitterUI() {
        player = Minecraft.getInstance().player;
        level = player.level();
        consistent = false;

        MinecraftForge.EVENT_BUS.register(this);

        setState(new StateStart());

        ControlOverrider.override(OVERRIDE_GROUP, KeyManager.MOUSE_LEFT, KeyManager.MOUSE_RIGHT);
    }

    private void setState(State next) {
        if (next == null) {
            dispose();
            if (completeOverriding) {
                ControlOverrider.endCompleteOverride();
                completeOverriding = false;
            }
        } else {
            if (current != null && current.handlesKeyInput() && completeOverriding) {
                ControlOverrider.endCompleteOverride();
                completeOverriding = false;
            }
            if (next.handlesKeyInput() && !completeOverriding) {
                ControlOverrider.startCompleteOverride();
                completeOverriding = true;
            }
        }
        current = next;
    }

    private static String local(String key) {
        return I18n.get("app.academy.freq_transmitter." + key);
    }

    @Override
    public void onDisposed() {
        MinecraftForge.EVENT_BUS.unregister(this);
        ControlOverrider.endOverride(OVERRIDE_GROUP);
        if (completeOverriding) {
            ControlOverrider.endCompleteOverride();
            completeOverriding = false;
        }
        FreqTransmitterResultMessage.clearPending();
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        HudUtils.setPose(gg.pose());

        AppFreqTransmitter app = AppFreqTransmitter.instance;

        {
            final float x0 = 15, y0 = 15, isize = 18;
            final FontOption option = new FontOption(10);
            String str = app.getDisplayName();
            double len = font.getTextWidth(str, option);

            drawBox(x0, y0, 30 + len, 18);

            RenderSystem.setShaderColor(1, 1, 1, 1);
            HudUtils.loadTexture(app.getIcon());
            HudUtils.rect(x0 + 2, y0, isize, isize);

            font.draw(str, x0 + isize + 6, y0 + 4, option);
        }

        current.handleDraw(width, height);

        long dt = current.getDeltaTime();
        if (dt > current.timeout) {
            setState(new StateNotifyAndQuit("st"));
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static void drawBox(double x, double y, double width, double height) {
        Colors.bindToGL(BG_COLOR);
        HudUtils.colorRect(x, y, width, height);
        ACRenderingHelper.drawGlow(x, y, width, height, GLOW_SIZE, GLOW_COLOR);
    }

    private void drawTextBox(String str, float x, float y) {
        final float trimLength = 120;
        final FontOption option = new FontOption(10);
        Extent extent = font.drawSeperated_Sim(str, trimLength, option);
        final float MARGIN = 5;

        drawBox(x, y, MARGIN * 2 + extent.width + 25, MARGIN * 2 + extent.height);
        font.drawSeperated(str, x + MARGIN, y + MARGIN, trimLength, option);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(InputEvent.MouseButton.Pre event) {
        if (disposed || current == null || !cn.lambdalib2.datapart.EntityData.isLocalPlayerReady()) return;
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && event.getAction() == GLFW.GLFW_PRESS) {
            current.handleClicking(raytrace());
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key event) {
        if (disposed || current == null || !cn.lambdalib2.datapart.EntityData.isLocalPlayerReady()) return;
        if (event.getAction() == GLFW.GLFW_RELEASE) return;
        if (!current.handlesKeyInput()) return;

        int key = event.getKey();
        char ch = resolveChar(key, event.getScanCode(), event.getModifiers());
        current.handleKeyInput(ch, key);

        ControlOverrider.suppressAllNow();
    }

    private static char resolveChar(int key, int scancode, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE) return ' ';
        String name = GLFW.glfwGetKeyName(key, scancode);
        if (name != null && name.length() == 1) {
            char c = name.charAt(0);
            if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) c = applyShift(c);
            return c;
        }
        return '\0';
    }

    private static char applyShift(char c) {
        if (c >= 'a' && c <= 'z') return Character.toUpperCase(c);
        return switch (c) {
            case '1' -> '!'; case '2' -> '@'; case '3' -> '#'; case '4' -> '$'; case '5' -> '%';
            case '6' -> '^'; case '7' -> '&'; case '8' -> '*'; case '9' -> '('; case '0' -> ')';
            case '-' -> '_'; case '=' -> '+'; case '[' -> '{'; case ']' -> '}'; case '\\' -> '|';
            case ';' -> ':'; case '\'' -> '"'; case ',' -> '<'; case '.' -> '>'; case '/' -> '?';
            case '`' -> '~';
            default -> c;
        };
    }

    private BlockHitResult raytrace() {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(REACH));
        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private BlockEntity resolve(BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) return be;
        if (level.getBlockState(pos).getBlock() instanceof ACMultiBlock) {
            return ACMultiBlock.getOriginTile(level, pos);
        }
        return null;
    }

    private abstract class State {
        final boolean handlesKey;
        final double createTime;
        long timeout = 20000;

        State(boolean handlesKey) {
            this.handlesKey = handlesKey;
            this.createTime = GameTimer.getTime();
        }

        final boolean handlesKeyInput() {
            return handlesKey;
        }

        abstract void handleDraw(float w, float h);

        abstract void handleClicking(BlockHitResult result);

        void handleKeyInput(char ch, int key) {}

        final long getDeltaTime() {
            return (long) ((GameTimer.getTime() - createTime) * 1000);
        }

        final void startTransmitting() {
            timeout = 3000;
        }
    }

    private class StateNotify extends State {
        final String key;

        StateNotify(String key) {
            super(false);
            this.key = key;
        }

        @Override void handleDraw(float w, float h) {
            drawTextBox(local(key), w / 2 + 10, h / 2 + 10);
        }

        @Override void handleClicking(BlockHitResult result) {}
    }

    private class StateNotifyAndQuit extends StateNotify {
        StateNotifyAndQuit(String key) { super(key); }

        @Override void handleDraw(float w, float h) {
            super.handleDraw(w, h);
            if (getDeltaTime() > 1000L) dispose();
        }
    }

    private class StateNotifyAndReturn extends StateNotify {
        final State toSwitch;

        StateNotifyAndReturn(String key, State toSwitch) {
            super(key);
            this.toSwitch = toSwitch;
        }

        @Override void handleDraw(float w, float h) {
            super.handleDraw(w, h);
            if (getDeltaTime() > 700L) setState(toSwitch);
        }
    }

    private class StateStart extends State {
        boolean started = false;

        StateStart() { super(false); }

        @Override void handleDraw(float w, float h) {
            drawTextBox(local("s0_0"), w / 2 + 10, h / 2 + 10);
        }

        @Override void handleClicking(BlockHitResult result) {
            if (result == null) {
                setState(null);
                return;
            }
            if (started) return;

            BlockEntity te = resolve(result.getBlockPos());
            if (te instanceof IWirelessNode) {
                setState(new StateAuthorizeNode((IWirelessNode) te, te.getBlockPos()));
            } else if (te instanceof IWirelessMatrix) {
                started = true;
                BlockPos matrixPos = te.getBlockPos();
                startTransmitting();
                FreqTransmitterActionMessage.send(FreqTransmitterActionMessage.QUERY_SSID,
                        matrixPos, BlockPos.ZERO, "", (success, ssid) -> {
                            if (current == StateStart.this) {
                                if (!success) {
                                    setState(new StateNotifyAndQuit("e0"));
                                } else {
                                    setState(new StateAuthorize(matrixPos, ssid));
                                }
                            }
                        });
            } else {
                setState(new StateNotifyAndQuit("e4"));
            }
        }
    }

    private class StateAuthorize extends State {
        final BlockPos matrixPos;
        final String ssid;
        String pass = "";

        StateAuthorize(BlockPos matrixPos, String ssid) {
            super(true);
            this.matrixPos = matrixPos;
            this.ssid = ssid;
        }

        @Override void handleDraw(float w, float h) {
            drawPasswordBox(w, h, "SSID: " + ssid, pass, local("s1_0"));
        }

        @Override void handleClicking(BlockHitResult result) {}

        @Override void handleKeyInput(char ch, int key) {
            if (SharedConstants.isAllowedChatCharacter(ch)) {
                pass += ch;
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                State state = new StateNotify("s1_1");
                setState(state);
                state.startTransmitting();
                FreqTransmitterActionMessage.send(FreqTransmitterActionMessage.AUTH_MATRIX,
                        matrixPos, BlockPos.ZERO, pass, (success, ignored) -> {
                            if (state == current) {
                                if (success) setState(new StateDoMatrixLink(matrixPos, pass));
                                else setState(new StateNotifyAndQuit("e1"));
                            }
                        });
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!pass.isEmpty()) pass = pass.substring(0, pass.length() - 1);
            }
        }
    }

    private class StateAuthorizeNode extends State {
        final BlockPos nodePos;
        final String name;
        String pass = "";

        StateAuthorizeNode(IWirelessNode node, BlockPos nodePos) {
            super(true);
            this.nodePos = nodePos;
            this.name = node.getNodeName();
        }

        @Override void handleDraw(float w, float h) {

            drawPasswordBox(w, h, "NAME: " + name, pass, local("s1_0"));
        }

        @Override void handleClicking(BlockHitResult result) {}

        @Override void handleKeyInput(char ch, int key) {
            if (SharedConstants.isAllowedChatCharacter(ch)) {
                pass += ch;
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                State state = new StateNotify("s1_1");
                setState(state);
                state.startTransmitting();
                FreqTransmitterActionMessage.send(FreqTransmitterActionMessage.AUTH_NODE,
                        nodePos, BlockPos.ZERO, pass, (success, ignored) -> {
                            if (state == current) {
                                if (success) setState(new StateDoNodeLink(nodePos, pass));
                                else setState(new StateNotifyAndQuit("e1"));
                            }
                        });
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!pass.isEmpty()) pass = pass.substring(0, pass.length() - 1);
            }
        }
    }

    private void drawPasswordBox(float w, float h, String head, String pass, String hint) {
        float x = w / 2 + 10, y = h / 2 - 10;
        drawBox(x, y, 140, 40);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pass.length(); ++i) sb.append('*');

        font.draw(head, x + 10, y + 5, new FontOption(10, 0xffbfbfbf));
        font.draw("PASS: " + sb, x + 10, y + 15, new FontOption(10, 0xffffffff));
        font.draw(hint, x + 10, y + 25, new FontOption(10, 0xff30ffff));
    }

    private class StateDoMatrixLink extends State {
        final BlockPos matrixPos;
        final String pass;

        StateDoMatrixLink(BlockPos matrixPos, String pass) {
            super(false);
            this.matrixPos = matrixPos;
            this.pass = pass;
        }

        @Override void handleDraw(float w, float h) {
            drawTextBox(local("s2_0"), w / 2 + 10, h / 2 + 10);
        }

        @Override void handleClicking(BlockHitResult result) {
            BlockEntity te;
            if (result == null || !((te = resolve(result.getBlockPos())) instanceof IWirelessNode)) {
                setState(new StateNotifyAndQuit("e4"));
            } else {
                BlockPos nodePos = te.getBlockPos();
                State state = new StateNotify("e5");
                setState(state);
                state.startTransmitting();
                FreqTransmitterActionMessage.send(FreqTransmitterActionMessage.LINK_NODE,
                        nodePos, matrixPos, pass, (success, ignored) -> {
                            if (current == state) {
                                if (success) setState(new StateNotifyAndReturn("e6", StateDoMatrixLink.this));
                                else setState(new StateNotifyAndQuit("e2"));
                            }
                        });
            }
        }
    }

    private class StateDoNodeLink extends State {
        final BlockPos nodePos;
        final String pass;

        StateDoNodeLink(BlockPos nodePos, String pass) {
            super(false);
            this.nodePos = nodePos;
            this.pass = pass;
        }

        @Override void handleDraw(float w, float h) {
            drawTextBox(local("s3_0"), w / 2 + 10, h / 2 + 10);
        }

        @Override void handleClicking(BlockHitResult result) {
            BlockEntity te;
            if (result == null || (te = resolve(result.getBlockPos())) == null) {
                setState(new StateNotifyAndQuit("e4"));
                return;
            }
            if (te instanceof IWirelessUser) {
                BlockPos userPos = te.getBlockPos();
                State state = new StateNotify("e5");
                setState(state);
                state.startTransmitting();
                FreqTransmitterActionMessage.send(FreqTransmitterActionMessage.LINK_USER,
                        userPos, nodePos, "", (success, ignored) -> {
                            if (current == state) {
                                if (success) setState(new StateNotifyAndReturn("e6", StateDoNodeLink.this));
                                else setState(new StateNotifyAndQuit("e3"));
                            }
                        });
            } else {
                setState(new StateNotifyAndQuit("e4"));
            }
        }
    }
}
