#include <jni.h>
#include "../../../../../core/document/history/HistoryArchive.hpp"
#include <unistd.h>

#include <algorithm>
#include <cerrno>
#include <cmath>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

#include "velyntora/ArtisticEffects.hpp"
#include "velyntora/BlurEffects.hpp"
#include "velyntora/Canvas.hpp"
#include "velyntora/ColorAdjustments.hpp"
#include "velyntora/DistortionEffects.hpp"
#include "velyntora/LayerDocument.hpp"
#include "velyntora/ObjectEffects.hpp"
#include "velyntora/PixelEffects.hpp"
#include "velyntora/ProjectIO.hpp"
#include "velyntora/RenderEffects.hpp"
#include "velyntora/SelectionTransform.hpp"
#include "velyntora/UtilityEffects.hpp"

namespace {
std::mutex guard;
std::unique_ptr<velyntora::Canvas> canvas;
std::unique_ptr<velyntora::LayerDocument> layers;
std::unique_ptr<velyntora::LayerDocument> importing;
int curveTag = 0;
std::uint64_t revision = 0, nextRevision = 0;
std::string historyDirectory;
std::shared_ptr<velyntora::history::HistoryArchive> historyArchive;
struct Snapshot {
  std::shared_ptr<velyntora::history::HistoryArchive> archive;
  velyntora::history::HistoryArchive::Entry entry{};
  std::shared_ptr<velyntora::LayerDocument> fallback;
  int curveTag = 0;
  std::uint64_t revision = 0;
};
std::vector<Snapshot> undoStack, redoStack;
struct ImportPrevious {
  std::unique_ptr<velyntora::Canvas> canvas;
  std::unique_ptr<velyntora::LayerDocument> layers;
  std::vector<Snapshot> undo,redo;
  std::shared_ptr<velyntora::history::HistoryArchive> archive;
  int tag;std::uint64_t revision;
};
std::unique_ptr<ImportPrevious> importPrevious;
Snapshot captureSnapshot() {
  Snapshot result;result.curveTag=curveTag;result.revision=revision;
  if(historyArchive) {
    try {result.entry=historyArchive->capture(*layers);result.archive=historyArchive;return result;}
    catch(const std::exception&) {historyArchive.reset();}
  }
  // A storage failure must never silently discard a user's undo steps.
  result.fallback=std::make_shared<velyntora::LayerDocument>(*layers);return result;
}
std::vector<std::uint8_t> brushMask, effectMask;
velyntora::tiles::LayerPixels sampledSource;
std::uint32_t sampledTarget;
void applyEffectSelection(velyntora::tiles::LayerPixels& pixels) {
  if (effectMask.empty()) return;
  if (effectMask.size() != pixels.size()) {
    pixels = canvas->pixels();
    return;
  }
  for (std::size_t i = 0; i < pixels.size(); i++)
    if (!effectMask[i]) pixels[i] = canvas->pixels()[i];
}
void checkpoint() {
  if (!canvas) return;
  undoStack.push_back(captureSnapshot());
  redoStack.clear();
  revision = ++nextRevision;
  curveTag = 0;
}
void storeActive() {
  curveTag = 0;
  if (layers && canvas) layers->replaceActivePixels(canvas->pixels());
}
// Brush edits touch only their swept bounds, not every pixel in the active layer.
void storeStrokeRegion(float x0,float y0,float x1,float y1,float radius) {
  if (!layers || !canvas || !std::isfinite(x0) || !std::isfinite(y0)
      || !std::isfinite(x1) || !std::isfinite(y1) || !std::isfinite(radius) || radius<=0) return;
  radius=std::min(radius,2048.f)+2;
  int left=int(std::clamp(std::floor(std::min(x0,x1)-radius),0.f,float(canvas->width())));
  int top=int(std::clamp(std::floor(std::min(y0,y1)-radius),0.f,float(canvas->height())));
  int right=int(std::clamp(std::ceil(std::max(x0,x1)+radius),0.f,float(canvas->width())));
  int bottom=int(std::clamp(std::ceil(std::max(y0,y1)+radius),0.f,float(canvas->height())));
  curveTag=0;
  if(right>left && bottom>top)layers->replaceActivePixels(canvas->pixels());
}
void loadActive() {
  if (layers && canvas) canvas->setPixels(layers->layer(layers->activeIndex()).pixels);
}
void resetHistory() {
  revision = ++nextRevision;
  sampledSource.clear();
  curveTag = 0;
  undoStack.clear();
  redoStack.clear();
  brushMask.clear();
  historyArchive.reset();
  velyntora::pages::cache().discardUnused();
  if(!historyDirectory.empty())try {
    historyArchive=std::make_shared<velyntora::history::HistoryArchive>(historyDirectory);
    // Import already runs on a worker. Archive its baseline there, before the first stroke.
    if(layers)historyArchive->capture(*layers);
  } catch(const std::exception&) {historyArchive.reset();}
}
void restore(const Snapshot& snapshot) {
  auto restored = snapshot.archive
      ? std::make_unique<velyntora::LayerDocument>(snapshot.archive->restore(snapshot.entry))
      : std::make_unique<velyntora::LayerDocument>(*snapshot.fallback);
  auto restoredCanvas = std::make_unique<velyntora::Canvas>(restored->width(), restored->height());
  restoredCanvas->setPixels(restored->layer(restored->activeIndex()).pixels);
  layers = std::move(restored);
  canvas = std::move(restoredCanvas);
  curveTag = snapshot.curveTag;
  revision = snapshot.revision;
}
}  // namespace
