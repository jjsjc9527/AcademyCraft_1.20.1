package cn.academy.mixin;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void academy$dazeBlockMove(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet,
                                       CallbackInfo ci) {
        if (this.player != null
                && (cn.academy.ability.vanilla.mentalout.DazeState.isDazed(this.player)
                    || cn.academy.ability.vanilla.mentalout.Helpless.isPositionLocked(this.player))) {
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            index = 1)
    private Vec3 academy$movePlayerToLocal(Vec3 worldDelta) {
        Direction grav = ACGravity.getGravityDirection(this.player);
        if (grav == Direction.DOWN) return worldDelta;
        return RotationUtil.vecWorldToPlayer(worldDelta, grav);
    }

    @ModifyArg(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            index = 1)
    private Vec3 academy$moveVehicleToLocal(Vec3 worldDelta) {
        Entity vehicle = this.player.getRootVehicle();
        Direction grav = ACGravity.getGravityDirection(vehicle);
        if (grav == Direction.DOWN) return worldDelta;
        return RotationUtil.vecWorldToPlayer(worldDelta, grav);
    }

    @Inject(method = "handleClientCommand", at = @At("HEAD"))
    private void academy$releaseRespawnDeadlock(
            net.minecraft.network.protocol.game.ServerboundClientCommandPacket packet,
            CallbackInfo ci) {
        if (packet.getAction()
                != net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN) {
            return;
        }
        ServerPlayer p = this.player;
        if (p == null || p.wonGame || p.getHealth() <= 0.0F) {
            return;
        }
        if (!p.serverLevel().getServer().isSameThread()) {
            return;
        }

        p.resetSentInfo();
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                p.getHealth(), p.getFoodData().getFoodLevel(), p.getFoodData().getSaturationLevel()));

        cn.academy.network.DeathScreenReleaseMessage.send(p);
    }

    @Inject(method = "noBlocksAround", at = @At("HEAD"), cancellable = true)
    private void academy$noBlocksAroundGravity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Direction grav = ACGravity.getGravityDirection(entity);
        if (grav == Direction.DOWN) return;
        Vec3 down = RotationUtil.vecPlayerToWorld(new Vec3(0.0D, -0.55D, 0.0D), grav);
        cir.setReturnValue(entity.level()
                .getBlockStates(entity.getBoundingBox().inflate(0.0625D).expandTowards(down.x, down.y, down.z))
                .allMatch(BlockBehaviour.BlockStateBase::isAir));
    }
}
