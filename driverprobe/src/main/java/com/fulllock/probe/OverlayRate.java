package com.fulllock.probe;
import com.fulllock.app.*;
import org.json.JSONObject;
import java.math.RoundingMode;
/** Scheduled overlay ignores pickup distance; price formula and fee still follow the profile. */
final class OverlayRate {
 static String text(JSONObject rules,ScreenCardParser.Card card){var rate="งานล่วงหน้า".equals(card.job().type())?scheduledRate(rules,card):RootRules.rate(rules,card,card.routeKm());return rate==null?"—":"฿"+rate.setScale(2,RoundingMode.HALF_UP).toPlainString();}
 private static java.math.BigDecimal scheduledRate(JSONObject rules,ScreenCardParser.Card card){Double km=card.routeKm();return km==null||!Double.isFinite(km)||km<=0?null:RootRules.comparisonPrice(rules,card).divide(java.math.BigDecimal.valueOf(km),8,RoundingMode.HALF_UP);}
}
