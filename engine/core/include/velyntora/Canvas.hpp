#pragma once
#include <cstdint>
#include <vector>
#include "../../document/tiles/StrokeTiles.hpp"
namespace velyntora {
namespace tiles {class LayerPixels;}
class Canvas {
  friend class TipBrushEngine;
 public:
  Canvas(int width, int height);
  Canvas(const Canvas& other);
  Canvas& operator=(const Canvas& other);
  int width() const noexcept { return width_; }
  int height() const noexcept { return height_; }
  const tiles::LayerPixels& pixels() const noexcept { return pixels_; }
  void clear(std::uint32_t argb);
  void beginOpacityStroke();
  void endOpacityStroke();
  void stroke(float x0, float y0, float x1, float y1, float radius, std::uint32_t argb);
  void strokeStyled(float x0, float y0, float x1, float y1, float radius, std::uint32_t argb,
                    float opacity, float hardness, bool square, bool eraser,
                    const std::vector<std::uint8_t>* mask = nullptr);
  void strokeAntialiased(float x0,float y0,float x1,float y1,float radius,std::uint32_t argb,
      float opacity,float hardness,bool square,const std::vector<std::uint8_t>* mask=nullptr);
  void sampledStroke(const tiles::LayerPixels& source, bool clone, int offsetX, int offsetY,
                     std::uint32_t target, std::uint32_t replacement, int tolerance, float x0,
                     float y0, float x1, float y1, float radius, float opacity, float hardness,
                     const std::vector<std::uint8_t>* mask = nullptr);
  void rectangle(int x0, int y0, int x1, int y1, std::uint32_t argb, bool filled);
  void ellipse(int x0, int y0, int x1, int y1, std::uint32_t argb, bool filled);
  void fill(int x, int y, std::uint32_t argb, const std::vector<std::uint8_t>* mask = nullptr);
  void setPixels(const std::vector<std::uint32_t>& pixels);
  void setPixels(std::initializer_list<std::uint32_t> pixels){setPixels(std::vector<std::uint32_t>(pixels));}
  void setPixels(const tiles::LayerPixels& pixels);

 private:
  void dab(float x, float y, float radius, std::uint32_t argb);
  void pixel(int x, int y, std::uint32_t argb);
  int width_, height_;
  tiles::LayerPixels pixels_;
  tiles::StrokeTiles strokeTiles_;
  tiles::StrokeTiles::Baseline strokeBase_{&strokeTiles_};
  tiles::StrokeTiles::Coverage strokeCoverage_{&strokeTiles_};
};
}  // namespace velyntora
