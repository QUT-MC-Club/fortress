package us.potatoboy.fortress.game;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.block.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import us.potatoboy.fortress.custom.item.ModuleItem;
import us.potatoboy.fortress.game.active.FortressPlayer;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Cell {
    private GameTeamKey owner;
    private final BlockPos center;
    public final BlockBounds bounds;
    private final List<ModuleItem> modules;
    public boolean enabled;

    public CaptureState captureState;
    public int captureTicks;

    public Cell(BlockPos center) {
        this.center = center;
        this.owner = null;
        this.modules = new ArrayList<>();
        this.bounds = BlockBounds.of(center.add(-1, 0, -1), center.add(1, 0, 1));
        this.enabled = true;
    }

    public GameTeamKey getOwner() {
        return owner;
    }

    public void setOwner(GameTeamKey owner, ServerWorld world, CellManager cellManager) {
        this.owner = owner;
        bounds.iterator().forEachRemaining(blockPos -> world.setBlockState(blockPos, cellManager.getTeamBlock(owner, blockPos)));
    }

    public BlockPos getCenter() {
        return center;
    }

    public boolean hasModules() {
        return modules.isEmpty();
    }

    public boolean hasModuleAt(int index) {
        return modules.size() >= index + 1;
    }

    public void addModule(ModuleItem module) {
        modules.add(module);
    }

    public void tickModules(Object2ObjectMap<PlayerRef, FortressPlayer> participants, ServerWorld world) {
        modules.forEach(moduleItem -> moduleItem.tick(center, participants, owner, world));
    }

    public boolean incrementCapture(GameTeamKey team, ServerWorld world, int amount, CellManager cellManager) {
        captureTicks += amount;

        Iterator<BlockPos> iterator = bounds.iterator();
        for (int i = 0; i < captureTicks; i++) {
            if (iterator.hasNext()) {
                BlockPos blockPos = iterator.next();

                world.setBlockState(blockPos, cellManager.getTeamBlock(team, center));
            }
        }

        if (captureTicks >= 9) {
            captureTicks = 0;
            setOwner(team, world, cellManager);
            captureState = null;

            return true;
        }

        return false;
    }

    public boolean decrementCapture(ServerWorld world, int amount, CellManager cellManager) {
        captureTicks -= amount;

        BlockPos offset = center.add(1, 0, 1);
        for (int z = 0, i = 0; z > -3; z--) {
            for (int x = 0; x > -3 && i < 9 - captureTicks; x--, i++) {
                world.setBlockState(offset.add(x, 0, z), cellManager.getTeamBlock(owner, offset));
            }
        }

        if (captureTicks <= 0) {
            captureTicks = 0;
            captureState = null;

            return true;
        }

        return false;
    }

    public void setModuleColor(TeamPallet pallet, ServerWorld world) {
        BlockBounds moduleBounds = BlockBounds.of(bounds.min(), bounds.max().add(0, modules.size() * 3, 0));
        
        moduleBounds.iterator().forEachRemaining(blockPos -> {
            BlockState state = world.getBlockState(blockPos);

            if (state.isIn(BlockTags.PLANKS)) {
                world.setBlockState(blockPos, pallet.woodPlank().getDefaultState());
            } else if (state.isIn(BlockTags.WOODEN_STAIRS)) {
                world.setBlockState(blockPos, pallet.woodStair().getDefaultState()
                        .with(StairsBlock.FACING, state.get(StairsBlock.FACING))
                        .with(StairsBlock.HALF, state.get(StairsBlock.HALF))
                        .with(StairsBlock.SHAPE, state.get(StairsBlock.SHAPE))
                );
            } else if (state.isIn(BlockTags.WOODEN_SLABS)) {
                world.setBlockState(blockPos, pallet.woodSlab().getDefaultState()
                        .with(SlabBlock.TYPE, state.get(SlabBlock.TYPE))
                );
            }

            Block block = state.getBlock();

            if (block == Blocks.RED_CONCRETE || block == Blocks.BLUE_CONCRETE) {
                world.setBlockState(blockPos, pallet.primary().getDefaultState());
            }
        });
    }

    public void spawnParticles(ParticleEffect effect, ServerWorld world) {
        bounds.iterator().forEachRemaining(pos -> world.spawnParticles(
                effect,
                pos.getX() + 0.5,
                pos.getY() + 1,
                pos.getZ() + 0.5,
                1,
                0.0, 0.0, 0.0,
                0.0
        ));
    }

    public void spawnTeamParticles(GameTeamConfig team, ServerWorld world) {
        int color = team.blockDyeColor().getFireworkColor();
        DustParticleEffect effect = new DustParticleEffect(color, 2);

        spawnParticles(effect, world);
    }
}
