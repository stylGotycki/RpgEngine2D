package net.dp.rpg.game.item;

import java.text.Normalizer;

public final class Texts {

  private Texts() {
  }

  // The skin's bitmap fonts have no Polish glyphs; drop the marks until a font with them is added.
  public static String ascii(String text) {
    String folded = text.replace('ł', 'l').replace('Ł', 'L');

    return Normalizer.normalize(folded, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
  }
}
