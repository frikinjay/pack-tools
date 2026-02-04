package com.frikinjay.packtools.mixin.features.clumped;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.clumped.ClumpedOrbHandler;
import com.frikinjay.packtools.features.clumped.IClumpedOrb;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = ExperienceOrb.class, priority = 1001)
public abstract class ExperienceOrbMixin extends Entity implements IClumpedOrb, ClumpedOrbHandler.OrbAccessor {

    @Shadow private int count;
    @Shadow private int age;

    @Shadow protected abstract int repairPlayerItems(ServerPlayer player, int value);
    @Shadow public abstract int getValue();

    @Unique private ClumpedOrbHandler packtools$handler;

    @Unique private static boolean packtools$clumpedEnabled() {
        return ConfigRegistry.CLUMPED.getValue();
    }

    public ExperienceOrbMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private ClumpedOrbHandler packtools$handler() {
        if (packtools$handler == null) {
            packtools$handler = new ClumpedOrbHandler((ExperienceOrb) (Object) this);
        }
        return packtools$handler;
    }

    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;)Z", at = @At("HEAD"), cancellable = true)
    private void packtools$canMerge(ExperienceOrb orb, CallbackInfoReturnable<Boolean> cir) {
        if (!packtools$clumpedEnabled()) return;
        cir.setReturnValue(orb.isAlive() && !this.is(orb));
    }

    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;II)Z", at = @At("HEAD"), cancellable = true)
    private static void packtools$canMergeStatic(ExperienceOrb orb, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (!packtools$clumpedEnabled()) return;
        cir.setReturnValue(orb.isAlive());
    }

    @Inject(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    public void packtools$playerTouch(Player rawPlayer, CallbackInfo ci) {
        if (!packtools$clumpedEnabled()) return;

        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }

        player.takeXpDelay = 0;
        player.take(this, 1);

        int totalXp = packtools$handler().processPlayerPickup(player, this::repairPlayerItems);
        if (totalXp > 0) {
            player.giveExperiencePoints(totalXp);
        }

        this.discard();
        ci.cancel();
    }

    @ModifyVariable(
            index = 3,
            method = "repairPlayerItems",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRandomItemWith(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Optional;")
    )
    public Optional<EnchantedItemInUse> packtools$captureRepairItem(Optional<EnchantedItemInUse> entry) {
        if (!packtools$clumpedEnabled()) return entry;
        packtools$handler().captureRepairItem(entry);
        return entry;
    }

    @Inject(
            method = "repairPlayerItems",
            cancellable = true,
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRandomItemWith(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Optional;")
    )
    public void packtools$repairPlayerItems(ServerPlayer player, int value, CallbackInfoReturnable<Integer> cir) {
        if (!packtools$clumpedEnabled()) return;
        cir.setReturnValue(packtools$handler().calculateRepairAndRemainingXp(player, value, this::repairPlayerItems));
    }

    @Inject(
            method = "merge(Lnet/minecraft/world/entity/ExperienceOrb;)V",
            at = @At(value = "INVOKE", target = "net/minecraft/world/entity/ExperienceOrb.discard()V", shift = At.Shift.BEFORE),
            cancellable = true
    )
    public void packtools$merge(ExperienceOrb other, CallbackInfo ci) {
        if (!packtools$clumpedEnabled()) return;

        packtools$handler().mergeWith(
                ((IClumpedOrb) other).packtools$getHandler(),
                this,
                (ClumpedOrbHandler.OrbAccessor) other
        );
        other.discard();
        ci.cancel();
    }

    @Inject(
            method = "tryMergeToExisting(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void packtools$tryMergeToExisting(ServerLevel level, Vec3 pos, int value, CallbackInfoReturnable<Boolean> cir) {
        if (!packtools$clumpedEnabled()) return;
        cir.setReturnValue(ClumpedOrbHandler.tryMergeToExisting(level, pos, value));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void packtools$addAdditionalSaveData(ValueOutput valueOutput, CallbackInfo ci) {
        if (!packtools$clumpedEnabled()) return;
        packtools$handler().saveToNBT(valueOutput);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void packtools$readAdditionalSaveData(ValueInput valueInput, CallbackInfo ci) {
        if (!packtools$clumpedEnabled()) return;
        packtools$handler().loadFromNBT(valueInput, getValue(), count);
    }

    @Override
    public ClumpedOrbHandler packtools$getHandler() {
        return packtools$handler();
    }

/*    @Override
    public Int2IntMap packtools$getClumpedMap() {
        return packtools$handler().getClumpedMap();
    }*/

    @Override
    public int packtools$getAge() {
        return age;
    }

    @Override
    public void packtools$setAge(int age) {
        this.age = age;
    }

    @Override
    public void packtools$setCount(int count) {
        this.count = count;
    }
}