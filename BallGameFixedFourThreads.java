package ball_game_readonlyLists;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Random;

public class BallGameFixedFourThreads
{
    public static void main(String[] args){
        Toolkit window = Toolkit.getDefaultToolkit();
        Dimension screen = window.getScreenSize();
        double yFrameSize = (screen.height)/(3);
        double xFrameSize = (screen.width)/(3);
          
        int nBalls=20;
        int[] sizeArray  = {Ball.SMALL, Ball.MEDIUM, Ball.LARGE};
        Color[] colorArray = {Color.RED, Color.BLUE};
        final List<Ball> ballList = new ArrayList<Ball>(nBalls);
          
    //create all the random ball instances
        Random rn = new Random();
        for (int i=1;i <= nBalls; i++){
            int ranSize = sizeArray[rn.nextInt(3)];
            double ranX = (100 + (rn.nextDouble()*(xFrameSize)));
            double ranY = (100 + (rn.nextDouble()*(yFrameSize)));
            double ranU = (0.5 + (rn.nextDouble()));
            double ranV = (0.5 + (rn.nextDouble()));
            System.out.println(ranX+" "+ranY+" "+ranU+" "+ranV);
            Color ranColor = colorArray[rn.nextInt(2)];
            Ball b = new Ball(ranSize,ranX,ranY,ranU,ranV,ranColor);
            ballList.add(b);
        }
   
    //start the gui in a thread
        EventQueue.invokeLater(new Runnable(){
            public void run(){
                MyFrame gui = new MyFrame(ballList);
                gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                gui.setTitle("Swing Ball GAME!");
                gui.setVisible(true);
            }
        });
    }
}
   
class MyFrame extends JFrame
{
    private int iteration = 0;
    private JButton startBut = new JButton("START");
    private JButton speedUpBut  = new JButton("SPEED UP");
    private JButton slowDownBut  = new JButton("SLOW DOWN");
    private JButton pauseBut = new JButton("PAUSE");
    private JButton resumeBut = new JButton("RESUME");
    private JButton addBallBut = new JButton("ADD BALL");
    private JButton remBallBut = new JButton("REM BALL");
    private JLabel controls = new JLabel("Controls: "); 
    private JLabel threadLabel;

    private JLabel iterLabel = new JLabel("Iterations: 0"  + "       Collisions: 0" + "      Blue Balls: 0"
        		   +"      RedBalls: 0" +"      SmallBalls: 0" + "     MedBalls: 0"+
        			"      LargeBalls: 0");
    private JPanel ballPanel;
    private JPanel butPanel, butPanel2;
    private int panelHeight, panelWidth;
    private final List<Ball> ballList;
    private boolean startGame = false;
    private Animation animation;
    private Thread gameThread;
    
    MyFrame(final List<Ball> ballList)
    {
        this.ballList = ballList;
        //setting the window location
        Toolkit window = Toolkit.getDefaultToolkit();
        Dimension screen = window.getScreenSize();
        panelHeight = screen.height;
        panelWidth = screen.width;
        setSize(panelWidth/2, panelHeight/2);
        setLocationByPlatform(true);  
        int processors = Runtime.getRuntime().availableProcessors();//get # of threads on machine
        threadLabel = new JLabel("This Machine has " + processors + " threads");
        butPanel = new JPanel();
        butPanel2 = new JPanel();
        
        ballPanel = new BallPanel(ballList, butPanel);
        
        butPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        butPanel.setBackground(SystemColor.window);
        this.add(butPanel,BorderLayout.NORTH);
        this.add(butPanel2,BorderLayout.SOUTH);
        this.add(ballPanel,BorderLayout.CENTER);
        butPanel.add(controls);
        butPanel.add(startBut);
        butPanel.add(pauseBut);
        butPanel.add(resumeBut);
        butPanel.add(addBallBut);
        butPanel.add(remBallBut);
        butPanel.add(speedUpBut);
        butPanel.add(slowDownBut);
        butPanel.add(threadLabel);
        butPanel2.add(iterLabel);
        setupActionListernsForButtons();
    }
    private void setupActionListernsForButtons()
    {
        startBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                if(!startGame)
                {
	                animation = new Animation(ballPanel,ballList,MyFrame.this,butPanel,iterLabel);
	                gameThread = new Thread(animation);
	                gameThread.start();
                }
                startGame = true;
            }
        });
        speedUpBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                animation.speedBalls = true;
            }
        });
        slowDownBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                animation.slowBalls = true;
            }
        });
        pauseBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
            	animation.isPaused = true;
            }           
        });
        resumeBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
            	try{
            		animation.resume();
            	}
            	catch(Exception e){
            		e.printStackTrace();
            	}
            }
        });
        addBallBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                animation.addBall = true;
            }
        });
        remBallBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
            	animation.remBall = true;
            }
        });
    }
}
  
class Animation implements Runnable{
    
//    List<Ball> ballList;
    List<Ball> readOnlyBallList;
    List<Ball> ballPanelAddList;
    List<Ball> ballPanelRemList;
    private int iter = 0;
    private int collisions = 0;
    public boolean isPaused = false;
    public boolean remBall = false;
    public boolean addBall = false;
    public boolean slowBalls = false;
    public boolean speedBalls = false;
    
    JPanel ballPanel;
    JLabel iteration;
    private JLabel iterLabel;
    
    Animation(JPanel panel,List<Ball> ballList, MyFrame frame, JPanel butPanel, JLabel iterLabel)
    {
    	readOnlyBallList = new ArrayList<Ball>();
    	ballPanelAddList = new ArrayList<Ball>();
        ballPanelRemList = new ArrayList<Ball>();
        
	    this.ballPanel = panel;
	    //create new 'read-only' list with copy of initial list
	    this.readOnlyBallList = ballList;
	    this.iterLabel = iterLabel;
    }
    
    private Ball addBallToList()
    {
    	Toolkit window = Toolkit.getDefaultToolkit();
        Dimension screen = window.getScreenSize();
        int panelHeight = screen.height;
        int panelWidth = screen.width;
    	int[] sizeArray  = {Ball.SMALL, Ball.MEDIUM, Ball.LARGE};
        Color[] colorArray = {Color.RED, Color.BLUE};
        Random rn = new Random();
        int ranSize = sizeArray[rn.nextInt(3)];
        double ranX = (100 + (rn.nextDouble()*(panelWidth/2)));
        double ranY = (100 + (rn.nextDouble()*(panelHeight/2)));
        double ranU = (0.5 + (rn.nextDouble()));
        double ranV = (0.5 + (rn.nextDouble()));
        System.out.println(ranX+" "+ranY+" "+ranU+" "+ranV);
        Color ranColor = colorArray[rn.nextInt(2)];
        Ball b = new Ball(ranSize,ranX,ranY,ranU,ranV,ranColor);
        return b;
    }
    private void slowBalls()
    {
    	for(Ball b: readOnlyBallList)
        {
            if(b.getU() > 1)
                b.setU(b.getU()-1); 
            if(b.getV() > 1)
                b.setV(b.getV()-1);
        }
    }
    private void speedBalls()
    {
    	for(Ball b: readOnlyBallList)
        {
            b.setU(b.getU()+1); b.setV(b.getV()+1);
        }
    }
    public void run()
    {
        for(iter =  0; iter < 100000; iter++)
        {
        	while(isPaused)
        	{
        		synchronized (this) 
        		{
    				try {
    					wait();
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
    			}
        	}
        	if(remBall && (readOnlyBallList.size() > 1))
        	{
        		readOnlyBallList.remove(readOnlyBallList.size()-1);//remove the last ball in the list
        		remBall = false;
        	}
        	if(addBall && (readOnlyBallList.size() < 100))
        	{
        		readOnlyBallList.add(addBallToList());//add random ball to the list
        		addBall = false;
        	}
        	if(slowBalls)
        	{
        		slowBalls();
        		slowBalls = false;
        	}
        	if(speedBalls)
        	{
        		speedBalls();
        		speedBalls = false;
        	}
            List<Ball> updatedBallList = new ArrayList<Ball>();
            int nBalls = readOnlyBallList.size();//say 20
            if(nBalls < 5)
            {
            	BallCollisionThread btc1 = new BallCollisionThread(readOnlyBallList.subList(3*((nBalls-1)/4),nBalls),ballPanel, readOnlyBallList);
	            btc1.start();
	     
	            try {
	                btc1.join();
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	            //once all threads are finished, create a new ballList with all the balls from each thread
	            
	            for(Ball b: btc1.getThreadBallList())
	            {
	            	updatedBallList.add(b);
	            }
	            
	            //copy balls from this List into another originalReadOnly List for next iteration
	            readOnlyBallList.clear();
	            for(Ball b: updatedBallList)
	            	readOnlyBallList.add(b);
	            
	            int blueBalls = 0, redBalls =0, smallBalls = 0, medBalls = 0, largeBalls = 0;
	            for(Ball b: readOnlyBallList)
	            	{
		        		if(b.getColor() == Color.BLUE)
		        			blueBalls++;
		        		else
		        			redBalls++;
		        		if(b.getSize() == 0)
		        			smallBalls++;
		        		else if(b.getSize() == 1)
		        			medBalls++;
		        		else
		        			largeBalls++;
		        		ArrayList<Ball> CList = b.getCollisionList();
		        		while(CList.size() > 0)
		        		{
		        			collisions++; CList.remove(CList.size()-1);
		        		}
	            	}
	        	System.out.println("Collisions: "+collisions);
	        	int totalCollisions = (collisions/2);//since two balls colliding is considered one collision(i'm not taking into account 3 ball collisions)
	        	System.out.println("Iterations: " + iter + "\tCollisions: " + collisions + "\tBlue Balls: " +blueBalls+
	        			"\tRedBalls: "+redBalls);
	            iterLabel.setText("Iterations: " + iter + "       Collisions: " + totalCollisions + "      BlueBalls: " +blueBalls+
	            		"      RedBalls: "+redBalls +"      SmallBalls: "+smallBalls + "     MedBalls: "+medBalls+
	        			"      LargeBalls: "+largeBalls + " Thread1: " + btc1.getBallsInThread());
	        			
            }
            else
            {
	            BallCollisionThread btc1 = new BallCollisionThread(readOnlyBallList.subList(0,(nBalls-1)/4),ballPanel, readOnlyBallList);
	            BallCollisionThread btc2 = new BallCollisionThread(readOnlyBallList.subList((nBalls-1)/4,(nBalls-1)/2),ballPanel, readOnlyBallList);
	            BallCollisionThread btc3 = new BallCollisionThread(readOnlyBallList.subList((nBalls-1)/2,3*((nBalls-1)/4)), ballPanel, readOnlyBallList);
	            BallCollisionThread btc4 = new BallCollisionThread(readOnlyBallList.subList(3*((nBalls-1)/4),nBalls),ballPanel, readOnlyBallList);
	            btc1.start();btc2.start();
	            btc3.start();btc4.start();
	     
	            try {
	                btc1.join(); btc2.join();
	                btc3.join();btc4.join();
	            } catch (InterruptedException e) {
	                e.printStackTrace();
	            }
	            //once all threads are finished, create a new ballList with all the balls from each thread
	            
	            for(Ball b: btc1.getThreadBallList())
	            {
	            	updatedBallList.add(b);
	            }
	            for(Ball b: btc2.getThreadBallList())
	            {
	            	updatedBallList.add(b);
	            }
	            for(Ball b: btc3.getThreadBallList())
	            {
	            	updatedBallList.add(b);
	            }
	            for(Ball b: btc4.getThreadBallList())
	            {
	            	updatedBallList.add(b);
	            }
	            //copy balls from this List into another originalReadOnly List for next iteration
	            readOnlyBallList.clear();
	            for(Ball b: updatedBallList)
	            	readOnlyBallList.add(b);
	            
	            int blueBalls = 0, redBalls =0, smallBalls = 0, medBalls = 0, largeBalls = 0;
	            for(Ball b: readOnlyBallList)
	            	{
		        		if(b.getColor() == Color.BLUE)
		        			blueBalls++;
		        		else
		        			redBalls++;
		        		if(b.getSize() == 0)
		        			smallBalls++;
		        		else if(b.getSize() == 1)
		        			medBalls++;
		        		else
		        			largeBalls++;
		        		ArrayList<Ball> CList = b.getCollisionList();
		        		if(CList.size() > 0)
		        		{
		        			collisions++; 
		        		}
	            	}
	          
	        	System.out.println("Collisions: "+collisions);
	        	int totalCollisions = (collisions/2);//since two balls colliding is considered one collision(i'm not taking into account 3 ball collisions)
	        	System.out.println("Iterations: " + iter + "\tCollisions: " + collisions + "\tBlue Balls: " +blueBalls+
	        			"\tRedBalls: "+redBalls);
	            iterLabel.setText("Iterations: " + iter + "       Collisions: " + totalCollisions + "      BlueBalls: " +blueBalls+
	            		"      RedBalls: "+redBalls +"      SmallBalls: "+smallBalls + "     MedBalls: "+medBalls+
	        			"      LargeBalls: "+largeBalls + " Thread1: " + btc1.getBallsInThread() +
	        			" Thread2: " +btc2.getBallsInThread() + " Thread3: "+btc3.getBallsInThread()
	        			+" Thread4: "+btc4.getBallsInThread());
            }
            //set the balllist to paint in the ballPanel to the new ballLIst
            ((BallPanel) ballPanel).setBallList(updatedBallList);
            Thread t = new Thread((Runnable) ballPanel);
            t.start();//repaint all the balls
            try {
                t.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                Thread.sleep(8);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();  
            }
        }   
    }
    synchronized void resume(){
    	isPaused = false;
    	notify();
    }
}

class BallCollisionThread extends Thread
{
    private List<Ball> threadBallList;
    private JPanel ballPanel;
    private List<Ball> entireList;
    private List<Ball> ballRemList;
    private List<Ball> ballAddList;
    
    BallCollisionThread(List<Ball> list,JPanel ballPanel, List<Ball> ballList){
	    this.threadBallList = new ArrayList<Ball>();
	    this.entireList = new ArrayList<Ball>();
	    this.ballRemList = new ArrayList<Ball>();
	    this.ballAddList = new ArrayList<Ball>();
	    for(Ball b: list)//copy from readonly list to modifiable thread-balllist
	    	threadBallList.add(b);
	    for(Ball b: ballList)//copy from readonly list ot modifiable entireballlist
	    	entireList.add(b);
	    this.ballPanel = ballPanel;
    }
    public void run(){
    	try
    	{
    		//can't use for each loops since they throw exceptions, using Iterator loops
	    	for (Ball b:threadBallList)
		    {
		         b.updatePosition(ballPanel.getBounds(), threadBallList, ballRemList);
		    }
	    	
		    for (Ball b:threadBallList)
		    {
		        b.checkRegularCollision(entireList, threadBallList, ballRemList, ballAddList);//add and remove balls from threadBallList
		        
		    }
		    for (Ball b:ballRemList)
	    	{
	    		int thisBall = threadBallList.indexOf(b);
	    		threadBallList.remove(thisBall);
	    	}		    	
		    for (Ball b:ballAddList)
	    	{
	    		threadBallList.add(b);
	    	}	
    	}catch(ConcurrentModificationException e){
    		e.printStackTrace();
    	}
    }  
    public int getBallsInThread()
    {
    	return threadBallList.size()-1;
    }
    public List<Ball> getThreadBallList()
    {
    	return threadBallList;
    }
}
   
class BallPanel extends JPanel implements Runnable{
    public List<Ball> ballList;
    
	private JPanel butPanel;
    private static int count = 0;
    private static boolean start = true;
    private int height;
    private int width;
    BallPanel(List<Ball> ballList, JPanel butPanel)
    {
	    this.ballList = ballList;
	    this.butPanel = butPanel;
	    
	    super.setBackground(Color.DARK_GRAY.brighter().brighter().brighter());
	    
	    Toolkit window = Toolkit.getDefaultToolkit();
	    Dimension screen = window.getScreenSize();
	    height = screen.height;
	    width = screen.width;
	    super.setBounds(0,0,width/3,height/3); 
    }
    public List<Ball> getBallList() {
		return ballList;
	}
	public void setBallList(List<Ball> ballList) {
		this.ballList = ballList;
	}
    //call by repaint();
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, 4, height);
        g2d.fillRect(0, 0, width, 4);
        g2d.fillRect((int)getBounds().getMaxX()-4, 0, 4, height);
        //drawing the gap for balling to tunnel out
        g2d.fillRect(0, (int)getBounds().getMaxY()-(int)getBounds().getY()-4, 
        		(4*(int)getBounds().getMaxX()/10), 4);
        g2d.fillRect((5*(int)getBounds().getMaxX()/10), (int)getBounds().getMaxY()-(int)getBounds().getY()-4, 
        		((int)getBounds().getMaxX()), 4);
        
        if(start)
        {
            for (Ball b:ballList)
            {
                b.checkInitialSpawnCollision(ballList);
            }
            start = false;
        }
        System.out.println("Paint Count "+count);
        butPanel.repaint();
        ++count;
        for (Ball b:ballList)
        {
            g2d.setColor(b.getColor());
            g2d.fill(b.getShape());    
        }   
    }
    @Override
    public void run() {
        // TODO Auto-generated method stub
        repaint();
    }    
}


