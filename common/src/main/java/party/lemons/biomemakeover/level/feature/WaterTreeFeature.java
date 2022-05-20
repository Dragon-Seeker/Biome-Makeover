/*
 * Decompiled with CFR 0.0.9 (FabricMC cc05e23f).
 */
package party.lemons.biomemakeover.level.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class WaterTreeFeature extends Feature<TreeConfiguration> {

    public WaterTreeFeature(Codec<TreeConfiguration> codec) {
        super(codec);
    }

    public static boolean isFree(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
        return validTreePos(levelSimulatedReader, blockPos) || levelSimulatedReader.isStateAtPosition(blockPos, blockState -> blockState.is(BlockTags.LOGS));
    }

    private static boolean isVine(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
        return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> blockState.is(Blocks.VINE));
    }

    private static boolean isBlockWater(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
        return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> blockState.is(Blocks.WATER));
    }

    private static void setBlockKnownShape(LevelWriter levelWriter, BlockPos blockPos, BlockState blockState) {
        levelWriter.setBlock(blockPos, blockState, 19);
    }

    private static boolean isReplaceablePlant(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
        return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> {
            Material material = blockState.getMaterial();
            return material == Material.REPLACEABLE_PLANT;
        });
    }

    public static boolean validTreePos(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
        return TreeFeature.isAirOrLeaves(levelSimulatedReader, blockPos) || isReplaceablePlant(levelSimulatedReader, blockPos) || isBlockWater(levelSimulatedReader, blockPos);
    }

    private boolean doPlace(WorldGenLevel worldGenLevel, RandomSource random, BlockPos blockPos, BiConsumer<BlockPos, BlockState> biConsumer, BiConsumer<BlockPos, BlockState> biConsumer2, TreeConfiguration treeConfiguration) {
        int i = treeConfiguration.trunkPlacer.getTreeHeight(random);
        int j = treeConfiguration.foliagePlacer.foliageHeight(random, i, treeConfiguration);
        int k = i - j;
        int l = treeConfiguration.foliagePlacer.foliageRadius(random, k);

        if (blockPos.getY() < worldGenLevel.getMinBuildHeight() + 1 || blockPos.getY() + i + 1 > worldGenLevel.getMaxBuildHeight()) {
            return false;
        }
        OptionalInt optionalInt = treeConfiguration.minimumSize.minClippedHeight();
        int m = this.getMaxFreeTreeHeight(worldGenLevel, i, blockPos, treeConfiguration);
        if (!(m >= i || optionalInt.isPresent() && m >= optionalInt.getAsInt())) {
            return false;
        }
        List<FoliagePlacer.FoliageAttachment> list = treeConfiguration.trunkPlacer.placeTrunk(worldGenLevel, biConsumer, random, m, blockPos, treeConfiguration);
        list.forEach(foliageAttachment -> treeConfiguration.foliagePlacer.createFoliage(worldGenLevel, biConsumer2, random, treeConfiguration, m, (FoliagePlacer.FoliageAttachment)foliageAttachment, j, l));
        return true;
    }

    private int getMaxFreeTreeHeight(LevelSimulatedReader levelSimulatedReader, int i, BlockPos blockPos, TreeConfiguration treeConfiguration) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (int j = 0; j <= i + 1; ++j) {
            int k = treeConfiguration.minimumSize.getSizeAtHeight(i, j);
            for (int l = -k; l <= k; ++l) {
                for (int m = -k; m <= k; ++m) {
                    mutableBlockPos.setWithOffset(blockPos, l, j, m);
                    if (isFree(levelSimulatedReader, mutableBlockPos) && (treeConfiguration.ignoreVines || !isVine(levelSimulatedReader, mutableBlockPos))) continue;
                    return j - 2;
                }
            }
        }
        return i;
    }

    @Override
    protected void setBlock(LevelWriter levelWriter, BlockPos blockPos, BlockState blockState) {
        setBlockKnownShape(levelWriter, blockPos, blockState);
    }

    @Override
    public final boolean place(FeaturePlaceContext<TreeConfiguration> featurePlaceContext) {
        WorldGenLevel worldGenLevel = featurePlaceContext.level();
        RandomSource random = featurePlaceContext.random();
        BlockPos blockPos2 = featurePlaceContext.origin();

        if(featurePlaceContext.level().getFluidState(featurePlaceContext.origin()).getType() != Fluids.WATER)
            return false;

        TreeConfiguration treeConfiguration = featurePlaceContext.config();
        HashSet<BlockPos> set = Sets.newHashSet();
        HashSet<BlockPos> set2 = Sets.newHashSet();
        HashSet<BlockPos> set3 = Sets.newHashSet();
        BiConsumer<BlockPos, BlockState> biConsumer = (blockPos, blockState) -> {
            set.add(blockPos.immutable());
            worldGenLevel.setBlock(blockPos, blockState, 19);
        };
        BiConsumer<BlockPos, BlockState> biConsumer2 = (blockPos, blockState) -> {
            set2.add(blockPos.immutable());
            worldGenLevel.setBlock(blockPos, blockState, 19);
        };
        BiConsumer<BlockPos, BlockState> biConsumer3 = (blockPos, blockState) -> {
            set3.add(blockPos.immutable());
            worldGenLevel.setBlock(blockPos, blockState, 19);
        };
        boolean bl = this.doPlace(worldGenLevel, random, blockPos2, biConsumer, biConsumer2, treeConfiguration);
        if (!bl || set.isEmpty() && set2.isEmpty()) {
            return false;
        }
        if (!treeConfiguration.decorators.isEmpty()) {
            ArrayList<BlockPos> list = Lists.newArrayList(set);
            ArrayList<BlockPos> list2 = Lists.newArrayList(set2);
            list.sort(Comparator.comparingInt(Vec3i::getY));
            list2.sort(Comparator.comparingInt(Vec3i::getY));
            treeConfiguration.decorators.forEach(treeDecorator -> {
                TreeDecorator.Context context = new TreeDecorator.Context(worldGenLevel, biConsumer3, random, set2, set3, set);

                treeDecorator.place(context);
            });
        }

        return BoundingBox.encapsulatingPositions(Iterables.concat(set, set2, set3)).map(bb->{
            DiscreteVoxelShape discreteVoxelShape = updateLeaves(worldGenLevel, bb, set, set3);
            StructureTemplate.updateShapeAtEdge(worldGenLevel, 3, discreteVoxelShape, bb.minX(),  bb.minY(),  bb.minZ());
            return true;
        }).orElse(false);
    }

    private static DiscreteVoxelShape updateLeaves(LevelAccessor levelAccessor, BoundingBox boundingBox, Set<BlockPos> set, Set<BlockPos> set2) {
        ArrayList list = Lists.newArrayList();
        BitSetDiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
        int i = 6;
        for (int j = 0; j < 6; ++j) {
            list.add(Sets.newHashSet());
        }
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (BlockPos blockPos : Lists.newArrayList(set2)) {
            if (!boundingBox.isInside(blockPos)) continue;
            ((DiscreteVoxelShape)discreteVoxelShape).fill(blockPos.getX() - boundingBox.minX(), blockPos.getY() - boundingBox.minY(), blockPos.getZ() - boundingBox.minZ());
        }
        for (BlockPos blockPos : Lists.newArrayList(set)) {
            if (boundingBox.isInside(blockPos)) {
                ((DiscreteVoxelShape)discreteVoxelShape).fill(blockPos.getX() - boundingBox.minX(), blockPos.getY() - boundingBox.minY(), blockPos.getZ() - boundingBox.minZ());
            }
            for (Direction direction : Direction.values()) {
                BlockState blockState;
                mutableBlockPos.setWithOffset((Vec3i)blockPos, direction);
                if (set.contains(mutableBlockPos) || !(blockState = levelAccessor.getBlockState(mutableBlockPos)).hasProperty(BlockStateProperties.DISTANCE)) continue;
                ((Set)list.get(0)).add(mutableBlockPos.immutable());
                setBlockKnownShape(levelAccessor, mutableBlockPos, (BlockState)blockState.setValue(BlockStateProperties.DISTANCE, 1));
                if (!boundingBox.isInside(mutableBlockPos)) continue;
                ((DiscreteVoxelShape)discreteVoxelShape).fill(mutableBlockPos.getX() - boundingBox.minX(), mutableBlockPos.getY() - boundingBox.minY(), mutableBlockPos.getZ() - boundingBox.minZ());
            }
        }
        for (int k = 1; k < 6; ++k) {
            Set<BlockPos> set3 = (Set)list.get(k - 1);
            Set<BlockPos> set4 = (Set)list.get(k);
            for (BlockPos blockPos2 : set3) {
                if (boundingBox.isInside(blockPos2)) {
                    ((DiscreteVoxelShape)discreteVoxelShape).fill(blockPos2.getX() - boundingBox.minX(), blockPos2.getY() - boundingBox.minY(), blockPos2.getZ() - boundingBox.minZ());
                }
                for (Direction direction2 : Direction.values()) {
                    int l;
                    BlockState blockState2;
                    mutableBlockPos.setWithOffset((Vec3i)blockPos2, direction2);
                    if (set3.contains(mutableBlockPos) || set4.contains(mutableBlockPos) || !(blockState2 = levelAccessor.getBlockState(mutableBlockPos)).hasProperty(BlockStateProperties.DISTANCE) || (l = blockState2.getValue(BlockStateProperties.DISTANCE).intValue()) <= k + 1) continue;
                    BlockState blockState3 = (BlockState)blockState2.setValue(BlockStateProperties.DISTANCE, k + 1);
                    setBlockKnownShape(levelAccessor, mutableBlockPos, blockState3);
                    if (boundingBox.isInside(mutableBlockPos)) {
                        ((DiscreteVoxelShape)discreteVoxelShape).fill(mutableBlockPos.getX() - boundingBox.minX(), mutableBlockPos.getY() - boundingBox.minY(), mutableBlockPos.getZ() - boundingBox.minZ());
                    }
                    set4.add(mutableBlockPos.immutable());
                }
            }
        }
        return discreteVoxelShape;
    }
}

