package us.potatoboy.fortress.custom.item;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.GameMode;
import us.potatoboy.fortress.game.active.FortressPlayer;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.util.PlayerRef;

import java.util.Random;

public class TeslaCoilModuleItem extends ModuleItem {
    public TeslaCoilModuleItem(Identifier structure) {
        super(Items.LIGHTNING_ROD, structure);
    }

    @Override
    public void tick(BlockPos center, Object2ObjectMap<PlayerRef, FortressPlayer> participants, GameTeamKey owner, ServerWorld world) {
        BlockBounds bounds = BlockBounds.of(center.add(-4, 0, -4), center.add(4, 4, 4));

        for (Object2ObjectMap.Entry<PlayerRef, FortressPlayer> entry : Object2ObjectMaps.fastIterable(participants)) {
            ServerPlayerEntity player = entry.getKey().getEntity(world);
            if (player == null) continue;
            if (player.interactionManager.getGameMode() != GameMode.ADVENTURE) continue;
            if (entry.getValue().team == owner) continue;
            if (!bounds.contains(player.getBlockPos().getX(), player.getBlockPos().getZ())) continue;

            player.damage(DamageSource.LIGHTNING_BOLT, 0.3f);
            world.playSoundFromEntity(null, player, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.BLOCKS, 0.5f, 2f);
        }

        Random random = new Random();
        for (int i = 0; i < 10; i++) {

            Vec3d pos = HealModuleItem.randomPos(random, bounds);

            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5,
                    pos.getY() + 1,
                    pos.getZ() + 0.5,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }
}