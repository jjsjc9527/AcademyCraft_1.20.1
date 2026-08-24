package cn.academy.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ACTrigger extends SimpleCriterionTrigger<ACTrigger.Instance> {

    private final ResourceLocation id;

    public ACTrigger(String name) {
        this.id = new ResourceLocation("academy", name);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player,
                                      DeserializationContext ctx) {
        return new Instance(id, player);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, inst -> true);
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        Instance(ResourceLocation id, ContextAwarePredicate player) {
            super(id, player);
        }
    }
}
