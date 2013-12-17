
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
	},
    ALWAYS()
    {
        public final boolean isMatch( int set, int input )
        {
            return true;
        }
    },
    NEVER()
    {
        public final boolean isMatch( int set, int input )
        {
            return false;
        }
    };

	public abstract boolean isMatch( int set, int input );
}
