package com.fulllock.app;
/** Preference/RPC names and defaults shared by settings and injected drawing. */
public final class OverlayPalette {
 public static final String[] KEYS={"rootOverlayBackground","rootOverlayText","rootOverlayLabel","rootOverlayBorder","pickupOverlayBackground","pickupOverlayText","pickupOverlayBorder"};
 public static int fallback(String key){return switch(key){case "rootOverlayText"->GozzaDesign.textPrimary;case "rootOverlayBackground","pickupOverlayBackground"->GozzaDesign.surface;default->GozzaDesign.primary;};}
 private OverlayPalette(){}
}
