
package org.magnos.game.net;

public enum Match
{
	EXACT()
	{
		public final boolean isMatch( int set, int input )
		{
			return (set == input);
		}
	},
	ALL()
	{
		public final boolean isMatch( int set, int input )
		{
			return (set & input) == input;
		}
	},
	ANY_OF()
	{
		public final boolean isMatch( int set, int input )
		{
			return (set & input) != 0;
		}
	},
	NONE()
	{
		public final boolean isMatch( int set, int input )
		{
			return (set & input) == 0;
		}
	},
	NOT_ALL()
	{
		public final boolean isMatch( int set, int input )
		{
			return (set & input) != input;
		}
	};

	public abstract boolean isMatch( int set, int input );
}
