package party.lemons.biomemakeover;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import party.lemons.biomemakeover.block.blockentity.AltarBlockEntity;
import party.lemons.biomemakeover.block.blockentity.render.AltarRenderer;
import party.lemons.biomemakeover.block.blockentity.render.LightningBugBottleRenderer;
import party.lemons.biomemakeover.block.blockentity.render.TapestryRenderer;
import party.lemons.biomemakeover.crafting.AltarScreen;
import party.lemons.biomemakeover.crafting.DirectionDataScreen;
import party.lemons.biomemakeover.crafting.witch.menu.WitchScreen;
import party.lemons.biomemakeover.entity.render.*;
import party.lemons.biomemakeover.entity.render.feature.HatLayer;
import party.lemons.biomemakeover.entity.render.feature.ScarabElytraLayer;
import party.lemons.biomemakeover.init.BMBlockEntities;
import party.lemons.biomemakeover.init.BMBlocks;
import party.lemons.biomemakeover.init.BMEntities;
import party.lemons.biomemakeover.init.BMScreens;
import party.lemons.biomemakeover.util.sound.AltarCursingSoundInstance;
import party.lemons.taniwha.client.color.ColorProviderHelper;
import party.lemons.taniwha.client.color.FoliageBlockColorProvider;
import party.lemons.taniwha.client.color.FoliageShiftBlockColorProvider;
import party.lemons.taniwha.client.color.StaticBlockColorProvider;
import party.lemons.taniwha.hooks.TClientEvents;

public class BiomeMakeoverClient
{
    public static void init()
    {
        if (Platform.getEnvironment() == Env.CLIENT) {
            BMEntities.registerModelLayers();;

            LifecycleEvent.SETUP.register(()->{
                BlockEntityRendererRegistry.register(BMBlockEntities.TAPESTRY.get(), TapestryRenderer::new);
                BlockEntityRendererRegistry.register(BMBlockEntities.ALTAR.get(), AltarRenderer::new);
                BlockEntityRendererRegistry.register(BMBlockEntities.LIGHTNING_BUG_BOTTLE.get(), LightningBugBottleRenderer::new);

                initColors();

                MenuRegistry.registerScreenFactory(BMScreens.WITCH.get(), WitchScreen::new);
                MenuRegistry.registerScreenFactory(BMScreens.ALTAR.get(), AltarScreen::new);
                MenuRegistry.registerScreenFactory(BMScreens.DIRECTIONAL_DATA.get(), DirectionDataScreen::new);
            });

            registerModels();
        }
    }

    public static void registerModels()
    {
        EnvExecutor.runInEnv(Env.CLIENT, ()->()->{

            EntityRendererRegistry.register(BMEntities.TUMBLEWEED, TumbleweedRender::new);
            EntityRendererRegistry.register(BMEntities.LIGHTNING_BOTTLE, ThrownItemRenderer::new);

            EntityRendererRegistry.register(BMEntities.GLOWFISH, GlowfishRender::new);
            EntityRendererRegistry.register(BMEntities.BLIGHTBAT, BlightBatRender::new);
            EntityRendererRegistry.register(BMEntities.MUSHROOM_TRADER, MushroomTraderRender::new);
            EntityRendererRegistry.register(BMEntities.SCUTTLER, ScuttlerRender::new);
            EntityRendererRegistry.register(BMEntities.GHOST, GhostRender::new);
            EntityRendererRegistry.register(BMEntities.COWBOY, CowboyRender::new);
            EntityRendererRegistry.register(BMEntities.DECAYED, DecayedRender::new);
            EntityRendererRegistry.register(BMEntities.DRAGONFLY, DragonflyRender::new);
            EntityRendererRegistry.register(BMEntities.TOAD, ToadRender::new);
            EntityRendererRegistry.register(BMEntities.TADPOLE, TadpoleRender::new);
            EntityRendererRegistry.register(BMEntities.LIGHTNING_BUG, LightningBugRender::new);
            EntityRendererRegistry.register(BMEntities.LIGHTNING_BUG_ALTERNATE, LightningBugRender::new);
            EntityRendererRegistry.register(BMEntities.OWL, OwlRender::new);
            EntityRendererRegistry.register(BMEntities.MOTH, MothRender::new);
            EntityRendererRegistry.register(BMEntities.ROOTLING, RootlingRender::new);
            EntityRendererRegistry.register(BMEntities.ADJUDICATOR, AdjudicatorRender::new);
            EntityRendererRegistry.register(BMEntities.ADJUDICATOR_MIMIC, AdjudicatorMimicRender::new);
            EntityRendererRegistry.register(BMEntities.STONE_GOLEM, StoneGolemRender::new);
            EntityRendererRegistry.register(BMEntities.HELMIT_CRAB, HelmitCrabRender::new);
            EntityRendererRegistry.register(BMEntities.CHEST_CAMEL, ChestCamelRender::new);
            EntityRendererRegistry.register(BMEntities.BANNER_CAMEL, BannerCamelRender::new);

            TClientEvents.LAYERS.add((renderLayerParent, entityModelSet) -> new ScarabElytraLayer(renderLayerParent, entityModelSet));
            TClientEvents.LAYERS.add((renderLayerParent, entityModelSet) -> new HatLayer(renderLayerParent, entityModelSet));
        });
    }

    private static void initColors()
    {
        ColorProviderHelper.registerSimpleBlockWithItem(new FoliageBlockColorProvider(),
                BMBlocks.ANCIENT_OAK_LEAVES,
                BMBlocks.IVY
        );
        ColorProviderHelper.registerSimpleBlockWithItem(new StaticBlockColorProvider(0x84ab6f),
                BMBlocks.SWAMP_CYPRESS_LEAVES
        );

        ColorProviderHelper.registerSimpleBlockWithItem(new FoliageShiftBlockColorProvider(0, 0, 0)
        {
            @Override
            protected int[] getColorBoosts(BlockAndTintGetter world, BlockState state, BlockPos pos, int tintIndex)
            {
                if(world instanceof ClientLevel && pos != null)
                {
                    if(((ClientLevel) world).getBiome(pos).is(BiomeTags.HAS_SWAMP_HUT))
                    {
                        return new int[]{-20, 40, -20};
                    }
                }

                return super.getColorBoosts(world, state, pos, tintIndex);
            }
        },
                BMBlocks.SMALL_LILY_PAD,
                ()->Blocks.LILY_PAD,
                BMBlocks.WATER_LILY
        );

        ColorProviderHelper.registerSimpleBlockWithItem(new FoliageShiftBlockColorProvider(0, 0, 0)
        {
            @Override
            protected int[] getColorBoosts(BlockAndTintGetter world, BlockState state, BlockPos pos, int tintIndex)
            {
                if(world instanceof ClientLevel)
                {
                    if(((ClientLevel) world).getBiome(pos).is(BiomeTags.HAS_SWAMP_HUT))
                    {
                        return new int[]{-10, 15, -10};
                    }
                }

                return super.getColorBoosts(world, state, pos, tintIndex);
            }
        },
BMBlocks.WILLOW_LEAVES,
                BMBlocks.WILLOWING_BRANCHES
        );

        ColorProviderHelper.registerSimpleBlockWithItem(new FoliageShiftBlockColorProvider(35, -10, -5),
                BMBlocks.MOTH_BLOSSOM, BMBlocks.ITCHING_IVY
        );
    }

    //TODO: Find somewhere else for this
    public static void curseSound(AltarBlockEntity altar)
    {
        AltarCursingSoundInstance sound = new AltarCursingSoundInstance(altar, altar.getLevel().getRandom());
        Minecraft.getInstance().getSoundManager().play(sound);
    }
}
