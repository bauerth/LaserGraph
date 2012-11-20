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

