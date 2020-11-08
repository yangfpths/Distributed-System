import java.io.Serializable;

@SuppressWarnings("serial")
public class DPoint implements Serializable
{
	private double X;
	private double Y;
	private String color; //use the string representation
	private double size;
		
	public DPoint(double x, double y, String c, double s)
	{
		this.X = x;
		this.Y = y;
		this.color = c;
		this.size = s;
	}
	
	public double getX()
	{
		return this.X;
	}
	
	public double getY()
	{
		return this.Y;
	}
	
	public double getSize()
	{
		return this.size;
	}
	
	public String getColor()
	{
		return this.color;
	}
	
}