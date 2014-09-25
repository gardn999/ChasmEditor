
package main;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import mapdata.*;

public class Main extends JPanel implements ActionListener{
  final int TILESIZE = 32;
  TileMap tileMap = new TileMap();
  Player player = new Player(82*TILESIZE, 30*TILESIZE);
  boolean up, down, left, right;
  long nanoTime = System.nanoTime();
  Timer timer;
  
  public static void main(String[] args){
    // init panel
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    frame.setResizable(true);
    Main main = new Main();
    frame.add(main);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setTitle("Simple Game Example");
    frame.setVisible(true);
  }
  
  public Main(){
    setPreferredSize(new Dimension(1200, 800));
    setFocusable(true);
    requestFocusInWindow();
    
    // init tileMap: initAll(mapURL, imageURL, tileWidth, tileHeight) 
    tileMap.initAll(getClass().getResource("BasicExample.map"),
                    getClass().getResource("BasicExample.png"), 
                    TILESIZE, TILESIZE);
    
    // keyboard input
    addKeyListener(new KeyAdapter(){
      @Override public void keyPressed(KeyEvent e){
        switch (e.getKeyCode()){
        case (KeyEvent.VK_UP): up = true; break;
        case (KeyEvent.VK_DOWN): down = true; break;
        case (KeyEvent.VK_LEFT): left = true; break;
        case (KeyEvent.VK_RIGHT): right = true; break;
        }
      }
      @Override public void keyReleased(KeyEvent e){
        switch (e.getKeyCode()){
        case (KeyEvent.VK_UP): up = false; break;
        case (KeyEvent.VK_DOWN): down = false; break;
        case (KeyEvent.VK_LEFT): left = false; break;
        case (KeyEvent.VK_RIGHT): right = false; break;
        }
      }
    });
    
    addMouseListener(new MouseAdapter(){
      @Override public void mouseExited(MouseEvent e){
        up = down = left = right = false;
      }
    });
    
    // set actionPerformed to run every 20 ms
    timer = new Timer(20, this);
    timer.start();
  }
  
  @Override public void actionPerformed(ActionEvent e){
    long dt = -nanoTime; nanoTime = System.nanoTime(); dt+= nanoTime;
    tileMap.update(dt);
    player.update(dt/1e9);
    repaint();
  }
  
  @Override public void paintComponent(Graphics g){
    // draw black background
    g.setColor(Color.black);
    g.fillRect(0, 0, getWidth(), getHeight());
    
    // draw map centered on the player
    int x = (int)Math.round(player.x) + player.size/2 - getWidth()/2, 
        y = (int)Math.round(player.y) + player.size/2 - getHeight()/2;
    tileMap.drawMap(x, y, getWidth(), getHeight(), g, 0, 0);
    
    // draw player
    player.draw(g);
         
    // draw info
    g.setColor(Color.white);
    g.drawString("Move With Arrow Keys", 20, 30);
  }
  
  // Player ////////////////////////////////////////////////////////////////////
  class Player{
    final int size = 30;
    final double speed = 500;
    double x, y, vx, vy;
    
    Player(double x, double y){ this.x = x; this.y = y; }
    
    void update(double dt){
      // move player if stuck in solid object
      if (collisionAt(x, y)){
        for (int i = 1; i <= TILESIZE*5; i+= 4){
          if (!collisionAt(x, y-i)){ y = y - i; break; }
          if (!collisionAt(x, y+i)){ y = y + i; break; }
          if (!collisionAt(x-i, y)){ x = x - i; break; }
          if (!collisionAt(x+i, y)){ x = x + i; break; }
        }
      }
      
      // left/right input
      vx = 0;
      if (left) vx-= speed;
      if (right) vx+= speed;
      vy = 0;
      if (up) vy-= speed;
      if (down) vy+= speed;
      
      
      // move player in 1 pixel steps while checking for a collision
      int n = (int)(dt*Math.max(Math.abs(vx), Math.abs(vy))) + 1;
      double stepx = vx*dt/n, stepy = vy*dt/n;
      for (int i = 0; i < n; i++){
        if ((vx == 0.0) && (vy == 0.0)) break;
        if (vx != 0.0){
          if (collisionAt(x+stepx, y)) vx = 0.0; else x+= stepx;
        }
        if (vy != 0.0){
          if (collisionAt(x, y+stepy)) vy = 0.0; else y+= stepy;
        }
      }
    }  
    
    void draw(Graphics g){
      g.setColor(Color.blue);
      g.fillRect(getWidth()/2 - size/2, getHeight()/2 - size/2, size, size);
    }
    
    boolean collisionAt(double testx, double testy){
      return tileMap.collisionAt(testx, testy, size, size);
    }
  }
  
}
