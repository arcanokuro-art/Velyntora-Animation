package art.velyntora.core;
final class Blend15 {static double[] apply(double[] a,double[] b){return BlendCompositor.setLum(BlendCompositor.setSat(b,BlendCompositor.sat(a)),BlendCompositor.lum(b));}}
