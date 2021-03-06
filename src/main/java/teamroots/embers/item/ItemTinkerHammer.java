package teamroots.embers.item;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import teamroots.embers.api.power.IEmberPacketProducer;
import teamroots.embers.api.power.IEmberPacketReceiver;
import teamroots.embers.api.tile.ITargetable;
import teamroots.embers.util.Misc;

import java.util.List;

public class ItemTinkerHammer extends ItemBase {
	public ItemTinkerHammer() {
		super("tinker_hammer", true);
		this.setMaxStackSize(1);
	}
	
	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected){
		if (!stack.hasTagCompound()){
			stack.setTagCompound(new NBTTagCompound());
		}
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing face, float hitX, float hitY, float hitZ){
		ItemStack stack = player.getHeldItem(hand);
		TileEntity tile = world.getTileEntity(pos);
		if (player.isSneaking()){
			stack.getTagCompound().setInteger("targetX", pos.getX());
			stack.getTagCompound().setInteger("targetY", pos.getY());
			stack.getTagCompound().setInteger("targetZ", pos.getZ());
			world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1.0f, 1.9f+Misc.random.nextFloat()*0.2f, false);
			return EnumActionResult.SUCCESS;
		}
		else if (tile instanceof IEmberPacketProducer && stack.getTagCompound().hasKey("targetX")){
			if (world.getTileEntity(new BlockPos(stack.getTagCompound().getInteger("targetX"),stack.getTagCompound().getInteger("targetY"),stack.getTagCompound().getInteger("targetZ"))) instanceof IEmberPacketReceiver){
				((IEmberPacketProducer)tile).setTargetPosition(new BlockPos(stack.getTagCompound().getInteger("targetX"),stack.getTagCompound().getInteger("targetY"),stack.getTagCompound().getInteger("targetZ")), face);
				world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1.0f, 0.95f+Misc.random.nextFloat()*0.1f, false);
				return EnumActionResult.SUCCESS;
			}
		}
		else if (tile instanceof ITargetable && stack.getTagCompound().hasKey("targetX")){
			((ITargetable)tile).setTarget(new BlockPos(stack.getTagCompound().getInteger("targetX"),stack.getTagCompound().getInteger("targetY"),stack.getTagCompound().getInteger("targetZ")));
			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.FAIL;
	}
	
	@Override
	public boolean hasContainerItem(ItemStack stack){
		return true;
	}
	
	@Override
	public ItemStack getContainerItem(ItemStack stack){
		return new ItemStack(this,1);
	}
	
	@Override
	public boolean hasContainerItem(){
		return true;
	}
	
	@Override
	public Item getContainerItem(){
		return this;
	}
	
	@Override
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag advanced){
		if (stack.hasTagCompound()){
			if (stack.getTagCompound().hasKey("targetX")){
				tooltip.add(I18n.format("embers.tooltip.targetingBlock"));
				tooltip.add(" X=" + stack.getTagCompound().getInteger("targetX"));
				tooltip.add(" Y=" + stack.getTagCompound().getInteger("targetY"));
				tooltip.add(" Z=" + stack.getTagCompound().getInteger("targetZ"));
			}
		}
	}
}
