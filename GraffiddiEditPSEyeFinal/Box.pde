class Box
{
  float x1,y1,x2,y2,x3,y3,x4,y4;
  
  public Box()
  {
  }
  
  public void draw()
  {
    beginShape();                
        vertex(x1*Config.canvasWidth, y1 * Config.canvasHeight);
        vertex(x2 * Config.canvasWidth, y2 * Config.canvasHeight);
        vertex(x3 * Config.canvasWidth, y3 * Config.canvasHeight);
        vertex(x4 * Config.canvasWidth, y4 * Config.canvasHeight);
    endShape(CLOSE);
  }
  
  public void send()
  {
      OscMessage msg = new OscMessage("/box");
      msg.add(x1);
      msg.add(y1);
      msg.add(x2);
      msg.add(y2);
      msg.add(x3);
      msg.add(y3);
      msg.add(x4);
      msg.add(y4);
      
      oscP5.send(msg, myRemoteLocation); 
  }
  
  public String toString()
  {
    return "X1:"+x1+",Y1:"+y1+" X2:"+x2+", Y2:"+y2+" X3:"+x3+"Y3:"+y3+" X4:"+x4+", Y4:"+y4;
  }
}
