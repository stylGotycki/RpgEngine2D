package net.dp.rpg.game;

public final class FixtureCategory
{
    public static short WALL = 0x0001;
    public static short PLAYER_ATTACK = 0x0002;
    public static short ENEMY = 0x0004;
    public static short OBSTACLE = 0x0008;
    public static short SENSOR = 0x0010;
    public static short LIGHT = 0x0020;
    public static short PLAYER = 0x0040;
    public static short ENEMY_ATTACK = 0x0080;
    // Moved off 0x0080 when ENEMY_ATTACK came in on dev: a shared bit made items enemy attacks.
    public static short ITEM = 0x0200;
    public static short NPC = 0x0400;

    public static boolean isCategory(short bits, short... categories)
    {
        int categoriesCombined = 0;
        for(short category : categories)
        {
            categoriesCombined |= category;
        }
        return (bits & categoriesCombined) != 0;
    }
}
