package net.rasanovum.rockandstone.util;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class AdvancementTrigger extends SimpleCriterionTrigger<AdvancementTrigger.TriggerInstance> {
    public static final ResourceLocation ID = VersionUtils.fromNamespaceAndPath("rockandstone", "geological_hotspot");

    public static AdvancementTrigger register() {
        //? if <1.21 {
        /*return CriteriaTriggers.register(new AdvancementTrigger());
        *///?} else {
        return Registry.register(BuiltInRegistries.TRIGGER_TYPES, ID, new AdvancementTrigger());
        //?}
    }

    //? if <1.21 {
    /*@Override
    public ResourceLocation getId() {
        return ID;
    }
    *///?} else {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }
    //?}

    //? if <1.21 {
    /*@Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate playerPredicate, DeserializationContext context) {
        String oreType = json.has("ore_type") ? json.get("ore_type").getAsString() : "";
        return new TriggerInstance(playerPredicate, oreType);
    }
    *///?}

    public void trigger(ServerPlayer player, String currentMatchingOre) {
        this.trigger(player, instance -> instance.matches(currentMatchingOre));
    }

    //? if <1.21 {
    /*public static class TriggerInstance extends AbstractCriterionTriggerInstance {
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
    *///?} else {
    public record TriggerInstance(Optional<ContextAwarePredicate> player, String oreType)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.fieldOf("ore_type").forGetter(TriggerInstance::oreType)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(String currentMatchingOre) {
            return this.oreType.equals(currentMatchingOre);
        }
    }
    //?}
}
