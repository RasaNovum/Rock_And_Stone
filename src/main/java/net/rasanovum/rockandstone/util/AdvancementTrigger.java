package net.rasanovum.rockandstone.util;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementTrigger extends SimpleCriterionTrigger<AdvancementTrigger.TriggerInstance> {
    private static final ResourceLocation ID = new ResourceLocation("rockandstone", "geological_hotspot");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        String oreType = json.has("ore_type") ? json.get("ore_type").getAsString() : "";
        return new TriggerInstance(playerPredicate, oreType);
    }

    public void trigger(ServerPlayer player, String currentMatchingOre) {
        this.trigger(player, instance -> instance.matches(currentMatchingOre));
    }

    public static class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final String oreType;

        public TriggerInstance(ContextAwarePredicate playerPredicate, String oreType) {
            super(AdvancementTrigger.ID, playerPredicate);
            this.oreType = oreType;
        }

        public boolean matches(String currentMatchingOre) {
            return this.oreType.equals(currentMatchingOre);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("ore_type", this.oreType);
            return json;
        }
    }
}
