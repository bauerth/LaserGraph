import processing.core.*; 
import processing.xml.*; 

import processing.video.*; 
import processing.opengl.*; 
import blobDetection.*; 
import oscP5.*; 
import netP5.*; 
import blobDetection.*; 

import controlP5.*; 
import cl.eye.*; 
import fullscreen.*; 
import blobDetection.*; 
import japplemenubar.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class GraffiddiEditPSEyeFinal extends PApplet {




//import cl.eye.*;





PImage img;
PImage initialBackground;
PImage sourceImageReduced;

boolean newFrame = false;

// Color Things  
float BrushSize = 20;
int BrushColor = color(255, 0, 0); 
int redColor = 255;
int greenColor = 0;
int blueColor = 0;
ColorChord[] colorChords;
int currentChord;

// Calibration
DoCalibration calibration;
SaveToXml getNSave;
float[] srcX;
float[] srcY;
int calibCounter;
boolean to_calib = false;

// Blob Detection
BlobDetection blobDetection;
Blob lastBlob;

//MAC STUFF
boolean mac = true;
Capture video;

// WINDOWS STUFF
CLCamera clCamCapture;

//OSC P5
OscP5 oscP5;
NetAddress myRemoteLocation;

boolean setBoxMode;
int boxClickCounter = 0;
ArrayList boxList;
float[] newbox;
PImage cursorImage;

public void init() {
  
  frame.removeNotify();
  frame.setUndecorated(true); // works.

  // call PApplet.init() to take care of business
  super.init();
  
}

public void setup() {

   setupCamera();

   setupCanvas();
    
   setupCalibration();
   
   setupBlobdetection();

   setupOSCP5();
   
   setupBoxes();
  
   InitializeColorChords();
    
}

// Sets up the Camera 
public void setupCamera() {

  // initialize camera for mac
  if (mac) {    
    String[] cams = Capture.list();
    println(cams);
    video = new Capture(this, Config.cameraWidth, Config.cameraHeight, "Sony HD Eye for PS3 (SLEH 00201)", 60);
  }
  
  // initialize camera for windows
  else {   
    CLCamera.loadLibrary("C:/CLEyeMulticam.dll");
    // Checks available cameras
    int numCams = CLCamera.cameraCount();
    println("number of cl cams: " + numCams);
    clCamCapture = new CLCamera(this);
    // ----------------------(i, CLEYE_GRAYSCALE/COLOR, CLEYE_QVGA/VGA, Framerate)
    clCamCapture.createCamera(0, 0, 0, Config.cap_fps);
    // Starts camera captures
    clCamCapture.startCamera();
  }
}

public void captureEvent(Capture cam){
  
     newFrame = true;
     
     // Get Frame (Mac)
     if (mac) {
      if (cam.available() == true) {
        cam.read();
      }
      img = cam;
      //img.loadPixels();
    }
    
}

// Sets up the Canvas
public void setupCanvas() {
  
  // set application framerate
  frameRate(Config.cap_fps);
  
  // set application size
  size(Config.canvasWidth, Config.canvasHeight);
  
  // set bg Color
  background(Config.backcolor);
  
  // ? ? ? 
  img = createImage(Config.imgWidth, Config.imgHeight, RGB);
  
  // sets Location of the fullscreen application (Windows!!)
  //frame.setLocation(1000,1000);

}

// Sets up the Calibration
public void setupCalibration() {
  
  // load our settings
  getNSave= new SaveToXml(this); 
   
  calibration = new DoCalibration();
  calibration.setDefaultSrc(getNSave);
  calibCounter = 0;
  
  srcX = new float[4];
  srcY = new float[4];
  
}

public void setupBlobdetection() {
  
  // Set up BlobDetection Class
  blobDetection = new BlobDetection(Config.cameraWidth, Config.cameraHeight);
    
  // Set to detects bright areas
  blobDetection.setPosDiscrimination(true);
    
  // Set thresh between 0 and 1 (detects blobs that are brighter than thresh)
  blobDetection.setThreshold(Config.blobThresh);
  
}

public void setupOSCP5(){

  /* start oscP5, listening for incoming messages at port 12000 */
  oscP5 = new OscP5(this, 12000);
  
  /* myRemoteLocation is a NetAddress. a NetAddress takes 2 parameters,
   * an ip address and a port number. myRemoteLocation is used as parameter in
   * oscP5.send() when sending osc packets to another computer, device, 
   * application. usage see below. for testing purposes the listening port
   * and the port of the remote location address are the same, hence you will
   * send messages back to this sketch.
   */
  myRemoteLocation = new NetAddress(Config.OSCRemoteLocation, 12000);
  
}

public void setupBoxes() {

  setBoxMode = false;
  boxList = new ArrayList();
  newbox = new float[8];  
  float[] box1 = new float[8];
  cursorImage = loadImage("media/crosshair2.png");
  
}


// Draw & Calibrate
public void draw() {
  
  if (newFrame) {
    
    newFrame = false;
   
    frame.setLocation(Config.mainScreenResolutionX + Config.canvasPositionX, 0 + Config.canvasPositionY);
    
    // Get Frame (Windows)
    if (!mac) {
      // save cameraFrame in img
      clCamCapture.getCameraFrame(img.pixels, 0);   
      img.updatePixels();
    }
    
    /*
    sourceImageReduced = img;
        
    for (int x = 0; x < img.width; x++) {
      for (int y = 0; y < img.height; y++ ) {
        int loc = x + y*img.width;
        // Test the brightness against the threshold
        // If brightness in background image greater than..
        if (brightness(initialBackground.pixels[loc]) > 0.2) {
          // compute it out of current img
          sourceImageReduced.pixels[loc] = color(255);  // White
         }  
      }
    }*/
    
    // detect all the blobs
    blobDetection.computeBlobs(img.pixels); 
    //println("Detected [ " + theBlobDetection.getBlobNb() + " ] Blobs. ");
    
    if (setBoxMode) {
      
      cursor(cursorImage);
      background(Config.boxBackColor);
      for (int i=0; i < boxList.size(); i++) {
        
        float[] newbox = (float[]) boxList.get(i);
        
        fill(100, 100, 100); 
        beginShape();                
        vertex(newbox[0] * Config.canvasWidth, newbox[1] * Config.canvasHeight);
        vertex(newbox[2] * Config.canvasWidth, newbox[3] * Config.canvasHeight);
        vertex(newbox[4] * Config.canvasWidth, newbox[5] * Config.canvasHeight);
        vertex(newbox[6] * Config.canvasWidth, newbox[7] * Config.canvasHeight);
        endShape(CLOSE);
        
      }
      return;
    } else { cursor(ARROW); }
    
    // draw
    if (!to_calib) {
      
      for (int n = 0; n < blobDetection.getBlobNb() ; n++) {
    
        Blob b = blobDetection.getBlob(n);
    
          if (b != null) {
      
          // Input for Warping: Camera Blob Coordinates * Camera Width/Height
          float x = calibration.warping.computeWarpedX(b.x * Config.cameraWidth, b.y * Config.cameraHeight);
          float y = calibration.warping.computeWarpedY(b.x * Config.cameraWidth, b.y * Config.cameraHeight);
      
          // Compute warped Blob Coordinates out of Screen Resolution
          float xblob_warped = x / Config.canvasWidth;
          float yblob_warped = y / Config.canvasHeight;
          
          /* in the following different ways of creating osc messages are shown by example */
          //OscMessage myMessage = new OscMessage("/brush");
          //myMessage.add(xblob_warped);
          //myMessage.add(yblob_warped);
          /* send the message */
          //goscP5.send(myMessage, myRemoteLocation); 
        
        }
      }
      
      drawStrokes(BrushSize, BrushColor, calibration.warping);
      //drawStrokesSimple(BrushColor, calibration.warping);
    }
    
    // calibrate
    else {
  
      // Set Up Calibration View
      calibration.calibrateView();
      
      // Set last blob (most up to date one)
      for (int n = 0 ; n < blobDetection.getBlobNb() ; n++) {
        lastBlob = blobDetection.getBlob(n);
        if (blobDetection.getBlob(n) != null) {
          lastBlob = blobDetection.getBlob(n);
        }
      }
      
      // Catch current Coordinates to Calibration Array (For States 1-4)
      // We are using the Coordinates of the Camera
      if (lastBlob != null) {
        calibration.catchCalCoords(lastBlob.x * Config.cameraWidth, lastBlob.y * Config.cameraHeight, calibCounter);
      }
            
      // Show live Video from Camera during Calibration
      image(img, 
            Config.canvasPositionX + (Config.canvasWidth - Config.cameraWidth) / 2, 
            Config.canvasPositionY + (Config.canvasHeight - Config.cameraHeight) / 2,
            img.width, 
            img.height);
          
      // Finish Off Calibration
      if (calibCounter == 4) {
        
        // Set the Calibration data in DoCalibration
        calibration.setSrc(srcX, srcY);
        
        // Compute the Warp Matrix
        calibration.calibrateIt();
        
        // Adjust the to_calib field
        to_calib = calibration.getCalState();
        
        // Change the background Color to the one we have set
        background(Config.backcolor);
        
        // Save Calibration to XML
        getNSave.saveXml(srcX, srcY);
        
      }  
    }
  }
}

public void drawStrokesSimple(int brushColor, Warp9 wrp) {
  
  Blob b;
    
  for (int n = 0; n < blobDetection.getBlobNb() ; n++) {
    
    b = blobDetection.getBlob(n);
    
    if (b != null) {
      
      fill(brushColor); 
      
      // Input for Warping: Camera Blob Coordinates * Camera Width/Height
      // Output: X/Y for Drawing
      float x = wrp.computeWarpedX(b.x * Config.cameraWidth, b.y * Config.cameraHeight);
      float y = wrp.computeWarpedY(b.x * Config.cameraWidth, b.y * Config.cameraHeight);
      
      beginShape();                
      vertex(x-5,y-5);
      vertex(x+5,y-5);
      vertex(x+5,y+5);
      vertex(x-5,y+5);
      endShape(CLOSE);    
      
    }
  }
} 

public void drawStrokes(float brushSize, int brushColor, Warp9 wrp) {
    
    stroke(0, 0, 0);
    float scaleFactor = 0.15f;
    Blob b;
    EdgeVertex eA,eB;
    for (int n = 0 ; n < blobDetection.getBlobNb() ; n++) {
      b = blobDetection.getBlob(n);
      if (b != null) {
        int maxSteps = 40;
        for (int i = 1; i <= maxSteps; i++) {
          //brush wird zu den r\u00e4ndern hin transparenter
          float alphaValue = (255 * (float)(Math.pow(maxSteps - i, 2) / Math.pow(maxSteps, 2)));
          fill(brushColor, alphaValue); 
                
          //scaleFactor =  1 - (float)(Math.pow(maxSteps - i, 2)/ Math.pow((float)maxSteps, 2));
          scaleFactor =  1 - (float)(maxSteps - i)/ maxSteps;
              
          float x = wrp.computeWarpedX(b.x * Config.cameraWidth, b.y * Config.cameraHeight);
          float y = wrp.computeWarpedY(b.x * Config.cameraWidth, b.y * Config.cameraHeight);
                
          //System.out.println("xScreen "+ x +" yScreen "+y);
                
          beginShape();
  	  for (int m = 0; m < b.getEdgeNb(); m++) {
            eA = b.getEdgeVertexA(m);
            eB = b.getEdgeVertexB(m);
              if (eA != null && eB != null) {  
                float eAx = wrp.computeWarpedX(eA.x * Config.cameraWidth, eA.y * Config.cameraHeight);
                float eAy = wrp.computeWarpedY(eA.x * Config.cameraWidth, eA.y * Config.cameraHeight);
                float differenceX = eAx - x;
                float differenceY = eAy - y;
                float eAxNew = x + scaleFactor * (differenceX * (brushSize / 10));
                float eAyNew = y + scaleFactor * (differenceY * (brushSize / 10));
                vertex(eAxNew, eAyNew);
  	      }
            }
          endShape(CLOSE);    
        }
      }
    } 
}
    

//
// OLD STROKE / CONTROL STUFF
//
    
    
    public void keyPressed()
	{
            switch(key){
		case ESC: 
		{
			exit();
		}
		case ' ':
		{ 
                  if(to_calib) { 
                    calibCounter += 1;
                    calibCounter %= 5;
                  } 
                  else {
                    String filename = "savedImages/" + millis() + ".png";
                    save(filename);
                    println("saved file " + filename);
                    background(Config.backcolor);
                  }
                  break;
		}
                case 'c':
                {
                 to_calib = true; 
                 calibCounter = 0;
                 background(Config.backcolor);
                 break;
                }
                // set new box
                case 'v':
                {
                  if (setBoxMode) {
                    setBoxMode = false;
                    background(Config.backcolor);
                    println("setBoxMode OFF");
                  } else {
                    setBoxMode = true;
                    background(Config.boxBackColor);
                    println("setBoxMode ON");
                  }
                }
                break;
                // delete all boxes
                case 'x':
                {
                  boxList.clear();
                }
                case '1':
                {
                  BrushSize = 7;
                }
                break;
                case '2':
                {
                  BrushSize = 14;
                }
                break;
                case '3':
                {
                  BrushSize = 21;
                }
                break;
                case '4':
                {
                  BrushSize = 35;
                }
                break;
                case '5':
                {
                  BrushSize = 49;
                }
                break;
                case '6':
                {
                  BrushSize = 63;
                }
                break;
                case '7':
                {
                  BrushSize = 85;
                }
                break;
                case 'u':
                {
                  currentChord--;
                  if (currentChord < 0)
                    currentChord = colorChords.length - 1;
                    
                  BrushColor = colorChords[currentChord].GetColor(0);
                }
                break;
                case 'r':
                {
                    BrushColor = colorChords[currentChord].GetColor(0);
                }
                break;
                case 'g':
                {
                    BrushColor = colorChords[currentChord].GetColor(1);
                }
                break;
                case 'b':
                {
                    BrushColor = colorChords[currentChord].GetColor(2);
                }
            }
	}


public void mousePressed() {
  if (setBoxMode) {
    if (boxClickCounter == 0){
      newbox[0] = (float) mouseX / Config.canvasWidth;
      newbox[1] = (float) mouseY / Config.canvasHeight;
      boxClickCounter++;
      println("setBoxMode Punkt 1: " + mouseX + ", Punkt 2: " + mouseY);
    }
    else if (boxClickCounter == 1){
      newbox[2] = (float) mouseX / Config.canvasWidth;
      newbox[3] = (float) mouseY / Config.canvasHeight;
      boxClickCounter++;
      println("setBoxMode Punkt 2: " + mouseX + ", Punkt 2: " + mouseY);
    }
    else if (boxClickCounter == 2){
      newbox[4] = (float) mouseX / Config.canvasWidth;
      newbox[5] = (float) mouseY / Config.canvasHeight;
      boxClickCounter++;
      println("setBoxMode Punkt 3: " + mouseX + ", Punkt 2: " + mouseY);
    }
    else if (boxClickCounter == 3){
      newbox[6] = (float) mouseX / Config.canvasWidth;
      newbox[7] = (float) mouseY / Config.canvasHeight;
      boxList.add(newbox);
      boxClickCounter = 0;
      newbox = new float[8];
      
      /* in the following different ways of creating osc messages are shown by example */
      OscMessage myMessage = new OscMessage("/boxes");
      
      // Send all the boxes as float[] Arrays
      for (int i=0; i < boxList.size(); i++) {
        float[] box = (float[]) boxList.get(i);
        myMessage.add(box);
      }
      
      /* send the message */
      oscP5.send(myMessage, myRemoteLocation); 
      
      println("setBoxMode Punkt 4: " + mouseX + ", Punkt 2: " + mouseY);
    }
  }
}



public void drawInfo() {
  fill(0);
  rect(5,height -60,250,50);
  fill(0,255,0);
  text("current frame rate of draw loop: " + round(frameRate), 5, height - 30);
}

public void InitializeColorChords() {
        int chordAmount = 5;
        colorChords = new ColorChord[chordAmount];
        
        colorChords[0] = new ColorChord();
        colorChords[0].AddColor(color(178, 114, 21));
        colorChords[0].AddColor(color(145, 214, 188));
        colorChords[0].AddColor(color(117, 95, 49));
        colorChords[0].AddColor(color(118, 140, 106));
        colorChords[0].AddColor(color(255, 186, 75));
        
        colorChords[1] = new ColorChord();
        colorChords[1].AddColor(color(76, 27, 51));
        colorChords[1].AddColor(color(45, 105, 96));
        colorChords[1].AddColor(color(152, 169, 66));
        colorChords[1].AddColor(color(239, 230, 51));
        colorChords[1].AddColor(color(20, 29, 20));
        
        colorChords[2] = new ColorChord();
        colorChords[2].AddColor(color(161, 113, 108));
        colorChords[2].AddColor(color(67, 154, 171));
        colorChords[2].AddColor(color(114, 130, 150));
        colorChords[2].AddColor(color(255, 91, 0));
        colorChords[2].AddColor(color(0, 202, 189));
        
        colorChords[3] = new ColorChord();
        colorChords[3].AddColor(color(143, 63, 73));
        colorChords[3].AddColor(color(83, 122, 81));
        colorChords[3].AddColor(color(63, 48, 66));
        colorChords[3].AddColor(color(204, 195, 173));
        colorChords[3].AddColor(color(214, 108, 64));
        

        colorChords[4] = new ColorChord();
        colorChords[4].AddColor(color(170, 0, 0));
        colorChords[4].AddColor(color(0, 170, 0));
        colorChords[4].AddColor(color(0, 0, 170));
        
        currentChord = 0;
        
        BrushColor = colorChords[currentChord].GetNextColor();
      }

public class ColorChord
{
  private int _currentColor = 0;
  private int[] _colors = new int[0];
  
  public void AddColor(int newColor)
  {
    int[] placeHolder = _colors;
    
    _colors = new int[_colors.length + 1];
    
    for (int i = 0; i < placeHolder.length; i++)
    {
      _colors[i] = placeHolder[i];
    }
    
    _colors[placeHolder.length] = newColor;
  }
  
  public int GetNextColor()
  {
    _currentColor++;
    _currentColor %= _colors.length;
    return _colors[_currentColor];
  }
  
  public int GetColor(int colorIndex)
  {
    if (colorIndex < _colors.length)
      return _colors[colorIndex];
    else
      return color(0,0,0);
  }
  
}
public static class Config {
  
  public static final int mainScreenResolutionX = 1280;
  public static final int canvasPositionX = 0;
  public static final int canvasPositionY = 0;
  
  //Projector sizes
  public static final int canvasWidth = 1024;//960;//1280;//1024;
  public static final int canvasHeight = 768;//930;//1024;//768;
  public static final float MaxWidth = 3;
  public static final float MinWidth = 1;
  public static final float SpeedChange = (float) 2.5f;
  public static final float SpeedInfluence = (float) 0.1f;
  public static final int Fontcolor = 150;

  //backgroundcolor
  public static final int backcolor = 0;
  public static final int boxBackColor = 255;
  
  
  public static final int availLaser = 10;
  
  public static int cameraWidth = 640;
  public static int cameraHeight = 480;
  public static int usedID = 0;
  public static int cap_fps = 100;
  
  public static int imgWidth = 640;
  public static int imgHeight = 480;
  
  public static float blobThresh = 0.25f;
  
  public static String OSCRemoteLocation = "192.168.1.144";
  
}
class DoCalibration{
  
  //warps coordinates from cam to screen, needs calibration:
  public Warp9 warping;
  public float[] wpmat;
  private float[] destCoordX;
  private float[] destCoordY;
  int counter = 1;
  private float[] srcCoordX;
  private float[] srcCoordY;
  private boolean _calFinished;
  private boolean _calibrationSet;
  
  public void setIsSet(boolean s){
    _calibrationSet = s;
  }
  
  public void setSrc(float[] srcX, float[] srcY) {
    srcCoordX = srcX;
    srcCoordY = srcY;
    _calibrationSet = true;
    _calFinished = true; 
    for (int i = 0; i < srcX.length; i++)
      println("srcX" + i + ": "+ srcX[i]);
  }
  
  public float[] getSrcX() {
    return srcCoordX;
  }
  
  public float[] getSrcY() {
    return srcCoordY;
  }
  
  public boolean getCalState() {
    if(_calFinished) {
    return false;
    } else {
      return true;
    }
  }

  
 // Constructor initializes Calibration parameter
  public DoCalibration() {
    
   // Set up Destination Coordinates
   destCoordX = new float[4];
   destCoordY = new float[4];
   destCoordX[0] = 10;  
   destCoordY[0] = 10;
   destCoordX[1] = Config.canvasWidth - 10;  
   destCoordY[1] = 10;
   destCoordX[2] = Config.canvasWidth - 10;      
   destCoordY[2] = Config.canvasHeight - 10;
   destCoordX[3] = 10;                            
   destCoordY[3] = Config.canvasHeight - 10;
   
   // Set up Array for Source Coordinates
   srcCoordX = new float[4];
   srcCoordY = new float[4];
   
   warping = new Warp9();
   wpmat = new float[16];
   calibrateIt();
   
   _calFinished = false;
   _calibrationSet = false;
  }
  
  // Set up Calibration View
  public void calibrateView() {
    
      int calfieldSize = 20;
      fill(255);
      strokeWeight(2);
      stroke(255, 25, 0);
      
      // Set up Calibration Dots in the Corners
      arc(calfieldSize, calfieldSize, calfieldSize, calfieldSize, 0, 2*PI);
      arc(Config.canvasWidth-calfieldSize, calfieldSize, calfieldSize, calfieldSize, 0, 2*PI);
      arc(Config.canvasWidth-calfieldSize, Config.canvasHeight-calfieldSize, calfieldSize, calfieldSize, 0, 2*PI);
      arc(calfieldSize, Config.canvasHeight-calfieldSize, calfieldSize, calfieldSize, 0, 2*PI);
      
  }
  
  public void calibrateIt() {
      
    if(_calibrationSet) {
        warping.setSource(srcCoordX[0], srcCoordY[0], srcCoordX[1], srcCoordY[1], srcCoordX[2], srcCoordY[2], srcCoordX[3], srcCoordY[3]);
        warping.setDestination(destCoordX[0], destCoordY[0], destCoordX[1], destCoordY[1], destCoordX[2], destCoordY[2], destCoordX[3], destCoordY[3]);
        warping.computeWarpMatrix();                        
      }
   
  }
  
  public void catchCalCoords(float cX, float cY, int counter) {
  
    switch(counter){
      case 0: {  srcX[0] = cX; srcY[0] = cY; break; }
      case 1: {  srcX[1] = cX; srcY[1] = cY; println("state 1"); println(srcX[0] + "   " + srcY[0]); break; }
      case 2: {  srcX[2] = cX; srcY[2] = cY; println("state 2"); println(srcX[1] + "   " + srcY[1]); break; }
      case 3: {  srcX[3] = cX; srcY[3] = cY; println("state 3"); println(srcX[2] + "   " + srcY[2]); break; }
      case 4: {   println("state 4"); 
                  println(srcX[0] + "   " + srcY[0]);
                  println(srcX[1] + "   " + srcY[1]);
                  println(srcX[2] + "   " + srcY[2]);
                  println(srcX[3] + "   " + srcY[3]);
                  break; }
      }
  }
  
  
  public void setDefaultSrc(SaveToXml saveit) {
    
    float[] sX = new float[4];
    float[] sY = new float[4];
    
    //extract String values for corner coordinates from settings.xml
    sX = saveit.getSaved_X();
    sY = saveit.getSaved_Y();
    setSrc(sX,sY);
   
    calibrateIt();
    
 }
 
}
public class SaveToXml{
  
  private String _filename = "settings/settings.xml";
  private float[] _savedX;
  private float[] _savedY;
  private int _num;
  private XMLElement _xml;
  private XMLElement[] _xmlchildren;
  private float[] zeros;

  
  public SaveToXml(PApplet xinout){
    _xml = new XMLElement(xinout,_filename);
    _num = _xml.getChildCount();
    _xmlchildren = _xml.getChildren();
    initZeros();
    _savedX=new float[4];
    _savedY=new float[4];
    for(int i = 1; i<5;i++){
      if(_num!=0){
      /*println("xml Constructor starts...............");
     println("entry["+i+"]= " + _xmlchildren[i].getName());
     println("entry["+i+"]= " + _xmlchildren[i].getContent());
     println("entry["+i+"]= " + _xmlchildren[i+4].getName());
     println("entry["+i+"]= " + _xmlchildren[i+4].getContent());*/
     _savedX[i-1] = Float.valueOf(_xmlchildren[i-1].getContent()).floatValue();
     _savedY[i-1] = Float.valueOf(_xmlchildren[i-1+4].getContent()).floatValue();
     println("saveX["+i+"] = " + _savedX[i-1]);
      }
    }
  }
  
  public float[] getSaved_X(){
    if(_savedX!=null){
    return _savedX;
    } else return zeros;
  }
  
  public float[] getSaved_Y(){
    if(_savedX!=null){
    return _savedY;}
    else return zeros;
  }
  
  public int getNumOfElements(){
    return _num;
  }
  
  public void saveXml(float[] srcx, float[] srcy){
    PrintWriter settingsXml;
    settingsXml = createWriter(_filename);
    settingsXml.println("<?xml version=\"1.0\"?>");
    
    //convert float[] to String and copy to xmlchildrens
    String aux_x,aux_y;
    println("...save to settings/settings.xml");
    for(int i = 1; i<5;i++){
      aux_x = Float.toString(srcx[i-1]);
      aux_y = Float.toString(srcy[i-1]);
     _xmlchildren[i-1].setContent(aux_x);
     _xmlchildren[i-1+4].setContent(aux_y);
     println("srcx["+ i +"] = " + aux_x);
     println("srcy["+ i +"] = " + aux_y);
    }
    
    //write to file:
    try
    {
      XMLWriter writeXML = new XMLWriter(settingsXml) ;
      //writeXML.write("<?xml version="1.0"?>");
      writeXML.write(_xml);
      settingsXml.flush();
      settingsXml.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    
    
  }
  
  private void initZeros(){
  zeros = new float[4];
  for(int i=0;i<4;i++){
    zeros[i]=0;
  }
 }
}



class Warp9 {
  
  private float[] _src_X = new float[4];
  private float[] _src_Y = new float[4];
  private float[] _dst_X = new float[4];
  private float[] _dst_Y = new float[4];
  private float[] _srcMatrix = new float[16];
  private float[] _dstMatrix = new float[16];
  private float[] _warpMatrix = new float[16];
  private float _warpedX;
  private float _warpedY;
  
  // getter methods
  /*************************************************************************************/
  public float[] getSourceX(){
    return _src_X;
  }
   
  public float[] getSourceY(){
    return _src_Y;
  } 
  
  public float[] getDestinationX(){
    return _dst_X;
  }
  
  public float[] getDestinationY(){
    return _dst_Y;
  }
  
  public float[] getSourceMatrix(){
    return _srcMatrix;
  }
  
  public float[] getDestinationMatrix(){
    return _dstMatrix;
  }
  
  public float[] getWarpMatrix(){
    return _warpMatrix;
  }
  
  public float getWarpedX(){
    return _warpedX;
  }
  
  public float getWarpedY(){
    return _warpedY;
  }
  
  // setter methods
  /***************************************************************************************/
  public void setSource(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3)
  {
    _src_X[0] = x0;
    _src_Y[0] = y0;
    _src_X[1] = x1;
    _src_Y[1] = y1;
    _src_X[2] = x2;
    _src_Y[2] = y2;
    _src_X[3] = x3;
    _src_Y[3] = y3;
  }
  
  public void setDestination(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3)
  {
    _dst_X[0] = x0;
    _dst_Y[0] = y0;
    _dst_X[1] = x1;
    _dst_Y[1] = y1;
    _dst_X[2] = x2;
    _dst_Y[2] = y2;
    _dst_X[3] = x3;
    _dst_Y[3] = y3;
  }
  
  //Constructor
  /********************************************************************************/
  public Warp9(){
    setAllVecsIdentity();
    _warpedX = 0;
    _warpedY = 0;
  }
  
  //Now the serious computation stuff
  /********************************************************************************/
  public void computeWarpMatrix(){
    transformSquareQuad(_dst_X[0],_dst_Y[0],_dst_X[1],_dst_Y[1],_dst_X[2],_dst_Y[2],_dst_X[3],_dst_Y[3],_dstMatrix);
    transformQuadSquare(_src_X[0],_src_Y[0],_src_X[1],_src_Y[1],_src_X[2],_src_Y[2],_src_X[3],_src_Y[3],_srcMatrix);
    matrixMulti(_srcMatrix,_dstMatrix,_warpMatrix);
  }

  
  //calculates transformation matrix from a Square to a normalized Quad
  public void transformSquareQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float[] matrix){
    float dx1 = x1 - x2;
    float dx2 = x3 - x2;
    float dy1 = y1 - y2;
    float dy2 = y3 - y2;
    float sx = x0 - x1 + x2 - x3;
    float sy = y0 - y1 + y2 - y3;
    float g = (sx*dy2 - dx2*sy)/(dx1*dy2 - dx2*dy1);
    float h = (dx1*sy - sx*dy1)/(dx1*dy2 - dx2*dy1);
    
    //form first half of homography matrix
    matrix[0] = x1 - x0 + g*x1; matrix[1] = y1 - y0 + g*y1; matrix[2] = 0;  matrix[3] = g;
    matrix[4] = x3 - x0 + h*x3; matrix[5] = y3 - y0 + h*y3; matrix[6] = 0;  matrix[7] = h;
    matrix[8] = 0;              matrix[9] = 0;              matrix[10] = 1; matrix[11] = 0;
    matrix[12] = x0;            matrix[13] = y0;            matrix[14] = 0; matrix[15] = 1;
  }
  
  public void transformQuadSquare(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float[] matrix){
    
    transformSquareQuad(x0,y0,x1,y1,x2,y2,x3,y3,matrix);
    
    //now calculate the inverse of: matrix
    float a1 = matrix[5] - matrix[13]*matrix[7];
    float b1 = matrix[12]*matrix[7] - matrix[4];
    float c1 = matrix[4]*matrix[13] - matrix[12]*matrix[5];
    float d1 = matrix[13]*matrix[3] - matrix[1];
    float e1 = matrix[0] - matrix[12]*matrix[3];
    float f1 = matrix[12]*matrix[1] - matrix[0]*matrix[13];
    float g1 = matrix[1]*matrix[7] - matrix[5]*matrix[3];
    float h1 = matrix[4]*matrix[3] - matrix[0]*matrix[7];
    float i1 = matrix[0]*matrix[5] - matrix[4]*matrix[1];
    
    float det = 1/(matrix[0]*a1 + matrix[4]*d1 + matrix[12]*g1);
    
    //form second half of homography matrix
    matrix[0] = a1*det;  matrix[1] = d1*det; matrix[2] = 0;  matrix[3] = g1*det;
    matrix[4] = b1*det;  matrix[5] = e1*det; matrix[6] = 0;  matrix[7] = h1*det;
    matrix[8] = 0;       matrix[9] = 0;              matrix[10] = 1; matrix[11] = 0;
    matrix[12] = c1*det; matrix[13] = f1*det;        matrix[14] = 0; matrix[15] = i1*det;
  }
  
  //this methode should be called for transforming a certain coordinate from the camera to screen
  public void warpCoordinates(float src_X, float src_Y){
    computeWarped(_warpMatrix, src_X, src_Y);
  }
  
  public void computeWarped(float[] matrix, float src_X, float src_Y){
   float[] res = new float[4];
   res[0] = src_X*matrix[0] + src_Y*matrix[4] + matrix[12];
   res[1] = src_X*matrix[1] + src_Y*matrix[5] + matrix[13];
   res[3] = src_X*matrix[3] + src_Y*matrix[7] + matrix[15];
   _warpedX = res[0]/res[3];
   _warpedY = res[1]/res[3];
  } 
  
  
  public float computeWarpedX(float src_X, float src_Y){
   float[] res = new float[4];
   res[0] = src_X*_warpMatrix[0] + src_Y*_warpMatrix[4] + _warpMatrix[12];
   res[3] = src_X*_warpMatrix[3] + src_Y*_warpMatrix[7] + _warpMatrix[15];
   return res[0]/res[3];
  } 
  
  
  public float computeWarpedY(float src_X, float src_Y){
   float[] res = new float[4];
   res[1] = src_X*_warpMatrix[1] + src_Y*_warpMatrix[5] + _warpMatrix[13];
   res[3] = src_X*_warpMatrix[3] + src_Y*_warpMatrix[7] + _warpMatrix[15];
   return res[1]/res[3];
  } 
  
    
  //Matrix multiplication resultMatrix = srcMatrix*dstMatrix
  public void matrixMulti(float[] srcMatrix, float[] dstMatrix, float[] resultMatrix)
  {
    for(int row = 0; row<4; row++)
    {
      int rowIndex = row*4;
      for(int col = 0; col<4; col++)
      {
        resultMatrix[rowIndex + col] = (srcMatrix[rowIndex]*dstMatrix[col] + srcMatrix[rowIndex + 1]*dstMatrix[col+4] 
                                           + srcMatrix[rowIndex + 2]*dstMatrix[col+8] + srcMatrix[rowIndex+3]*dstMatrix[col+12]);
        
      }
    }
  }
  
  
  public void setAllVecsIdentity(){
    setSource(0,0,1,0,1,1,0,1);
    setDestination(0,0,1,0,1,1,0,1);
    computeWarpMatrix();
  }
  
  
}
  static public void main(String args[]) {
    PApplet.main(new String[] { "--present", "--bgcolor=#666666", "--stop-color=#cccccc", "GraffiddiEditPSEyeFinal" });
  }
}
