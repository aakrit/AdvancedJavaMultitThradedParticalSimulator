package ball_game_readonlyLists;


import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.Color;
   
public class Ball implements Cloneable{
    private int size;
    private double x,y;
    private double u,v;
    public void setU(double u) {
        this.u = u;
    }
  
    public void setV(double v) {
        this.v = v;
    }
  
    public double getRadius() {
        return radius;
    }
  
    public static int getSMALL() {
        return SMALL;
    }
  
    public static int getMEDIUM() {
        return MEDIUM;
    }
  
    public static int getLARGE() {
        return LARGE;
    }
    
    private Color color;
    private double diameter;
    private double radius;
    public Object clone() throws CloneNotSupportedException
    {
    	
    	return new Ball(this.size, this.x,this.y,this.u,this.v,this.color);
    }
    private ArrayList<Ball> collisionList;
    
    public static int SMALL=0,MEDIUM=1,LARGE=2;
    Ball(){
    	
    }
    Ball(int size, double xpos, double ypos,
     double xvel, double yvel, Color color){
    this.size = size;
    this.x = xpos;
    this.y = ypos;
    this.u = xvel;
    this.v = yvel;
    this.color = color;
   
    if (size == SMALL)
        diameter = 10.0;
    else if (size == MEDIUM)
        diameter = 15.0;
    else if (size == LARGE)
        diameter = 20.0;
       
    collisionList = new ArrayList<Ball>(3);
    
    radius = diameter/2;
    }
   
    public ArrayList<Ball> getCollisionList(){
    return this.collisionList;
    }
    //only called once when game is started and its only checked by one thread so no sync needed
    public void checkInitialSpawnCollision(List<Ball> ballList)
    {
        for (Ball b:ballList)
        {
            if (this == b) continue;//ignore if its 'this' ball
            float bothBallRad = (float) (this.radius + b.radius);
            float delX = (float) Math.abs(this.x - b.x);
            float delY = (float) Math.abs(this.y - b.y);
            float deltaRadius = (float) Math.sqrt(
                    (Math.pow(delX, 2)+(Math.pow(delY, 2))));
            if (deltaRadius <= bothBallRad)
            {
                this.x += 20;
                this.y += 20;
                b.x -= 20;
                b.y -= 20;   
            }
        }
    }
    public void updatePosition(Rectangle2D bounds, List<Ball> threadBallList, List<Ball> ballRemList){
    collisionList.clear();//clear the list
    x += u;
    y += v;
    //check if ball hit escape window
    if(((y + diameter) >= bounds.getMaxY()-40) 
    		&& ((x + diameter) >= (4*(int)bounds.getMaxX()/10)) 
    		&& ((x - diameter) <= (5*(int)bounds.getMaxX()/10)))
		{
    		ballRemList.add(this);
		}
    //check wall boundary
    if (x - (diameter) <= bounds.getMinX())
        { 
        x = bounds.getMinX() + (diameter);
        u = -u;
        }
    if (x + (diameter) >= bounds.getMaxX())
        {
        x = bounds.getMaxX() - (diameter); 
        u = -u;
        }
    if (y - (diameter) <= bounds.getMinY())
        {
        y = bounds.getMinY() + (diameter); 
        v = -v;
        }
    if (y + (diameter) >= bounds.getMaxY()-40)
        {
        y = bounds.getMaxY() - (40+diameter); 
        v = -v;
        }
    }
    public void checkRegularCollision(List<Ball> ballList, List<Ball> threadList, List<Ball> ballRemList, List<Ball> ballAddList)
    {
    	try{
        for (Ball b:ballList)//for the entire list of balls
        {
        	//check for collision
            if (this == b) continue;//ignore if its the same ball
            float bothBallRad = (float) (this.radius + b.radius);
            float delX = (float) Math.abs(this.x - b.x);
            float delY = (float) Math.abs(this.y - b.y);
            float deltaRadius = (float) Math.sqrt(
                    (Math.pow(delX, 2)+(Math.pow(delY, 2))));
            if (deltaRadius < bothBallRad)//if collision detected
            {
                collisionList.add(b);
                //before spawning new balls, i was originally checking to see
                //if there are any balls near the collision so the spawned ball
                //isn't created on top of another but this required checking all the 
                //NSEW directions and was becoming very memory intensive and making the
                //program run really show
                if(this.color == b.color)//if both balls are same color
                    sameTypeBallsCollision(b);
                else if(this.size == b.size && this.color != b.color){//if same size diff color
                    if(b.size == 0)//both balls small
                        sameSizeSmallBallsCollision(b, ballAddList, threadList);//create 1 new small red ball
                    else if(b.size == 1)//both balls med
                        sameSizeMedBallsCollision(b, ballAddList, threadList);//create 2 new small red balls
                    else//both balls large
                        sameSizeLargeBallsCollision(b, ballAddList, threadList);//create 2 new small red balls and one 1 ball
                	}
                else//bigger ball kills the smaller one
                {
                    diffSizeAndColorBallsCollision(b, ballRemList);                   
                }
            }
        }
    	}catch(ConcurrentModificationException e){e.printStackTrace();
    	}
    }
    private void sameTypeBallsCollision(Ball b)
    {
        updateBallVelocities(b);
        
    }
    private void sameSizeSmallBallsCollision(Ball b, List<Ball> ballAddList, List<Ball> threadList)
    {
    	//create 1 new small red ball
    	updateBallVelocities(b);
		createRedBall(ballAddList, threadList);
    }
    private void sameSizeMedBallsCollision(Ball b, List<Ball> ballAddList, List<Ball> threadList)
    {
    	//create 2 new small red balls
    	updateBallVelocities(b); 
		createRedBall(ballAddList, threadList);
		createRedBall(ballAddList, threadList);
    }
    private void sameSizeLargeBallsCollision(Ball b, List<Ball> ballAddList, List<Ball> threadList)
    {
    	//create 2 small red balls and one blue ball
    	updateBallVelocities(b);    	
		createRedBall(ballAddList, threadList);
		createRedBall(ballAddList, threadList);
		createBlueBall(ballAddList, threadList);
    }
    private void diffSizeAndColorBallsCollision(Ball b, List<Ball> ballRemList)
    {
    	//eliminate smaller ball
    	if(this.size < b.size)
    	{
    		ballRemList.add(this);
    	}
    }
    private void createRedBall(List<Ball> ballAddList, List<Ball> threadList)
    {
    	if(threadList.size() < 20)//keeping count low to avoid cluttering the window
    	{
    		Random rn = new Random();
        	double ranX = ((this.x-100) + (rn.nextInt(100)));
        	double ranY = ((this.y-100) + (rn.nextInt(100)));
        	double ranU = (0.5 + (rn.nextDouble()));
            double ranV = (0.5 + (rn.nextDouble()));
        	Ball newBall = new Ball(SMALL,ranX,ranY,ranU, ranV,Color.RED);
        	ballAddList.add(newBall);
    	}
    }
    private void createBlueBall(List<Ball> ballAddList, List<Ball> threadList)
    {
    	if(threadList.size() < 20)//keeping count low to avoid cluttering the window
    	{
    		Random rn = new Random();
        	double ranX = ((this.x-200) + (rn.nextInt(200)));
        	double ranY = ((this.y-200) + (rn.nextInt(200)));
        	double ranU = (0.5 + (rn.nextDouble()));
            double ranV = (0.5 + (rn.nextDouble()));
        	Ball newBall = new Ball(SMALL,ranX,ranY,ranU,ranV,Color.BLUE);
        	ballAddList.add(newBall);
    	}
    }
    //need to use Physics to calculate momentum + energy transfer between balls
    private void updateBallVelocities(Ball b)
    {
    	//convert from cartesian to polar coordinates
    	double thetaVelb1 = Math.atan2(this.v, this.u);
    	double thetaVelb2 = Math.atan2(b.v, b.u);
    	//find theta to shift plane of axis to get rotated x' y' coordinates
    	double atan2val = Math.atan2(((this.y+this.diameter/2.0) - (b.y+b.diameter/2.0)), 
    			((this.x+this.radius)-(b.x+b.radius)));
    	//find each balls x and y velocities for both balls in rotated coordinate
    	double b1Vel = calcVelocity(this.u, this.v);
    	double b2Vel = calcVelocity(b.u, b.v);
    	
    	double b1VelX = b1Vel * Math.cos(thetaVelb1 - atan2val);
    	double b1VelY = b1Vel * Math.sin(thetaVelb1 - atan2val);
    	double b2VelX = b2Vel * Math.cos(thetaVelb2 - atan2val);
    	double b2VelY = b2Vel * Math.sin(thetaVelb2 - atan2val);
    	//find the final x velocities for both balls in 1D
    	double m1 = this.radius * this.radius;
    	double m2 = b.radius * b.radius;
    	double b1FinalVelX = (b1VelX*((double)(m1-m2)) + 
    			((2*m2*b2VelX)))/(m1 + m2);
    	double b2FinalVelX = (b2VelX*((double)(m2 -m1)) + 
    			((2*m1*b1VelX)))/(m1 + m2);
    	//find the magnituge velocites for both balls in cartecian coordinates
    	double b1FinalVel = calcVelocity(b1FinalVelX, b1VelY);
    	double b2FinalVel = calcVelocity(b2FinalVelX, b2VelY);
        //find the theta
    	double b1FinalTheta = Math.atan2(b1VelY, b1FinalVelX) + atan2val;
    	double b2FinalTheta = Math.atan2(b2VelY, b2FinalVelX) + atan2val;
    	//convert back from polar to cartesian coordinates and set the new vel for both balls
    	double b1xVel = Math.cos(b1FinalTheta)*b1FinalVel;
    	double b1yVel = Math.sin(b1FinalTheta)*b1FinalVel;
//    	double b2xVel = Math.cos(b2FinalTheta)*b2FinalVel;
//    	double b2yVel = Math.sin(b2FinalTheta)*b2FinalVel;
    	
    	this.u = b1xVel; this.v = b1yVel;// b.u = b2xVel; b.v = b2yVel;
    }
    private double calcVelocity(double u, double v)
    {
    	return Math.sqrt((Math.pow(u, 2)+Math.pow(v, 2)));
    }
    public Color getColor(){
    return(this.color);
    }
       
    public double getX(){
    return(this.x);
    }
   
    public double getY(){
    return(this.y);
    }
   
    public double getU(){
    return(this.u);
    }
   
    public double getV(){
    return(this.v);
    }
   
    public double getSize(){
    return(this.size);
    }
   
    public double getDiameter(){
    return(this.diameter);
    }
   
    public Ellipse2D getShape(){
        Ellipse2D circle = new Ellipse2D.Double();
        circle.setFrameFromCenter(x, y, x+radius, y+radius);
        return circle;//new Ellipse2D.Double(x-radius,y-radius,diameter,diameter);
    }
}
