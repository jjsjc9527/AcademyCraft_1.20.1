package cn.academy.client.gui.developer;

import cn.academy.ability.AbilityLocalization;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.KeyEvent;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@OnlyIn(Dist.CLIENT)
public class Console extends Component {

    private static final int MAX_LINES = 10;
    private static final FontOption FO = new FontOption(8);
    private static final String CONSOLE_HEAD = "OS >";

    public static String localized(String id, Object... args) {

        return AbilityLocalization.instance.local("console." + id, args).replace("\\n", "\n");
    }

    private static IFont font() {
        return Fonts.getDefault();
    }

    private final Task inputTask = new Task() {
        @Override
        public void begin() {
            output(CONSOLE_HEAD);
        }

        @Override
        public boolean isFinished() {
            return false;
        }
    };

    private final List<Command> commands = new ArrayList<>();
    private final Deque<String> outputs = new LinkedList<>();
    private final Queue<Task> taskQueue = new LinkedList<>();
    private Task currentTask = null;
    private String input = "";

    public Console(boolean emergency, boolean hasDeveloper) {
        super("Console");

        String playerName = Minecraft.getInstance().player == null
                ? "" : Minecraft.getInstance().player.getName().getString();
        enqueue(slowPrintTask(localized("init", playerName)));
        pause(0.4);

        List<String> numSeq = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            numSeq.add((i * 10 + RandUtils.nextInt(6) - 3) + "%");
        }
        numSeq.add((64 + RandUtils.nextInt(4)) + "%");
        numSeq.add(localized("boot_failed"));
        animSequence(0.3, numSeq);

        String startupText;
        if (emergency) {
            startupText = localized("override");
        } else if (hasDeveloper) {
            startupText = localized("invalid_cat") + localized("learn_hint");
        } else {
            startupText = localized("invalid_cat");
        }
        enqueue(slowPrintTask(startupText));

        listen(FrameEvent.class, (w, e) -> {
            if (currentTask != null && currentTask.isFinished()) {
                currentTask.finish();
                currentTask = null;
            }
            if (currentTask != null) {
                currentTask.update();
            }
            if (currentTask == null) {
                currentTask = taskQueue.isEmpty() ? inputTask : taskQueue.remove();
                currentTask.begin();
            }

            float x = 5, y = 5;
            int idx = 0, last = outputs.size() - 1;
            for (String line : outputs) {
                if (idx == last && currentTask == inputTask) {

                    String caret = ((int) (GameTimer.getTime() * 1000)) % 1000 < 500 ? "_" : "";
                    font().draw(line + input + caret, x, y, FO);
                } else {
                    font().draw(line, x, y, FO);
                }
                y += 10;
                idx++;
            }

            if (w.getGui().getWidget("link_page") == null) {
                w.gainFocus();
            }
        });

        listen(KeyEvent.class, (w, e) -> {
            if (e.keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!input.isEmpty()) {
                    input = input.substring(0, input.length() - 1);
                }
            } else if (e.keyCode == GLFW.GLFW_KEY_ENTER || e.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                parseCommand(input);
                input = "";
            } else if (e.inputChar != '\0' && SharedConstants.isAllowedChatCharacter(e.inputChar)) {
                input += e.inputChar;
            }
        });
    }

    public void enqueue(Task task) {
        taskQueue.offer(task);

        if (currentTask == inputTask) {
            outputln(input);
            currentTask = null;
        }
    }

    public void output(String content) {
        StringBuilder current = new StringBuilder(outputs.isEmpty() ? "" : outputs.removeLast());
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '\b') {
                current.setLength(Math.max(0, current.length() - 1));
            } else if (ch == '\n') {
                outputs.addLast(current.toString());
                outputs.addLast("");
                current = new StringBuilder(outputs.removeLast());
            } else {
                current.append(ch);
            }
        }
        outputs.addLast(current.toString());
        while (outputs.size() > MAX_LINES) {
            outputs.removeFirst();
        }
    }

    public void outputln(String content) {
        output(content + '\n');
    }

    public void outputln() {
        output("\n");
    }

    public void animSequence(double time, List<String> strs) {
        for (int i = 0; i < strs.size(); i++) {
            String s = strs.get(i);
            boolean isLast = i == strs.size() - 1;
            enqueue(new TimedTask(time) {
                @Override
                public void begin() {
                    super.begin();
                    output(s);
                }

                @Override
                public void finish() {
                    if (!isLast) {
                        output("\b".repeat(s.length()));
                    }
                }
            });
        }
    }

    public void pause(double time) {
        enqueue(new TimedTask(time));
    }

    public void enqueueRebuild() {
        enqueue(new Task() {
            @Override
            public void begin() {
                widget.getGui().postEvent(new RebuildEvent());
            }

            @Override
            public boolean isFinished() {
                return true;
            }
        });
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    private void parseCommand(String cmd) {
        for (Command c : commands) {
            if (c.name().equals(cmd)) {
                c.callback().run();
                return;
            }
        }
        enqueue(printTask(localized("invalid_command")));
    }

    public Task printTask(String str) {
        return new Task() {
            @Override
            public void begin() {
                output(str);
            }

            @Override
            public boolean isFinished() {
                return true;
            }
        };
    }

    public Task slowPrintTask(String str) {
        return new Task() {
            static final double PER_CHAR_TIME = 0.01;
            int idx = 0;
            double last = -1;

            @Override
            public void begin() {
                last = GameTimer.getTime();
            }

            @Override
            public void update() {
                double time = GameTimer.getTime();
                int n = (int) ((time - last) / PER_CHAR_TIME);
                if (n > 0) {
                    int end = Math.min(str.length(), idx + n);
                    output(str.substring(idx, end));
                    last += n * PER_CHAR_TIME;
                    idx = end;
                }
            }

            @Override
            public boolean isFinished() {
                return idx == str.length();
            }
        };
    }

    public interface Task {
        default void begin() {}

        default void update() {}

        default void finish() {}

        boolean isFinished();
    }

    public static class TimedTask implements Task {
        private final double life;
        private double creationTime = -1;

        public TimedTask(double life) {
            this.life = life;
        }

        public double getCreationTime() {
            return creationTime;
        }

        @Override
        public void begin() {
            creationTime = GameTimer.getTime();
        }

        @Override
        public boolean isFinished() {
            return (GameTimer.getTime() - creationTime) >= life;
        }
    }

    public record Command(String name, Runnable callback) {}
}
