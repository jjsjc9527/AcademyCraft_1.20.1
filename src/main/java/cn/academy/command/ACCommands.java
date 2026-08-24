package cn.academy.command;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.datapart.CooldownData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.List;

public final class ACCommands {

    private ACCommands() {}

    public static final int PERM_LEVEL = 4;

    private static LiteralArgumentBuilder<CommandSourceStack> op(String name) {
        return Commands.literal(name).requires(src -> src.hasPermission(PERM_LEVEL));
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(op("acset")

                .then(Commands.argument("category", StringArgumentType.greedyString())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                CategoryManager.INSTANCE.getCategories().stream().map(Category::getName), b))
                        .executes(c -> setCategory(c.getSource(),
                                StringArgumentType.getString(c, "category").trim())))
                .executes(c -> showCategories(c.getSource())));

        event.getDispatcher().register(
                op("acmaxexp").executes(ctx -> maxExp(ctx.getSource())));

        event.getDispatcher().register(op("ackill")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(c -> ackill(c.getSource(),
                                EntityArgument.getEntities(c, "targets"))))
                .executes(c -> ackill(c.getSource(),
                        java.util.List.of(c.getSource().getEntityOrException()))));

        event.getDispatcher().register(
                op("acnocd").executes(c -> toggleNoCd(c.getSource())));

        event.getDispatcher().register(
                op("aclearnall").executes(c -> learnAll(c.getSource())));

        event.getDispatcher().register(
                op("aclevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                .executes(c -> setLevel(c.getSource(),
                                        IntegerArgumentType.getInteger(c, "level"))))
                        .executes(c -> showLevel(c.getSource())));

        event.getDispatcher().register(
                op("aclevelexp")

                        .then(Commands.argument("percent",
                                        com.mojang.brigadier.arguments.FloatArgumentType.floatArg(0, 100))
                                .executes(c -> setLevelProgress(c.getSource(),
                                        com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "percent"))))
                        .executes(c -> showLevel(c.getSource())));

    }

    private static int showCategories(CommandSourceStack source) throws CommandSyntaxException {
        AbilityData aData = AbilityData.get(source.getPlayerOrException());
        String cur = aData.hasCategory()
                ? aData.getCategory().getDisplayName() + "(" + aData.getCategory().getName() + ")"
                : "无";
        source.sendSuccess(() -> Component.literal(
                "当前能力系:" + cur + "\n可选:" + categoryNames() + "\n用法:/acset <能力名>"), false);
        return 1;
    }

    private static int setCategory(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        Category cat = CategoryManager.INSTANCE.getCategory(name);
        if (cat == null) {
            for (Category c : CategoryManager.INSTANCE.getCategories()) {
                if (c.getName().equalsIgnoreCase(name) || c.getDisplayName().equals(name)) {
                    cat = c;
                    break;
                }
            }
        }
        if (cat == null) {
            source.sendFailure(Component.literal(
                    "没有名叫「" + name + "」的能力系 —— 可选:" + categoryNames()));
            return 0;
        }

        AbilityData aData = AbilityData.get(p);
        boolean same = aData.hasCategory() && aData.getCategory() == cat;
        aData.setCategory(cat);
        Skill root = cat.getSkillCount() > 0 ? cat.getSkill(0) : null;
        if (root != null && !aData.isSkillLearned(root)) {
            aData.learnSkill(root);
        }

        CPData.get(p).setActivateState(true, cn.academy.datapart.AbilityToggleSource.COMMAND);

        final Category fc = cat;
        final Skill fr = root;
        source.sendSuccess(() -> Component.literal(same
                ? "本来就是「" + fc.getDisplayName() + "」—— 未做任何重置"
                : "能力系已换成「" + fc.getDisplayName() + "」(" + fc.getName() + ")"
                        + ",已学技能与熟练度已清空"
                        + (fr == null ? "" : ",并学会根技能「" + fr.getDisplayName() + "」")
                        + ";要全部技能用 /aclearnall"), false);
        return 1;
    }

    private static String categoryNames() {
        StringBuilder sb = new StringBuilder();
        for (Category c : CategoryManager.INSTANCE.getCategories()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(c.getName()).append("(").append(c.getDisplayName()).append(")");
        }
        return sb.length() == 0 ? "(无)" : sb.toString();
    }

    private static int showLevel(CommandSourceStack source) throws CommandSyntaxException {
        AbilityData aData = AbilityData.get(source.getPlayerOrException());
        if (!aData.hasCategory()) {
            source.sendFailure(Component.literal("当前没有能力系 —— 先用开发机 learn 或 /acset <能力名>"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(String.format(
                "当前等级:%d / 5,本级进度:%.1f%%%n用法:/aclevel <1-5> · /aclevelexp <0-100>",
                aData.getLevel(), aData.getLevelProgress() * 100)), false);
        return 1;
    }

    private static int setLevel(CommandSourceStack source, int lv) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        AbilityData aData = AbilityData.get(p);
        if (!aData.hasCategory()) {
            source.sendFailure(Component.literal("当前没有能力系 —— 先用开发机 learn 或 /acset <能力名>"));
            return 0;
        }
        int old = aData.getLevel();
        aData.setLevel(lv);
        source.sendSuccess(() -> Component.literal(old == lv
                ? "本来就是 " + lv + " 级 —— 未做任何改动(本级进度也没动)"
                : "能力等级:" + old + " → " + lv + "(本级进度已清零,要设进度用 /aclevelexp)"), false);
        return lv;
    }

    private static int setLevelProgress(CommandSourceStack source, float percent)
            throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        AbilityData aData = AbilityData.get(p);
        if (!aData.hasCategory()) {
            source.sendFailure(Component.literal("当前没有能力系 —— 先用开发机 learn 或 /acset <能力名>"));
            return 0;
        }
        if (!aData.setLevelProgress(percent / 100f)) {
            source.sendFailure(Component.literal(String.format(
                    "设不了:当前 %d 级没有可计入进度的【可控】技能,本级进度恒为 100%%",
                    aData.getLevel())));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(String.format(
                "本级(%d 级)进度已设为 %.1f%%%s",
                aData.getLevel(), aData.getLevelProgress() * 100,
                aData.canLevelUp() ? " —— 已满,可以升级了" : "")), false);
        return (int) percent;
    }

    private static int ackill(CommandSourceStack source,
                              java.util.Collection<? extends net.minecraft.world.entity.Entity> targets) {
        int n = 0;
        for (net.minecraft.world.entity.Entity e : targets) {
            if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                cn.academy.api.ACImmortal.beginForceKill(le);
            }
            e.kill();
            n++;
        }
        final int killed = n;
        source.sendSuccess(() -> Component.literal(killed == 1
                ? "已处决 1 个实体(穿透常驻免疫)"
                : String.format("已处决 %d 个实体(穿透常驻免疫)", killed)), true);
        return n;
    }

    private static int toggleNoCd(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        CooldownData cd = CooldownData.of(p);
        boolean on = !cd.isNoCooldown();
        cd.setNoCooldown(on);
        source.sendSuccess(() -> Component.literal(on
                ? "所有技能 0CD【开启】—— 冷却已清空,期间技能秒放;再执行一次关闭"
                : "所有技能 0CD【关闭】"), false);
        return 1;
    }

    private static int learnAll(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        AbilityData aData = AbilityData.get(p);
        if (!aData.hasCategory()) {
            source.sendFailure(Component.literal("当前没有能力系 —— 先用开发机 learn 或 /acset <能力名> 获得能力"));
            return 0;
        }
        int learned = 0;
        int skipped = 0;
        int locked = 0;
        for (Skill s : aData.getCategory().getSkillList()) {
            if (!s.isEnabled()) {
                skipped++;
                continue;
            }

            if (s.isNeverLearnable()) {
                locked++;
                continue;
            }
            if (!aData.isSkillLearned(s)) {
                aData.learnSkill(s);
                learned++;
            }
        }
        int count = learned;
        int hidden = skipped;
        int shut = locked;

        source.sendSuccess(() -> Component.literal(
                "已学会「" + aData.getCategory().getDisplayName() + "」的全部技能(新学 " + count + " 个)"
                        + (hidden == 0 ? ""
                        : ";检测到 " + hidden + " 个技能,这些技能需要附属才能学会")
                        + (shut == 0 ? ""
                        : ";另有 " + shut + " 个技能暂未开放")), false);
        return count;
    }

    private static int maxExp(net.minecraft.commands.CommandSourceStack source)
            throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        AbilityData aData = AbilityData.get(p);
        List<Skill> learned = aData.getLearnedSkillList();
        for (Skill s : learned) {
            aData.setSkillExp(s, 1.0f);
        }
        int count = learned.size();
        source.sendSuccess(
                () -> Component.literal("已把 " + count + " 个已习得技能的熟练度设为 100%"), false);
        return count;
    }

}
