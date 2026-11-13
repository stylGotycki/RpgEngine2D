package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.engine.puzzle.CircuitBoard;
import net.dp.rpg.engine.puzzle.PuzzleSpec;
import net.dp.rpg.engine.puzzle.SequenceGame;
import net.dp.rpg.engine.puzzle.ToggleBoard;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.events.GameMessageEvent;
import net.dp.rpg.game.events.InteractEvent;
import net.dp.rpg.game.events.PuzzleSolvedEvent;

public class PuzzleScript extends EntityScript implements GameEventListener {

  private static final Color IDLE = new Color(0.55f, 0.55f, 0.58f, 1f);

  private static final Color POWER = new Color(0.45f, 0.9f, 1f, 1f);

  private static final Color LIT = new Color(1f, 0.85f, 0.3f, 1f);

  private static final Color GOOD = new Color(0.5f, 1f, 0.5f, 1f);

  private static final Color WRONG = new Color(1f, 0.3f, 0.25f, 1f);

  private static final float SEQUENCE_SLOT = 0.7f;

  private static final float SEQUENCE_LIGHT = 0.45f;

  private static final int SEQUENCE_LENGTH = 4;

  private final PuzzleSpec spec;

  private final TileRoom room;

  private final TileFloor floor;

  private int player = -1;

  private final TextureAtlas atlas;

  private final Consumer<PuzzleScript> onSolved;

  private final List<Sprite> sprites = new ArrayList<>();

  private final Random random = new Random();

  private CircuitBoard circuit;

  private Sprite[] cells;

  private Sprite sink;

  private ToggleBoard toggle;

  private SequenceGame sequence;

  private List<Integer> playback = List.of();

  private float playbackTime;

  private float flashTime;

  private Color flashColour;

  private int lastX = Integer.MIN_VALUE;

  private int lastY = Integer.MIN_VALUE;

  private boolean hinted;

  private boolean logged;

  private boolean solved;

  // Built before the player entity exists; the level hands it over with setPlayer.
  public PuzzleScript(PuzzleSpec spec, TileRoom room, TileFloor floor, TextureAtlas atlas,
      Consumer<PuzzleScript> onSolved) {
    this.spec = spec;
    this.room = room;
    this.floor = floor;
    this.atlas = atlas;
    this.onSolved = onSolved;

    Random board = new Random(spec.seed());
    switch (spec.kind()) {
      case CIRCUIT -> buildCircuit(board);
      case TOGGLE -> buildToggle(board);
      case SEQUENCE -> buildSequence();
      default -> throw new IllegalStateException();
    }
    refresh();
  }

  public void setPlayer(int entity) {
    this.player = entity;
  }

  public List<Sprite> sprites() {
    return sprites;
  }

  public PuzzleSpec spec() {
    return spec;
  }

  public TileRoom room() {
    return room;
  }

  public boolean isSolved() {
    return solved;
  }

  private void buildCircuit(Random board) {
    circuit = CircuitBoard.generate(PuzzleSpec.CIRCUIT_BOARD_WIDTH, 3, board);
    cells = new Sprite[circuit.width() * circuit.height()];

    for (int y = 0; y < circuit.height(); y++) {
      for (int x = 0; x < circuit.width(); x++) {
        String region = circuit.piece(x, y) == CircuitBoard.Piece.STRAIGHT
            ? "puzzle/pipe_straight" : "puzzle/pipe_corner";
        cells[y * circuit.width() + x] = sprite(region, spec.circuitCell(x, y));
      }
    }

    sprite("puzzle/crystal", spec.circuitCell(-1, circuit.sourceRow())).setColor(POWER);
    sink = sprite("puzzle/crystal", spec.circuitCell(circuit.width(), circuit.sinkRow()));
  }

  private void buildToggle(Random board) {
    toggle = ToggleBoard.generate(PuzzleSpec.TOGGLE_BOARD_WIDTH, PuzzleSpec.TOGGLE_BOARD_HEIGHT,
        board);
    cells = new Sprite[toggle.width() * toggle.height()];

    for (int y = 0; y < toggle.height(); y++) {
      for (int x = 0; x < toggle.width(); x++) {
        cells[y * toggle.width() + x] = sprite("puzzle/plate_off", spec.toggleCell(x, y));
      }
    }
    sprite("puzzle/plate_reset", spec.toggleReset());
  }

  private void buildSequence() {
    sequence = new SequenceGame(PuzzleSpec.SEQUENCE_PLATES, SEQUENCE_LENGTH);
    cells = new Sprite[PuzzleSpec.SEQUENCE_PLATES];

    for (int i = 0; i < cells.length; i++) {
      cells[i] = sprite("puzzle/plate_rune", spec.sequencePlate(i));
    }
    sprite("puzzle/plate_start", spec.sequenceStart());
  }

  private Sprite sprite(String region, int[] tile) {
    TextureRegion texture = atlas.findRegion(region);

    if (texture == null) {
      throw new IllegalStateException("No " + region + " in the atlas; repack textures");
    }

    Sprite sprite = new Sprite(texture);
    sprite.setBounds(worldX(tile[0]), worldY(tile[1]), 1f, 1f);
    sprite.setOriginCenter();
    sprites.add(sprite);

    return sprite;
  }

  private float worldX(int tileX) {
    return room.origin().x() * RoomGeometry.CELL_WIDTH + tileX;
  }

  private float worldY(int tileY) {
    return room.origin().y() * RoomGeometry.CELL_HEIGHT + tileY;
  }

  @Override
  public void update(float delta) {
    if (floor.getActiveRoom() != room || player < 0) {
      return;
    }
    if (!logged) {
      logged = true;
      Vector2 at = getScene().getComponentStorage(TransformComponent.class).getByEntity(player)
          .position;
      Sprite first = sprites.get(0);
      Gdx.app.log("Puzzle", "%s in room %d: %d sprites, first at (%.1f, %.1f) size %.1f, "
          .formatted(spec.kind(), room.id(), sprites.size(), first.getX(), first.getY(),
              first.getWidth()) + "colour " + first.getColor() + ", player at (%.1f, %.1f)"
          .formatted(at.x, at.y));
    }

    if (flashTime > 0) {
      flashTime -= delta;
    }
    if (!playback.isEmpty()) {
      playbackTime += delta;
      if (playbackTime >= playback.size() * SEQUENCE_SLOT) {
        playback = List.of();
      }
    }

    Vector2 position = getScene().getComponentStorage(TransformComponent.class)
        .getByEntity(player).position;
    int x = (int) Math.floor(position.x - room.origin().x() * RoomGeometry.CELL_WIDTH);
    int y = (int) Math.floor(position.y - room.origin().y() * RoomGeometry.CELL_HEIGHT);

    if (x != lastX || y != lastY) {
      lastX = x;
      lastY = y;
      onStep(x, y);
    }
    refresh();
  }

  private void onStep(int x, int y) {
    if (spec.contains(x, y) && !hinted && !solved) {
      hinted = true;
      message(hint());
    }
    if (solved) {
      return;
    }

    switch (spec.kind()) {
      case TOGGLE -> {
        int[] reset = spec.toggleReset();

        if (x == reset[0] && y == reset[1]) {
          toggle.reset();
        } else {
          for (int row = 0; row < toggle.height(); row++) {
            for (int column = 0; column < toggle.width(); column++) {
              int[] cell = spec.toggleCell(column, row);

              if (cell[0] == x && cell[1] == y) {
                toggle.step(column, row);
              }
            }
          }
        }
        if (toggle.isSolved()) {
          solve();
        }
      }
      case SEQUENCE -> stepSequence(x, y);
      default -> {
      }
    }
  }

  private void stepSequence(int x, int y) {
    int[] start = spec.sequenceStart();

    if (x == start[0] && y == start[1]) {
      if (playback.isEmpty()) {
        playback = sequence.start(random);
        playbackTime = 0;
      }
      return;
    }
    if (!playback.isEmpty()) {
      return;
    }

    for (int i = 0; i < PuzzleSpec.SEQUENCE_PLATES; i++) {
      int[] plate = spec.sequencePlate(i);

      if (plate[0] == x && plate[1] == y) {
        switch (sequence.step(i)) {
          case CORRECT -> flash(GOOD, 0.25f);
          case WRONG -> {
            flash(WRONG, 0.8f);
            message("Zła kolejność. Stań na złotej płytce, aby zobaczyć nową sekwencję.");
          }
          case SOLVED -> solve();
          default -> {
          }
        }
      }
    }
  }

  @Override
  public void onEvent(GameEvent event) {
    if (event instanceof InteractEvent interact && circuit != null && !solved
        && floor.getActiveRoom() == room) {
      int x = (int) Math.floor(interact.x - room.origin().x() * RoomGeometry.CELL_WIDTH);
      int y = (int) Math.floor(interact.y - room.origin().y() * RoomGeometry.CELL_HEIGHT);

      for (int row = 0; row < circuit.height(); row++) {
        for (int column = 0; column < circuit.width(); column++) {
          int[] cell = spec.circuitCell(column, row);

          if (cell[0] == x && cell[1] == y) {
            circuit.rotate(column, row);
          }
        }
      }
      if (circuit.isSolved()) {
        solve();
      }
      refresh();
    }
  }

  private void solve() {
    solved = true;
    flash(GOOD, 1f);
    message("Zagadka rozwiązana!");
    Engine.eventBus.publish(PuzzleSolvedEvent.class, e -> {
      e.kind = spec.kind();
      e.questId = spec.questId();
      e.room = room;
    });
    onSolved.accept(this);
  }

  private void flash(Color colour, float seconds) {
    flashColour = colour;
    flashTime = seconds;
  }

  private void refresh() {
    switch (spec.kind()) {
      case CIRCUIT -> refreshCircuit();
      case TOGGLE -> {
        for (int y = 0; y < toggle.height(); y++) {
          for (int x = 0; x < toggle.width(); x++) {
            Sprite cell = cells[y * toggle.width() + x];
            cell.setRegion(atlas.findRegion(
                toggle.isOn(x, y) ? "puzzle/plate_on" : "puzzle/plate_off"));
            cell.setColor(solved ? GOOD : Color.WHITE);
          }
        }
      }
      case SEQUENCE -> refreshSequence();
      default -> {
      }
    }
  }

  private void refreshCircuit() {
    for (int y = 0; y < circuit.height(); y++) {
      for (int x = 0; x < circuit.width(); x++) {
        Sprite cell = cells[y * circuit.width() + x];
        // Pieces turn clockwise; libGDX rotation is counter-clockwise.
        cell.setRotation(-90f * circuit.rotation(x, y));
        cell.setColor(IDLE);
      }
    }
    for (int[] powered : circuit.powered()) {
      if (powered[0] < circuit.width()) {
        cells[powered[1] * circuit.width() + powered[0]].setColor(solved ? GOOD : POWER);
      }
    }
    sink.setColor(circuit.isSolved() ? POWER : IDLE);
  }

  private void refreshSequence() {
    int lit = -1;

    if (!playback.isEmpty()) {
      int slot = (int) (playbackTime / SEQUENCE_SLOT);

      if (slot < playback.size() && playbackTime - slot * SEQUENCE_SLOT < SEQUENCE_LIGHT) {
        lit = playback.get(slot);
      }
    }
    for (int i = 0; i < cells.length; i++) {
      Color colour = solved ? GOOD : i == lit ? LIT
          : flashTime > 0 && flashColour != null ? flashColour : IDLE;
      cells[i].setColor(colour);
    }
  }

  private String hint() {
    return switch (spec.kind()) {
      case CIRCUIT ->
          "Stań na elemencie i naciśnij E, aby go obrócić. Połącz kryształy mocą.";
      case TOGGLE ->
          "Każdy krok przełącza płytkę. Zapal wszystkie. Czerwona płytka resetuje.";
      case SEQUENCE -> "Stań na złotej płytce, zapamiętaj kolejność i powtórz ją.";
    };
  }

  private void message(String text) {
    Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = text);
  }
}
