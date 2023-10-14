package party.lemons.biomemakeover.block.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import party.lemons.biomemakeover.block.AltarBlock;
import party.lemons.biomemakeover.crafting.AltarMenu;
import party.lemons.biomemakeover.init.BMBlockEntities;
import party.lemons.biomemakeover.init.BMBlocks;
import party.lemons.biomemakeover.init.BMItems;
import party.lemons.biomemakeover.item.Cursing;
import party.lemons.biomemakeover.util.effect.BiomeMakeoverEffect;
import party.lemons.biomemakeover.util.effect.EffectHelper;

import java.util.Random;

public class AltarBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer
{
    public static final int MAX_TIME = 300;
    private static final double PI = Math.PI;
    private static final double TAU = Math.PI * 2D;
    private static final RandomSource RANDOM = RandomSource.create();

    private NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress = 0;
    protected final ContainerData data;
    public int ticks;
    public float nextPageAngle, pageAngle, nextPageTurn, angleChange, nextPageTurningSpeed, pageTurningSpeed, currentAngle, lastAngle, nextAngle;

    private boolean workingPrevious = false;

    public AltarBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BMBlockEntities.ALTAR.get(), blockPos, blockState);

        this.data = new ContainerData() {
            public int get(int index)
            {
                return progress;
            }

            public void set(int index, int value)
            {
                progress = value;
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    private void tick()
    {
        if(level == null)
            return;

        BlockPos pos = getBlockPos();
        ticks++;
        updateBook();

        boolean working = false;
        if(canWork())
        {
            if(!level.isClientSide())
            {
                working = true;
                if(!workingPrevious)
                {
                    level.setBlock(pos, level.getBlockState(pos).setValue(AltarBlock.ACTIVE, true), 3);
                    EffectHelper.doEffect(level, BiomeMakeoverEffect.PLAY_CURSE_SOUND, pos);
                }

                progress++;
                if(progress >= MAX_TIME)
                {
                    ItemStack cursedItem = Cursing.curseItemStack(level, getItem(0), level.random);
                    if(cursedItem.isEmpty())
                    {
                        //Cursing failed, get rid of the item
                        Block.popResource(level, getBlockPos(), getItem(0).copy());
                        getItem(0).shrink(1);
                    }
                    else {
                        setItem(0, cursedItem);
                    }
                    progress = 0;
                    getItem(1).shrink(1);
                }
            }
        }
        else if(!level.isClientSide())
        {
            progress = 0;
            working = false;
            if(!workingPrevious)
            {
                level.setBlock(pos, level.getBlockState(pos).setValue(AltarBlock.ACTIVE, false), 3);
            }
        }

        workingPrevious = working;
    }

    private void updateBook()
    {
        if(!level.isClientSide()) return;

        BlockPos pos = getBlockPos();
        this.pageTurningSpeed = this.nextPageTurningSpeed;
        this.lastAngle = this.currentAngle;

        if(!level.getBlockState(pos).is(BMBlocks.ALTAR.get()))
            return;

        if(level.getBlockState(pos).getValue(AltarBlock.ACTIVE))
        {
            this.nextAngle += 0.5F;
            this.nextPageTurningSpeed += 0.2F;
        }
        else
        {
            Player playerEntity = this.level.getNearestPlayer((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, 3.0D, false);
            if(playerEntity != null)
            {
                double distanceX = playerEntity.getX() - ((double) pos.getX() + 0.5D);
                double distanceZ = playerEntity.getZ() - ((double) pos.getZ() + 0.5D);
                this.nextAngle = (float) Mth.atan2(distanceZ, distanceX);
                this.nextPageTurningSpeed += 0.1F;
                if(this.nextPageTurningSpeed < 0.5F || RANDOM.nextInt(40) == 0)
                {
                    float f = this.nextPageTurn;

                    do
                    {
                        this.nextPageTurn += (float) (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                    }while(f == this.nextPageTurn);
                }
            }else
            {
                this.nextAngle += 0.02F;
                this.nextPageTurningSpeed -= 0.1F;
            }
        }

        while(this.currentAngle >= PI)
        {
            this.currentAngle -= TAU;
        }

        while(this.currentAngle < -PI)
        {
            this.currentAngle += TAU;
        }

        while(this.nextAngle >= PI)
        {
            this.nextAngle -= TAU;
        }

        while(this.nextAngle < -PI)
        {
            this.nextAngle += TAU;
        }

        float rotation;
        for(rotation = this.nextAngle - this.currentAngle; rotation >= PI; rotation -= TAU)
        {
        }

        while(rotation < -PI)
        {
            rotation += TAU;
        }

        this.currentAngle += rotation * 0.4F;
        this.nextPageTurningSpeed = Mth.clamp(this.nextPageTurningSpeed, 0.0F, 1.0F);
        ++this.ticks;
        this.pageAngle = this.nextPageAngle;
        float h = (this.nextPageTurn - this.nextPageAngle) * 0.4F;
        h = Mth.clamp(h, -0.2F, 0.2F);
        this.angleChange += (h - this.angleChange) * 0.9F;
        this.nextPageAngle += this.angleChange;
    }

    public boolean canWork()
    {
        return Cursing.isValidForCurse(inventory.get(0)) && !inventory.get(1).isEmpty();
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if(direction.getAxis() == Direction.Axis.Y) return new int[]{0};
        else return new int[]{1};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if(slot == 0 && Cursing.isValidForCurse(stack)) return true;
        else return slot == 1 && stack.is(BMItems.CURSE_FUEL);
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        if (i < 0 || i >= this.inventory.size()) {
            return ItemStack.EMPTY;
        }
        return this.inventory.get(i);
    }


    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack itemStack = ContainerHelper.removeItem(this.inventory, i, j);
        if (!itemStack.isEmpty()) {
            this.setChanged();
        }
        return itemStack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack itemStack = this.inventory.get(i);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.inventory.set(i, ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.inventory.set(i, itemStack);
        if (!itemStack.isEmpty() && itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = getBlockPos();
        if(this.level.getBlockEntity(pos) != this)
        {
            return false;
        }else
        {
            return player.distanceToSqr((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        this.inventory.clear();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.inventory = nonNullList;
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType)
    {
        return (lvl, pos, state, type) -> {
            BlockEntity be =lvl.getBlockEntity(pos);
            if(be != null && be instanceof AltarBlockEntity)
                ((AltarBlockEntity)be).tick();

        };
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        ContainerHelper.saveAllItems(compoundTag, this.inventory);
        compoundTag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        ContainerHelper.loadAllItems(compoundTag, this.inventory);
        progress = compoundTag.getInt("Progress");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new AltarMenu(i, inventory, this, data);
    }
}
