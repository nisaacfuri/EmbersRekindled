package teamroots.embers.tileentity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import teamroots.embers.EventManager;
import teamroots.embers.item.ItemTinkerHammer;
import teamroots.embers.util.EnumPipeConnection;
import teamroots.embers.util.Misc;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class TileEntityItemPipe extends TileEntity implements ITileEntityBase, ITickable, IPressurizable {
	int ticksExisted = 0;
	public ItemStackHandler inventory = new ItemStackHandler(1){
        @Override
        protected void onContentsChanged(int slot) {
            // We need to tell the tile entity that something has changed so
            // that the chest contents is persisted
        	TileEntityItemPipe.this.markDirty();
        }
	};
	public BlockPos lastReceived = new BlockPos(0,0,0);
	public int pressure;
	public boolean clogged;
	Random random = new Random();
	
	public EnumPipeConnection up = EnumPipeConnection.NONE, down = EnumPipeConnection.NONE, north = EnumPipeConnection.NONE, south = EnumPipeConnection.NONE, east = EnumPipeConnection.NONE, west = EnumPipeConnection.NONE;
	
	public TileEntityItemPipe(){
		super();
	}
	
	public void updateNeighbors(IBlockAccess world){
		up = getConnection(world,getPos().up(),EnumFacing.UP);
		down = getConnection(world,getPos().down(),EnumFacing.DOWN);
		north = getConnection(world,getPos().north(),EnumFacing.NORTH);
		south = getConnection(world,getPos().south(),EnumFacing.SOUTH);
		west = getConnection(world,getPos().west(),EnumFacing.WEST);
		east = getConnection(world,getPos().east(),EnumFacing.EAST);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		tag.setInteger("up", up.getIndex());
		tag.setInteger("down", down.getIndex());
		tag.setInteger("north", north.getIndex());
		tag.setInteger("south", south.getIndex());
		tag.setInteger("west", west.getIndex());
		tag.setInteger("east", east.getIndex());
		tag.setTag("inventory", inventory.serializeNBT());
		tag.setInteger("lastX", this.lastReceived.getX());
		tag.setInteger("lastY", this.lastReceived.getY());
		tag.setInteger("lastZ", this.lastReceived.getZ());
		tag.setInteger("pressure", pressure);
		tag.setBoolean("clogged", clogged);
		return tag;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		up = EnumPipeConnection.fromIndex(tag.getInteger("up"));
		down = EnumPipeConnection.fromIndex(tag.getInteger("down"));
		north = EnumPipeConnection.fromIndex(tag.getInteger("north"));
		south = EnumPipeConnection.fromIndex(tag.getInteger("south"));
		west = EnumPipeConnection.fromIndex(tag.getInteger("west"));
		east = EnumPipeConnection.fromIndex(tag.getInteger("east"));
		lastReceived = new BlockPos(tag.getInteger("lastX"),tag.getInteger("lastY"),tag.getInteger("lastZ"));
		pressure = tag.getInteger("pressure");
		clogged = tag.getBoolean("clogged");
		inventory.deserializeNBT(tag.getCompoundTag("inventory"));
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing){
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
			return true;
		}
		return super.hasCapability(capability, facing);
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing){
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
			return (T)this.inventory;
		}
		return super.getCapability(capability, facing);
	}
	
	public EnumPipeConnection getConnection(EnumFacing side){
		if (side == EnumFacing.UP){
			return up;
		}
		else if (side == EnumFacing.DOWN){
			return down;
		}
		else if (side == EnumFacing.EAST){
			return east;
		}
		else if (side == EnumFacing.WEST){
			return west;
		}
		else if (side == EnumFacing.NORTH){
			return north;
		}
		else if (side == EnumFacing.SOUTH){
			return south;
		}
		return EnumPipeConnection.NONE;
	}
	
	public void setConnection(EnumFacing side, EnumPipeConnection connect){
		if (side == EnumFacing.UP){
			up = connect;
		}
		else if (side == EnumFacing.DOWN){
			down = connect;
		}
		else if (side == EnumFacing.EAST){
			east = connect;
		}
		else if (side == EnumFacing.WEST){
			west = connect;
		}
		else if (side == EnumFacing.NORTH){
			north = connect;
		}
		else if (side == EnumFacing.SOUTH){
			south = connect;
		}
	}
	
	public EnumPipeConnection getConnection(IBlockAccess world, BlockPos pos, EnumFacing side){
		TileEntity tile = world.getTileEntity(pos);
		if (getConnection(side) == EnumPipeConnection.FORCENONE){
			return EnumPipeConnection.FORCENONE;
		}
		else if (tile instanceof IItemPipeConnectable){
			return ((IItemPipeConnectable) tile).getConnection(side);
		}
		else if (tile instanceof TileEntityItemPipe){
			return EnumPipeConnection.PIPE;
		}
		else if (tile instanceof TileEntityItemExtractor){
			return EnumPipeConnection.PIPE;
		}
		else if (tile != null){
			if (world.getTileEntity(pos).hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Misc.getOppositeFace(side))){
				return EnumPipeConnection.BLOCK;
			}
		}
		return EnumPipeConnection.NONE;
	}
	
	public void reverseConnection(EnumFacing face){
		
	}
	
	public static EnumPipeConnection reverseForce(EnumPipeConnection connect){
		if (connect == EnumPipeConnection.FORCENONE){
			return EnumPipeConnection.NONE;
		}
		if (connect != EnumPipeConnection.NONE && connect != EnumPipeConnection.LEVER){
			return EnumPipeConnection.FORCENONE;
		}
		return EnumPipeConnection.NONE;
	}

	@Override
	public boolean activate(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
			EnumFacing side, float hitX, float hitY, float hitZ) {
		ItemStack heldItem = player.getHeldItem(hand);
		if (!heldItem.isEmpty() && heldItem.getItem() instanceof ItemTinkerHammer) {
			if (side == EnumFacing.UP || side == EnumFacing.DOWN) {
				if (Math.abs(hitX - 0.5) > Math.abs(hitZ - 0.5)) {
					if (hitX < 0.5) {
						this.west = reverseForce(west);
						this.reverseConnection(EnumFacing.WEST);
					} else {
						this.east = reverseForce(east);
						this.reverseConnection(EnumFacing.EAST);
					}
				} else {
					if (hitZ < 0.5) {
						this.north = reverseForce(north);
						this.reverseConnection(EnumFacing.NORTH);
					} else {
						this.south = reverseForce(south);
						this.reverseConnection(EnumFacing.SOUTH);
					}
				}
			}
			if (side == EnumFacing.EAST || side == EnumFacing.WEST) {
				if (Math.abs(hitY - 0.5) > Math.abs(hitZ - 0.5)) {
					if (hitY < 0.5) {
						this.down = reverseForce(down);
						this.reverseConnection(EnumFacing.DOWN);
					} else {
						this.up = reverseForce(up);
						this.reverseConnection(EnumFacing.UP);
					}
				} else {
					if (hitZ < 0.5) {
						this.north = reverseForce(north);
						this.reverseConnection(EnumFacing.NORTH);
					} else {
						this.south = reverseForce(south);
						this.reverseConnection(EnumFacing.SOUTH);
					}
				}
			}
			if (side == EnumFacing.NORTH || side == EnumFacing.SOUTH) {
				if (Math.abs(hitX - 0.5) > Math.abs(hitY - 0.5)) {
					if (hitX < 0.5) {
						this.west = reverseForce(west);
						this.reverseConnection(EnumFacing.WEST);
					} else {
						this.east = reverseForce(east);
						this.reverseConnection(EnumFacing.EAST);
					}
				} else {
					if (hitY < 0.5) {
						this.down = reverseForce(down);
						this.reverseConnection(EnumFacing.DOWN);
					} else {
						this.up = reverseForce(up);
						this.reverseConnection(EnumFacing.UP);
					}
				}
			}
			updateNeighbors(world);
			return true;
		}
		return false;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		this.invalidate();
		Misc.spawnInventoryInWorld(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, inventory);
		world.setTileEntity(pos, null);
	}
	
	public boolean isConnected(EnumFacing face){
		TileEntity tile = getWorld().getTileEntity(getPos().offset(face));
		if (tile instanceof TileEntityItemPipe){
			if (((TileEntityItemPipe)tile).getConnection(Misc.getOppositeFace(face)) != EnumPipeConnection.FORCENONE
					&& ((TileEntityItemPipe)tile).getConnection(Misc.getOppositeFace(face)) != EnumPipeConnection.NONE){
				return true;
			}
		}
		if (tile instanceof TileEntityItemExtractor){
			if (((TileEntityItemExtractor)tile).getConnection(Misc.getOppositeFace(face)) != EnumPipeConnection.FORCENONE
					&& ((TileEntityItemExtractor)tile).getConnection(Misc.getOppositeFace(face)) != EnumPipeConnection.NONE){
				return true;
			}
		}
		if (getConnection(face) == EnumPipeConnection.BLOCK){
			return true;
		}
		return false;
	}

	@Override
	public void update() {
		ticksExisted ++;
		if (world.isRemote && clogged)
			Misc.spawnClogParticles(world,pos,1, 0.25f);
		if (ticksExisted % 1 == 0 && !world.isRemote){
			boolean itemsMoved = false;
			HashSet<BlockPos> toUpdate = new HashSet<>();
			ArrayList<EnumFacing> connections = new ArrayList<EnumFacing>();
			if (up != EnumPipeConnection.NONE && up != EnumPipeConnection.FORCENONE && up != EnumPipeConnection.LEVER && isConnected(EnumFacing.UP)){
				connections.add(EnumFacing.UP);
			}
			if (down != EnumPipeConnection.NONE && down != EnumPipeConnection.FORCENONE && down != EnumPipeConnection.LEVER && isConnected(EnumFacing.DOWN)){
				connections.add(EnumFacing.DOWN);
			}
			if (north != EnumPipeConnection.NONE && north != EnumPipeConnection.FORCENONE && north != EnumPipeConnection.LEVER && isConnected(EnumFacing.NORTH)){
				connections.add(EnumFacing.NORTH);
			}
			if (south != EnumPipeConnection.NONE && south != EnumPipeConnection.FORCENONE && south != EnumPipeConnection.LEVER && isConnected(EnumFacing.SOUTH)){
				connections.add(EnumFacing.SOUTH);
			}
			if (east != EnumPipeConnection.NONE && east != EnumPipeConnection.FORCENONE && east != EnumPipeConnection.LEVER && isConnected(EnumFacing.EAST)){
				connections.add(EnumFacing.EAST);
			}
			if (west != EnumPipeConnection.NONE && west != EnumPipeConnection.FORCENONE && west != EnumPipeConnection.LEVER && isConnected(EnumFacing.WEST)){
				connections.add(EnumFacing.WEST);
			}
			connections.removeIf(connectDir -> lastReceived.equals(getPos().offset(connectDir)));
			ArrayList<EnumFacing> priorities = new ArrayList<>();
			for (EnumFacing connection : connections) {
				if (getWorld().getTileEntity(getPos().offset(connection)) instanceof IItemPipePriority) {
					priorities.add(connection);
				}
			}
			if (priorities.size() > 0){
				if (lastReceived.getX() != 0 || lastReceived.getY() != 0 || lastReceived.getZ() != 0){
					for (int i = 0; i < 1; i ++){
						if (!inventory.getStackInSlot(0).isEmpty()){
							EnumFacing face = priorities.get(random.nextInt(priorities.size()));
							TileEntity tile = getWorld().getTileEntity(getPos().offset(face));
							if (tile != null){
								if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite())){
									IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
									if (handler != null){
										ItemStack passStack = this.inventory.extractItem(0, 1, true);
										int slot = -1;
										for (int j = 0; j < handler.getSlots() && slot == -1; j ++){
											if (handler.insertItem(j,passStack,true).isEmpty()){
												slot = j;
											}
										}
										if (slot != -1){
											ItemStack added = handler.insertItem(slot, passStack, false);
											if (added.isEmpty()){
												itemsMoved = true;
												ItemStack extracted = this.inventory.extractItem(0, 1, false);
												if (!extracted.isEmpty()){
													if (tile instanceof TileEntityItemPipe){
														((TileEntityItemPipe)tile).lastReceived = getPos();
													}
													if (!toUpdate.contains(getPos().offset(face))){
														toUpdate.add(getPos().offset(face));
													}
													if (!toUpdate.contains(getPos())){
														toUpdate.add(getPos());
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if (toUpdate.size() > 0){
					return;
				}
			}
			if (connections.size() > 0){
				if (lastReceived.getX() != 0 || lastReceived.getY() != 0 || lastReceived.getZ() != 0){
					for (int i = 0; i < 1; i ++){
						if (!inventory.getStackInSlot(0).isEmpty()){
							EnumFacing face = connections.get(random.nextInt(connections.size()));
							TileEntity tile = getWorld().getTileEntity(getPos().offset(face));
							if (tile != null){
								if (tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite())){
									IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face.getOpposite());
									if (handler != null){
										ItemStack passStack = this.inventory.extractItem(0, 1, true);
										int slot = -1;
										for (int j = 0; j < handler.getSlots() && slot == -1; j ++){
											if (handler.insertItem(j,passStack,true).isEmpty()){ //We can do it this way chiefly because the passStack has size 1
												slot = j;
											}
										}
										if (slot != -1){
											ItemStack added = handler.insertItem(slot, passStack, false);
											if (added.isEmpty()){
												ItemStack extracted = this.inventory.extractItem(0, 1, false);
												itemsMoved = true;
												if (!extracted.isEmpty()){
													if (tile instanceof TileEntityItemPipe){
														((TileEntityItemPipe)tile).lastReceived = getPos();
													}
													if (!toUpdate.contains(getPos().offset(face))){
														toUpdate.add(getPos().offset(face));
													}
													if (!toUpdate.contains(getPos())){
														toUpdate.add(getPos());
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if(inventory.getStackInSlot(0).isEmpty())
				itemsMoved = true;
			if(clogged == itemsMoved) {
				clogged = !itemsMoved;
				markDirty();
			}
		}
	}

	@Override
	public int getPressure() {
		return pressure;
	}

	@Override
	public void setPressure(int pressure) {
		this.pressure = pressure;
	}

	@Override
	public void markDirty() {
		super.markDirty();
		Misc.syncTE(this);
	}
}
