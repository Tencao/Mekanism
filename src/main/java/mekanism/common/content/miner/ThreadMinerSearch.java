package mekanism.common.content.miner;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import mekanism.api.Chunk3D;
import mekanism.api.Coord4D;
import mekanism.api.MekanismConfig.general;
import mekanism.api.util.GeometryUtils;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.tile.TileEntityDigitalMiner;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.IFluidBlock;

public class ThreadMinerSearch extends Thread
{
	public TileEntityDigitalMiner digitalMiner;

	public State state = State.IDLE;

	public Map<Chunk3D, BitSet> oresToMine = new HashMap<Chunk3D, BitSet>();
	public Map<Integer, MinerFilter> replaceMap = new HashMap<Integer, MinerFilter>();

	public Map<IBlockState, MinerFilter> acceptedItems = new HashMap<IBlockState, MinerFilter>();

	public int found = 0;

	public ThreadMinerSearch(TileEntityDigitalMiner tileEntity)
	{
		digitalMiner = tileEntity;
	}

	@Override
	public void run()
	{
		state = State.SEARCHING;

		if(!digitalMiner.inverse && digitalMiner.filters.isEmpty())
		{
			state = State.FINISHED;
			return;
		}
		
		Coord4D coord = digitalMiner.getStartingCoord();
		int diameter = digitalMiner.getDiameter();
		int size = digitalMiner.getTotalSize();

		// Reset filters
		for(MinerFilter filter : digitalMiner.filters)
		{
			filter.foundOres.clear();
		}

		for(int i = 0; i < size; i++)
		{
			int x = coord.xCoord+i%diameter;
			int z = coord.zCoord+(i/diameter)%diameter;
			// Start at top or bottom, depending on operation mode
			int y = general.minerAltOperation ? coord.yCoord-(i/diameter/diameter) : coord.yCoord+(i/diameter/diameter);

			if(digitalMiner.isInvalid())
			{
				return;
			}

			try {
				if( y < 0 )
				{
					// Sanity check - shouldn't be needed, but just in case
					// Skip blocks outside map bounds
					continue;
				}
				if(digitalMiner.getPos().getX() == x && digitalMiner.getPos().getY() == y && digitalMiner.getPos().getZ() == z)
				{
					// Skip block containing miner
					continue;
				}
	
				if(digitalMiner.getWorld().getChunkProvider().getLoadedChunk(x >> 4, z >> 4) == null)
				{
					// Skip unloaded chunks
					continue;
				}
	
				TileEntity tileEntity = digitalMiner.getWorld().getTileEntity(new BlockPos(x, y, z));
				
				if(tileEntity instanceof TileEntityBoundingBlock)
				{
					// Skip bounding blocks
					continue;
				}

				IBlockState blockState = digitalMiner.getWorld().getBlockState(new BlockPos(x, y, z));
				Block block = blockState.getBlock();
	
				if(block instanceof BlockLiquid || block instanceof IFluidBlock)
				{
					// Skip fluid blocks
					continue;
				}

				// Perform checks related to alternative operations
				if( general.minerAltOperation )
				{
					if( !GeometryUtils.isInsideSphere( x - digitalMiner.getPos().getX(), Math.max( y - digitalMiner.getPos().getY(), 0 ), z - digitalMiner.getPos().getZ(), diameter / 2 ) )
					{
						// Skip blocks outside operating boundaries
						continue;
					}
				}

				if(block != null && !digitalMiner.getWorld().isAirBlock(new BlockPos(x, y, z)) && blockState.getBlockHardness(digitalMiner.getWorld(), new BlockPos(x, y, z)) >= 0)
				{
					MinerFilter filterFound = null;
					boolean canFilter = false;
	
					if(acceptedItems.containsKey(blockState))
					{
						filterFound = acceptedItems.get(blockState);
					}
					else {
						ItemStack stack = new ItemStack(block, 1, block.getMetaFromState( blockState ));
	
						if(digitalMiner.isReplaceStack(stack))
						{
							continue;
						}
	
						for(MinerFilter filter : digitalMiner.filters)
						{
							if(filter.canFilter(stack))
							{
								filterFound = filter;
								break;
							}
						}
	
						acceptedItems.put(blockState, filterFound);
					}
					
					canFilter = digitalMiner.inverse ? filterFound == null : filterFound != null;
	
					if(canFilter)
					{
						set(i, new Coord4D(x, y, z, digitalMiner.getWorld().provider.getDimension()));
						replaceMap.put(i, filterFound);
						filterFound.foundOres.add( new BlockPos( x, y, z ) );
						
						found++;
					}
				}
			} catch(Exception e) {}
		}

		state = State.FINISHED;
		digitalMiner.oresToMine = oresToMine;
		digitalMiner.replaceMap = replaceMap;
		MekanismUtils.saveChunk(digitalMiner);
	}
	
	public void set(int i, Coord4D location)
	{
		Chunk3D chunk = new Chunk3D(location);
		
		if(oresToMine.get(chunk) == null)
		{
			oresToMine.put(chunk, new BitSet());
		}
		
		oresToMine.get(chunk).set(i);
	}

	public void reset()
	{
		state = State.IDLE;
	}

	public static enum State
	{
		IDLE("Not ready"),
		SEARCHING("Searching"),
		PAUSED("Paused"),
		FINISHED("Ready");

		public String desc;

		private State(String s)
		{
			desc = s;
		}
	}
}
