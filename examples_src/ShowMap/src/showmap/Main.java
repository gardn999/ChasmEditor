package showmap;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import mapdata.*;

public class Main extends JPanel implements ActionListener{
  TileMap tileMap = new TileMap();
  Timer timer;
  int x = 0, y = 0, dragX, dragY, dragMouseX, dragMouseY;
  boolean mouseDown;

  public static void main(String[] args){
    // init panel
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    frame.setResizable(true);
    Main main = new Main();
    main.setPreferredSize(new Dimension(1200, 800));
    frame.add(main);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setTitle("Show Map");
    frame.setVisible(true);
  }
  
  public Main(){
    // init tileMap: initAll(mapURL, imageURL, tileWidth, tileHeight) 
    tileMap.initAll(getClass().getResource("Metroid.map"),
                    getClass().getResource("Metroid.png"), 16, 16);
    
    // define mouse input for moving the view
    addMouseListener(new MouseAdapter(){
      @Override public void mousePressed(MouseEvent e){
        if (e.getButton() == MouseEvent.BUTTON1){ 
          setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
          mouseDown = true;
          dragX = x; dragY = y;
          dragMouseX = e.getX(); dragMouseY = e.getY();
        }
      }
      @Override public void mouseReleased(MouseEvent e){
        if (e.getButton() == MouseEvent.BUTTON1){ 
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          mouseDown = false;
        }
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
    // drag with left mouse button to move view
    if (mouseDown){
      Point pos = getMousePosition();
      if (pos != null){
        x = dragX + dragMouseX - pos.x;
        y = dragY + dragMouseY - pos.y;
      }
    }
    // draw black background
    g.setColor(Color.black);
    g.fillRect(0, 0, getWidth(), getHeight());
    // draw the map
    tileMap.drawMap(x, y, getWidth(), getHeight(), g, 0, 0);
    // draw text
    g.setColor(Color.white);
    g.drawString("Drag with left mouse button to move view.", 20, 30);
  }
}
