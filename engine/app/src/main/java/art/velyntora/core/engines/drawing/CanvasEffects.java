package art.velyntora.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


/** Owns engines/drawing behavior; the host is the workspace composition facade. */
final class CanvasEffects {
  private final DrawingView host;
  CanvasEffects(DrawingView host) { this.host = host; }

  boolean utility(int kind, int amount, int size, int parameter) {
    return host.nativeUtility(kind, amount, size, parameter);
  }

  boolean objectEffect(int kind, int amount, int tolerance, boolean option) {
    return host.nativeObject(kind, amount, tolerance, host.readColor(), option);
  }

  boolean render(int kind, int scale, int detail, int seed, int second) {
    return host.nativeRender(kind, scale, detail, seed, host.readColor(), second);
  }

  boolean artistic(int kind, int strength, int radius, int threshold) {
    return host.nativeArtistic(kind, strength, radius, threshold);
  }

  boolean distortion(int kind, int amount, int size, int angle, int cx, int cy) {
    return host.nativeDistortion(kind, amount, size, angle, cx, cy);
  }

  boolean blur(int kind, int amount, int angle, int cx, int cy) {
    return host.nativeBlur(kind, amount, angle, cx, cy);
  }

  boolean colorAdjustment(int kind, int[] values) {
    return host.nativeColorAdjustment(kind, values);
  }

  boolean applyEffect(int kind, int amount) {
    return host.nativeEffect(kind, amount);
  }

  boolean prepareEffectSelection() {
    return host.nativeSetEffectSelection(host.selectionMask());
  }

  void effectApplied() {
    host.refresh();
  }

  boolean flipActiveHorizontal() {
    if (!host.nativeFlipActiveHorizontal()) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean flipActiveVertical() {
    if (!host.nativeFlipActiveVertical()) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean rotateActive180() {
    if (!host.nativeRotateActive180()) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean rotateActive90(boolean clockwise) {
    if (!host.nativeRotateActive90(clockwise)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean adjustChannelActive(int channel, int adjustment) {
    if (!host.nativeAdjustChannelActive(channel, adjustment)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean liftShadowsActive(int floor) {
    if (!host.nativeLiftShadowsActive(floor)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean clampHighlightsActive(int ceiling) {
    if (!host.nativeClampHighlightsActive(ceiling)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean quantizeActive(int step) {
    if (!host.nativeQuantizeActive(step)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean normalizeActive() {
    if (!host.nativeNormalizeActive()) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean removeChannelActive(int channel) {
    if (!host.nativeRemoveChannelActive(channel)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean adjustAlphaActive(int percent) {
    if (!host.nativeAdjustAlphaActive(percent)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean grayscaleFromChannelActive(int channel) {
    if (!host.nativeDesaturateChannelActive(channel)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean exposureActive(int percent) {
    if (!host.nativeExposureActive(percent)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean levelsActive(int blackPoint, int whitePoint) {
    if (!host.nativeLevelsActive(blackPoint, whitePoint)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean hueRotateActive(int degrees) {
    if (!host.nativeHueRotateActive(degrees)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean colorBalanceActive(int redPercent, int greenPercent, int bluePercent) {
    if (!host.nativeColorBalanceActive(redPercent, greenPercent, bluePercent)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean swapChannelsActive(int mode) {
    if (!host.nativeSwapChannelsActive(mode)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean tintActive(int red, int green, int blue) {
    if (!host.nativeTintActive(red, green, blue)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean gammaActive(int percent) {
    if (!host.nativeGammaActive(percent)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean saturationActive(int adjustment) {
    if (!host.nativeSaturationActive(adjustment)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean solarizeActive(int threshold) {
    if (!host.nativeSolarizeActive(threshold)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean posterizeActive(int levels) {
    if (!host.nativePosterizeActive(levels)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean thresholdActive(int threshold) {
    if (!host.nativeThresholdActive(threshold)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean contrastActive(int adjustment) {
    if (!host.nativeContrastActive(adjustment)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean brightnessActive(int adjustment) {
    if (!host.nativeBrightnessActive(adjustment)) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean sepiaActive() {
    if (!host.nativeSepiaActive()) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean grayscaleActive() {
    if (!host.nativeGrayscaleActive()) return false;
    host.deselect();
    host.refresh();
    return true;
  }

  boolean invertActiveColors() {
    if (!host.nativeInvertActiveColors()) return false;
    host.deselect();
    host.refresh();
    return true;
  }
}
