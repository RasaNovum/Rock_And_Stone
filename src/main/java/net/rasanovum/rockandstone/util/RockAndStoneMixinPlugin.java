package net.rasanovum.rockandstone.util;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class RockAndStoneMixinPlugin implements IMixinConfigPlugin {
    private static final String CREATE_RNS_DEPOSIT_STRUCTURE =
            "com.bmaster.createrns.content.deposit.worldgen.DepositStructure";
    private static final String LARGE_ORE_DEPOSITS_DEPOSIT =
            "com.endertech.minecraft.mods.adlods.deposit.Deposit";
    private static final String LARGE_ORE_DEPOSITS_ABSTRACT_TARGET =
            "com.endertech.minecraft.mods.adlods.target.AbstractTarget";
    private boolean createRnsInstalled;
    private boolean largeOreDepositsInstalled;

    @Override
    public void onLoad(String mixinPackage) {
        createRnsInstalled = isClassAvailable(CREATE_RNS_DEPOSIT_STRUCTURE);
        largeOreDepositsInstalled = isClassAvailable(LARGE_ORE_DEPOSITS_DEPOSIT)
                && isClassAvailable(LARGE_ORE_DEPOSITS_ABSTRACT_TARGET);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("CreateRnsDepositStructureMixin")) {
            return createRnsInstalled;
        }
        if (mixinClassName.endsWith("LargeOreDepositsAbstractTargetMixin")) {
            return largeOreDepositsInstalled;
        }
        return true;
    }

    private static boolean isClassAvailable(String className) {
        String classResource = className.replace('.', '/') + ".class";
        ClassLoader[] classLoaders = {
                RockAndStoneMixinPlugin.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        };
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader == null) {
                continue;
            }
            if (classLoader.getResource(classResource) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
