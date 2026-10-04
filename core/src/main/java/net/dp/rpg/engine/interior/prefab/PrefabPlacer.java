package net.dp.rpg.engine.interior.prefab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.dp.rpg.engine.interior.corpus.CorpusScanner;
import net.dp.rpg.engine.interior.corpus.PrefabAnchor;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.model.TileClasses;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.wfc.WfcGrid;

public final class PrefabPlacer {

  private static final int DOOR_CLEARANCE = 1;

  private final TileClasses classes;

  public PrefabPlacer(TileClasses classes) {
    if (classes == null) {
      throw new IllegalArgumentException("Tile classes must not be null");
    }

    this.classes = classes;
  }

  public Placement place(RoomCanvas canvas, Prefab prefab, Random random) {
    List<Candidate> candidates = candidatesFor(canvas, prefab);

    Collections.shuffle(candidates, random);

    for (Candidate candidate : candidates) {
      if (tryCommit(canvas, candidate)) {
        return new Placement(prefab, candidate.orientation(), candidate.originX(),
            candidate.originY());
      }
    }

    return null;
  }

  private List<Candidate> candidatesFor(RoomCanvas canvas, Prefab prefab) {
    return switch (prefab.anchor()) {
      case WALL -> wallCandidates(canvas, prefab);
      case CELL_CENTER -> cellCenterCandidates(canvas, prefab);
      case ANY -> anyCandidates(canvas, prefab);
    };
  }

  private List<Candidate> wallCandidates(RoomCanvas canvas, Prefab prefab) {
    List<Candidate> candidates = new ArrayList<>();
    Direction drawnFacing = prefab.drawnFacing();

    for (Direction facing : Direction.values()) {
      for (PrefabOrientation orientation : PrefabOrientation.allFacing(prefab, drawnFacing,
          facing)) {
        addWallRunCandidates(canvas, orientation, facing, candidates);
      }
    }

    return candidates;
  }

  private void addWallRunCandidates(RoomCanvas canvas, PrefabOrientation orientation,
                                    Direction facing, List<Candidate> candidates) {
    int width = canvas.width();
    int height = canvas.height();

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (!fitsAgainstWall(canvas, orientation, facing, x, y)) {
          continue;
        }

        candidates.add(new Candidate(orientation, x, y));
      }
    }
  }

  private boolean fitsAgainstWall(RoomCanvas canvas, PrefabOrientation orientation,
                                  Direction facing, int x, int y) {
    if (!fitsInInterior(canvas, orientation, x, y)) {
      return false;
    }

    Direction back = facing.opposite();
    int width = orientation.width();
    int height = orientation.height();

    return switch (back) {
      case NORTH -> backRowTouchesWall(canvas, x, y - 1, width, true);
      case SOUTH -> backRowTouchesWall(canvas, x, y + height, width, true);
      case WEST -> backRowTouchesWall(canvas, x - 1, y, height, false);
      case EAST -> backRowTouchesWall(canvas, x + width, y, height, false);
    };
  }

  private boolean backRowTouchesWall(RoomCanvas canvas, int fixedX, int fixedY, int span,
                                     boolean horizontalSpan) {
    for (int along = 0; along < span; along++) {
      int x = horizontalSpan ? fixedX + along : fixedX;
      int y = horizontalSpan ? fixedY : fixedY + along;

      if (!canvas.zones().isInside(x, y) || canvas.zones().zoneAt(x, y) != Zone.WALL) {
        return false;
      }
    }

    return true;
  }

  private List<Candidate> cellCenterCandidates(RoomCanvas canvas, Prefab prefab) {
    List<Candidate> candidates = new ArrayList<>();

    for (PrefabOrientation orientation : PrefabOrientation.allFacing(prefab, Direction.NORTH,
        Direction.NORTH)) {
      for (var cell : canvas.blueprint().shape().cells()) {
        int x = cell.centerTileX() - orientation.width() / 2;
        int y = cell.centerTileY() - orientation.height() / 2;

        if (fitsInInterior(canvas, orientation, x, y)) {
          candidates.add(new Candidate(orientation, x, y));
        }
      }
    }

    return candidates;
  }

  private List<Candidate> anyCandidates(RoomCanvas canvas, Prefab prefab) {
    List<Candidate> candidates = new ArrayList<>();

    for (PrefabOrientation orientation : PrefabOrientation.allFacing(prefab, Direction.NORTH,
        Direction.NORTH)) {
      for (int y = 0; y < canvas.height(); y++) {
        for (int x = 0; x < canvas.width(); x++) {
          if (fitsInInterior(canvas, orientation, x, y)) {
            candidates.add(new Candidate(orientation, x, y));
          }
        }
      }
    }

    return candidates;
  }

  private boolean fitsInInterior(RoomCanvas canvas, PrefabOrientation orientation, int x, int y) {
    for (int dy = 0; dy < orientation.height(); dy++) {
      for (int dx = 0; dx < orientation.width(); dx++) {
        int tileX = x + dx;
        int tileY = y + dy;

        if (!canvas.zones().isInside(tileX, tileY)
            || canvas.zones().zoneAt(tileX, tileY) != Zone.INTERIOR
            || canvas.ground().isFixed(tileX, tileY)
            || nearDoorApproach(canvas, tileX, tileY)) {
          return false;
        }
      }
    }

    return true;
  }

  private boolean nearDoorApproach(RoomCanvas canvas, int x, int y) {
    for (var edge : canvas.blueprint().doors().keySet()) {
      int approachX = edge.doorTileX() + edge.direction().opposite().getDeltaX();
      int approachY = edge.doorTileY() + edge.direction().opposite().getDeltaY();

      if (Math.abs(x - approachX) <= DOOR_CLEARANCE && Math.abs(y - approachY) <= DOOR_CLEARANCE) {
        return true;
      }
    }

    return false;
  }

  private boolean tryCommit(RoomCanvas canvas, Candidate candidate) {
    WfcGrid trial = canvas.ground().copy();

    stamp(trial, candidate);

    if (!trial.newState().isConsistent()) {
      return false;
    }

    stamp(canvas.ground(), candidate);
    placeObjects(canvas, candidate);

    return true;
  }

  private void stamp(WfcGrid grid, Candidate candidate) {
    PrefabOrientation orientation = candidate.orientation();
    TileGrid ground = orientation.ground();

    for (int dy = 0; dy < orientation.height(); dy++) {
      for (int dx = 0; dx < orientation.width(); dx++) {
        int runtimeId = ground.get(dx, dy);

        if (runtimeId != TileGrid.EMPTY) {
          grid.fix(candidate.originX() + dx, candidate.originY() + dy, classes.classOf(runtimeId));
        }
      }
    }
  }

  private void placeObjects(RoomCanvas canvas, Candidate candidate) {
    PrefabOrientation orientation = candidate.orientation();

    for (int dy = 0; dy < orientation.height(); dy++) {
      for (int dx = 0; dx < orientation.width(); dx++) {
        int runtimeId = orientation.details().get(dx, dy);

        if (runtimeId != TileGrid.EMPTY) {
          canvas.setDetail(candidate.originX() + dx, candidate.originY() + dy, runtimeId);
        }
      }
    }

    for (TileMapObject object : orientation.objects()) {
      canvas.addObject(new TileMapObject(object.id(), object.name(), object.type(),
          object.x() + candidate.originX(), object.y() + candidate.originY(), object.width(),
          object.height(), object.properties()));
    }
  }

  private record Candidate(PrefabOrientation orientation, int originX, int originY) {
  }

  public record Placement(Prefab prefab, PrefabOrientation orientation, int originX, int originY) {

    public List<TileMapObject> accessPoints() {
      return orientation.objects().stream()
          .filter(object -> object.isType(CorpusScanner.ACCESS_TYPE))
          .map(object -> new TileMapObject(object.id(), object.name(), object.type(),
              object.x() + originX, object.y() + originY, object.width(), object.height(),
              object.properties()))
          .toList();
    }
  }
}
