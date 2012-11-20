public class ColorChord
{
  private int _currentColor = 0;
  private color[] _colors = new color[0];
  
  public void AddColor(color newColor)
  {
    color[] placeHolder = _colors;
    
    _colors = new color[_colors.length + 1];
    
    for (int i = 0; i < placeHolder.length; i++)
    {
      _colors[i] = placeHolder[i];
    }
    
    _colors[placeHolder.length] = newColor;
  }
  
  public color GetNextColor()
  {
    _currentColor++;
    _currentColor %= _colors.length;
    return _colors[_currentColor];
  }
  
  public color GetColor(int colorIndex)
  {
    if (colorIndex < _colors.length)
      return _colors[colorIndex];
    else
      return color(0,0,0);
  }
  
}
