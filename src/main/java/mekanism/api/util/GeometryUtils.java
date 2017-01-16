package mekanism.api.util;

public class GeometryUtils
{
	/**
	 * Check if a given point is inside a sphere
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param radius
	 * @return true or false
	 */
	public static boolean isInsideSphere( int x, int y, int z, int radius )
	{
		return Math.pow( x, 2 ) + Math.pow( y, 2 ) + Math.pow( z, 2 ) - Math.pow( radius, 2 ) <= 0;
	}
	
	/**
	 * Check if a given point is on the surface of a sphere
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param radius
	 * @return true or false
	 */
	public static boolean isOnSurface( int x, int y, int z, int radius )
	{
		int setCount = 0;
		int unsetCount = 0;
		
		if( isInsideSphere( x+1, y, z, radius) ) setCount++; else unsetCount++;
		if( isInsideSphere( x-1, y, z, radius) ) setCount++; else unsetCount++;

		if( isInsideSphere( x, y+1, z, radius) ) setCount++; else unsetCount++;
		if( isInsideSphere( x, y-1, z, radius) ) setCount++; else unsetCount++;

		if( isInsideSphere( x, y, z+1, radius) ) setCount++; else unsetCount++;
		if( isInsideSphere( x, y, z-1, radius) ) setCount++; else unsetCount++;

		return setCount > 0 && unsetCount > 0;
	}

}
