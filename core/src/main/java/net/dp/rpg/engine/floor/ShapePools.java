package net.dp.rpg.engine.floor;

public final class ShapePools {

  public static final ShapePool STANDARD = ShapePool.named("standard")
      .with(Shapes.SINGLE, 0.800)
      .with(Shapes.WIDE, 0.050)
      .with(Shapes.CORNER, 0.025)
      .with(Shapes.SQUARE, 0.025)
      .with(Shapes.JUNCTION, 0.025)
      .with(Shapes.HORSESHOE, 0.025)
      .with(Shapes.BEAM, 0.025)
      .with(Shapes.RING, 0.025)
      .build();

  public static final ShapePool ARENA = ShapePool.named("arena")
      .with(Shapes.SINGLE, 0.900)
      .with(Shapes.WIDE, 0.050)
      .with(Shapes.CORNER, 0.025)
      .with(Shapes.SQUARE, 0.025)
      .build();

  public static final ShapePool CHAMBER = ShapePool.named("chamber")
      .with(Shapes.SINGLE, 1.000)
      .build();

  private ShapePools() {
  }
}
