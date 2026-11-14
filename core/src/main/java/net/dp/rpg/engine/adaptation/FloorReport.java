package net.dp.rpg.engine.adaptation;

public record FloorReport(int floor, long seed, FloorMetrics metrics, PlayerProfile measured,
    PlayerProfile before, PlayerProfile after, Adaptation next) {
}
