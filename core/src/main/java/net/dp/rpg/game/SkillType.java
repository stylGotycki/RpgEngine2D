package net.dp.rpg.game;

public enum SkillType
{
    STRENGTH,
    AGILITY,
    WISDOM;

    public String getColorString()
    {
        String color = "";
        switch(this)
        {
            case STRENGTH -> color = "red";
            case AGILITY -> color = "green";
            case WISDOM -> color = "blue";
        }
        return color;
    }
}
