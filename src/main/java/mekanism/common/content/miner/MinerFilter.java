package mekanism.common.content.miner;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import mekanism.common.util.MekanismUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

public abstract class MinerFilter
{
	public ItemStack replaceStack;
	public boolean requireStack;

	// A list of locations where ores matching this filter have been found
	public List<BlockPos> foundOres = new ArrayList<BlockPos>();
	
	public abstract boolean canFilter(ItemStack itemStack);

	public NBTTagCompound writeToNBT(NBTTagCompound nbtTags)
	{
		nbtTags.setBoolean("requireStack", requireStack);

		if(replaceStack != null)
		{
			nbtTags.setTag("replaceStack", replaceStack.writeToNBT(new NBTTagCompound()));
		}
		
		if( !foundOres.isEmpty() )
		{
			NBTTagList tagList = new NBTTagList();
			NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setInteger( "oreCount", foundOres.size() );
			
			int[] array = new int[ foundOres.size() * 2 ];
			for( int i = 0 ; i < foundOres.size() ; i++ )
			{
				long pos = foundOres.get( i ).toLong();
				array[ i * 2     ] = (int)pos;
				array[ i * 2 + 1 ] = (int)(pos >> 32);
			}
			tagCompound.setIntArray( "offsetArray", array );
			tagList.appendTag( tagCompound );
			nbtTags.setTag( "foundOres", tagList );
		}
		
		return nbtTags;
	}

	protected void read(NBTTagCompound nbtTags)
	{
		requireStack = nbtTags.getBoolean("requireStack");
		
		if(nbtTags.hasKey("replaceStack"))
		{
			replaceStack = ItemStack.loadItemStackFromNBT(nbtTags.getCompoundTag("replaceStack"));
		}
		
		if(nbtTags.hasKey( "foundOres" ))
		{
			foundOres.clear();
			int oreCount = nbtTags.getInteger( "oreCount" );
			int[] array = nbtTags.getIntArray( "offsetArray" );

			for( int i = 0 ; i < oreCount ; i++ )
			{
				long pos = (long)array[ i * 2 + 1 ] << 32 | array[ i * 2 ];
				foundOres.add( BlockPos.fromLong( pos ) );
			}
		}
	}

	public void write(ArrayList<Object> data)
	{
		data.add(requireStack);
		
		if(replaceStack != null)
		{
			data.add(true);
			data.add(MekanismUtils.getID(replaceStack));
			data.add(replaceStack.getItemDamage());
		}
		else {
			data.add(false);
		}
	}

	protected void read(ByteBuf dataStream)
	{
		requireStack = dataStream.readBoolean();
		
		if(dataStream.readBoolean())
		{
			replaceStack = new ItemStack(Item.getItemById(dataStream.readInt()), 1, dataStream.readInt());
		}
		else {
			replaceStack = null;
		}
	}

	public static MinerFilter readFromNBT(NBTTagCompound nbtTags)
	{
		int type = nbtTags.getInteger("type");

		MinerFilter filter = null;

		if(type == 0)
		{
			filter = new MItemStackFilter();
		}
		else if(type == 1)
		{
			filter = new MOreDictFilter();
		}
		else if(type == 2)
		{
			filter = new MMaterialFilter();
		}
		else if(type == 3)
		{
			filter = new MModIDFilter();
		}

		filter.read(nbtTags);

		return filter;
	}

	public static MinerFilter readFromPacket(ByteBuf dataStream)
	{
		int type = dataStream.readInt();

		MinerFilter filter = null;

		if(type == 0)
		{
			filter = new MItemStackFilter();
		}
		else if(type == 1)
		{
			filter = new MOreDictFilter();
		}
		else if(type == 2)
		{
			filter = new MMaterialFilter();
		}
		else if(type == 3)
		{
			filter = new MModIDFilter();
		}

		filter.read(dataStream);

		return filter;
	}

	@Override
	public boolean equals(Object filter)
	{
		return filter instanceof MinerFilter;
	}
}
