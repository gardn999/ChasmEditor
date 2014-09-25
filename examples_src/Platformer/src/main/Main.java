package main;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import mapdata.*;

public class Main extends JPanel implements ActionListener{
  final int TILESIZE = 32;
  TileMap tileMap = new TileMap();
  Player player = new Player(32*TILESIZE, 23*TILESIZE);
  boolean upDown, upPressed, leftDown, rightDown;
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
    frame.setTitle("Platformer Example");
    frame.setVisible(true);
  }
  
  public Main(){
    setPreferredSize(new Dimension(1200, 800));
    setFocusable(true);
    requestFocusInWindow();
    
    // init tileMap: initAll(mapURL, imageURL, tileWidth, tileHeight) 
    tileMap.initAll(getClass().getResource("Platformer.map"),
                    getClass().getResource("Platformer.png"), 
                    TILESIZE, TILESIZE);
    
    // keyboard input
    addKeyListener(new KeyAdapter(){
      @Override public void keyPressed(KeyEvent e){
        switch (e.getKeyCode()){
        case (KeyEvent.VK_UP): 
          if (!upDown) upPressed = true;
          upDown = true; 
          break;
        case (KeyEvent.VK_LEFT): leftDown = true; break;
        case (KeyEvent.VK_RIGHT): rightDown = true; break;
        }
      }
      @Override public void keyReleased(KeyEvent e){
        switch (e.getKeyCode()){
        case (KeyEvent.VK_UP): upDown = false; break;
        case (KeyEvent.VK_LEFT): leftDown = false; break;
        case (KeyEvent.VK_RIGHT): rightDown = false; break;
        }
      }
    });
    
    addMouseListener(new MouseAdapter(){
      @Override public void mouseExited(MouseEvent e){
        upDown = leftDown = rightDown = false;
      }
    });
    
    // set actionPerformed to run every 20 ms
    timer = new Timer(20, this);
    timer.start();
  }
  
  @Override public void actionPerformed(ActionEvent e){
    repaint();
  }
  
  @Override public void paintComponent(Graphics g){
    // updates
    long dt = -nanoTime; nanoTime = System.nanoTime(); dt+= nanoTime;
    tileMap.update(dt);
    player.update(dt/1e9);
    
    // draw black background
    g.setColor(Color.black);
    g.fillRect(0, 0, getWidth(), getHeight());
    int w = getWidth()/2, h = getHeight()/2;
    
    // draw the map layers behind player
    int x = (int)Math.round(player.x) + player.w/2 - w/2, 
        y = (int)Math.round(player.y) + player.h/2 - h/2;
    tileMap.setLayerRange(0, 0);
    tileMap.drawMap(x/8, y/8, w, h, g, 0, 0, w*2, h*2);
    tileMap.setLayerRange(1, 1);
    tileMap.drawMap(x/3, y/3+180, w, h, g, 0, 0, w*2, h*2);
    tileMap.setLayerRange(2, 3);
    tileMap.drawMap(x, y, w, h, g, 0, 0, w*2, h*2);
    
    // draw player
    player.draw(g);
         
    // draw layers after player
    tileMap.setLayerRange(4, 5);
    tileMap.drawMap(x, y, w, h, g, 0, 0, w*2, h*2);
    
    // draw info
    g.setColor(Color.white);
    g.drawString("Move: Left / Right Arrow, Jump: Up Arrow", 20, 30);
    g.drawString("COINS: " + player.coins, 20, 60);
    g.drawString("HEALTH: " + player.health, 20, 80);
  }
  
  // Player ////////////////////////////////////////////////////////////////////
  class Player{
    final int w = 24, h = 56;
    final double aGrav = 350, xSpeed = 150, jumpSpeed = 225, vyMax = 2000,
                 dmgDelay = 0.5;
    double x, y, vx, vy, oldvy, dmgTimer;
    int coins = 0, health = 100;
    Color color = Color.blue;
    
    Player(double x, double y){ 
      this.x = x; this.y = y; vx = 0; vy = oldvy = 0; 
    }
    
    void update(double dt){
      if (dmgTimer > 0.0) dmgTimer-= dt;
      color = (dmgTimer > 0.0) ? Color.red : Color.blue;
      
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
      if (leftDown) vx-= xSpeed;
      if (rightDown) vx+= xSpeed;
      
      // jump
      boolean onGround = collisionAt(x, y+6);
      if (upPressed && onGround) vy = -jumpSpeed;
      upPressed = false;
      
      // update vertical speed
      oldvy = vy; vy+= aGrav*dt;
      if (vy > vyMax) vy = vyMax;
      
      // move player in 1 pixel steps while checking for a collision
      double vyAvg = (vy + oldvy)/2;
      int n = (int)(dt*Math.max(Math.abs(vx), Math.abs(vyAvg))) + 1;
      double stepx = vx*dt/n, stepy = vyAvg*dt/n;
      for (int i = 0; i < n; i++){
        if ((vx == 0.0) && (vy == 0.0)) break;
        if (vx != 0.0){
          if (!collisionAt(x+stepx, y)) 
            x+= stepx;
          else if (onGround && !collisionAt(x+stepx, y-10)){ 
            y-= 10; x+= stepx; 
          }else 
            vx = 0.0;
        }
        if (vy != 0.0){
          if (collisionAt(x, y+stepy)) vy = 0.0; else y+= stepy;
        }
      }
      
      // check for collisions with special objects
      for (int j = xtoi(y); j <= xtoi(y+h); j++)
        for (int i = xtoi(x); i <= xtoi(x+w); i++){
          int[] layer = tileMap.get(i, j);
           // with coins in layer 5
          if (tileMap.getName(layer[5]).equals("coin")){
            tileMap.set(0, 5, i, j);  // set tile to 0 at layer 5, (i,j)
            coins++;
          }
          if (tileMap.getType(layer[2]).equals("ChangeMap")){
            tileMap.setTopMap(1-tileMap.currentTopMapIndex());
          }
          // with damaging objects in layer 3
          if ((dmgTimer <= 0.0) && (layer[3]&4) == 4){
            int baseID = tileMap.getBaseTileID(layer[3]);
            if (tileMap.getType(baseID).equals("damage")){ 
              health--; dmgTimer = dmgDelay;
            }
          }
        }
          
    }  
    
    // convert from pixel to tile based coordinates
    int xtoi(double x){
      return (x < 0) ? (int)(x/TILESIZE) - 1 : (int)(x/TILESIZE);
    }
    
    void draw(Graphics g){
      g.setColor(color);
      g.fillRect((getWidth()/4 - w/2)*2, (getHeight()/4 - h/2)*2, 2*w, 2*h);
    }
    
    boolean collisionAt(double testx, double testy){
      return tileMap.collisionAt(testx, testy, w, h);
    }
  }
  
}
