package art.velyntora.core;
public final class ImageDecodePolicyTests {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(ImageDecodePolicy.sampleSize(8000,500)==1);check(ImageDecodePolicy.sampleSize(2000,2000)==1);check(ImageDecodePolicy.sampleSize(2048,2048)==1);check(ImageDecodePolicy.sampleSize(8193,1)==2);
  check(ImageDecodePolicy.sampleSize(2560,1600)==1);check(ImageDecodePolicy.sampleSize(3840,2160)==1);check(ImageDecodePolicy.sampleSize(4000,4000)==1);
  for(int w:new int[]{1,17,2000,2048,8192,8193,100000,Integer.MAX_VALUE})for(int h:new int[]{1,33,2000,4000,100000,Integer.MAX_VALUE}){int sample=ImageDecodePolicy.sampleSize(w,h);long rw=((long)w+sample-1)/sample,rh=((long)h+sample-1)/sample;check(rw<=8192&&rh<=8192&&rw*rh<=16000000);if(sample>1){long pw=((long)w+sample/2-1)/(sample/2),ph=((long)h+sample/2-1)/(sample/2);check(pw>8192||ph>8192||pw*ph>16000000);}}
  try{ImageDecodePolicy.sampleSize(0,1);throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("Decode bounds: panoramas, 16M-pixel budget, rounding and overflow tests passed");
 }
}
