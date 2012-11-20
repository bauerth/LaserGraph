import processing.video.*;
import processing.opengl.*;
import blobDetection.*;
//import cl.eye.*;
import oscP5.*;
import netP5.*;
import blobDetection.*;


PImage img;
PImage initialBackground;
PImage sourceImageReduced;

boolean newFrame = false;

// Color Things  
float BrushSize = 20;
color BrushColor = color(255, 0, 0); 
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

void init() {
  
  frame.removeNotify();
  frame.setUndecorated(true); // works.

  // call PApplet.init() to take care of business
  super.init();
  
}

void setup() {

   setupCamera();

   setupCanvas();
    
   setupCalibration();
   
   setupBlobdetection();

   setupOSCP5();
   
   setupBoxes();
  
   InitializeColorChords();
    
}

// Sets up the Camera 
void setupCamera() {

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

void captureEvent(Capture cam){
  
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
void setupCanvas() {
  
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
void setupCalibration() {
  
  // load our settings
  getNSave= new SaveToXml(this); 
   
  calibration = new DoCalibration();
  calibration.setDefaultSrc(getNSave);
  calibCounter = 0;
  
  srcX = new float[4];
  srcY = new float[4];
  
}

void setupBlobdetection() {
  
  // Set up BlobDetection Class
  blobDetection = new BlobDetection(Config.cameraWidth, Config.cameraHeight);
    
  // Set to detects bright areas
  blobDetection.setPosDiscrimination(true);
    
  // Set thresh between 0 and 1 (detects blobs that are brighter than thresh)
  blobDetection.setThreshold(Config.blobThresh);
  
}

void setupOSCP5(){

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

void setupBoxes() {

  setBoxMode = false;
  boxList = new ArrayList();
  newbox = new float[8];  
  float[] box1 = new float[8];
  cursorImage = loadImage("media/crosshair2.png");
  
}


// Draw & Calibrate
void draw() {
  
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

void drawStrokesSimple(color brushColor, Warp9 wrp) {
  
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

void drawStrokes(float brushSize, color brushColor, Warp9 wrp) {
    
    stroke(0, 0, 0);
    float scaleFactor = 0.15;
    Blob b;
    EdgeVertex eA,eB;
    for (int n = 0 ; n < blobDetection.getBlobNb() ; n++) {
      b = blobDetection.getBlob(n);
      if (b != null) {
        int maxSteps = 40;
        for (int i = 1; i <= maxSteps; i++) {
          //brush wird zu den rändern hin transparenter
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


void mousePressed() {
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

