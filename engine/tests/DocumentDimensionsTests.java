package art.velyntora.core;
public final class DocumentDimensionsTests {
 static void check(boolean ok){if(!ok)throw new AssertionError();}
 static void size(DocumentDimensions d,int w,int h){check(d.width==w&&d.height==h);}
 static void rejected(Runnable action){try{action.run();throw new AssertionError("invalid dimensions accepted");}catch(IllegalArgumentException expected){}}
 public static void main(String[] args){
  size(DocumentDimensions.fromWidth(1920,1080,960),960,540);size(DocumentDimensions.fromHeight(1920,1080,540),960,540);
  size(DocumentDimensions.fromWidth(3,2,5),5,3);size(DocumentDimensions.fromHeight(3,2,3),5,3);
  size(DocumentDimensions.fromPercent(1920,1080,50),960,540);size(DocumentDimensions.fromPercent(101,99,12.5),13,12);
  size(new DocumentDimensions(6000,500),6000,500);size(new DocumentDimensions(2000,2000),2000,2000);
  size(new DocumentDimensions(2048,2048),2048,2048);size(new DocumentDimensions(2560,1600),2560,1600);size(new DocumentDimensions(3840,2160),3840,2160);size(new DocumentDimensions(4000,4000),4000,4000);size(new DocumentDimensions(6000,6000),6000,6000);rejected(()->new DocumentDimensions(6001,6000));rejected(()->new DocumentDimensions(6001,1));rejected(()->new DocumentDimensions(0,1));
  rejected(()->DocumentDimensions.fromWidth(1,8192,Integer.MAX_VALUE));rejected(()->DocumentDimensions.fromHeight(8192,1,Integer.MAX_VALUE));
  rejected(()->DocumentDimensions.fromPercent(1,1,0));rejected(()->DocumentDimensions.fromPercent(1,1,Double.NaN));rejected(()->DocumentDimensions.fromPercent(1,1,Double.POSITIVE_INFINITY));rejected(()->DocumentDimensions.fromPercent(1,1,1));
  for(int w:new int[]{1,17,101,640,1920})for(int h:new int[]{1,19,99,480,1080})for(double percent:new double[]{50,100}){DocumentDimensions d=DocumentDimensions.fromPercent(w,h,percent);size(d,(int)Math.round(w*percent/100),(int)Math.round(h*percent/100));}
  System.out.println("Document dimensions: aspect ratios, rounding, percentages, budgets and overflow passed");
 }
}
