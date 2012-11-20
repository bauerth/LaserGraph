

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
