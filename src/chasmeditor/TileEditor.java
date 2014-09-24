package chasmeditor;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.text.NumberFormatter;
import mapdata.TileMap;

public class TileEditor extends JPanel{
  Main main; 
  TileMap tileMap;
  JSplitPane splitPane;
  JPanel leftPanel, rightPanel;
  TilesPanel baseTilesPanel, compTilesPanel;
  LeftEditPanel leftEditPanel;
  RightEditPanel rightEditPanel;
  JList compList;
  ArrayList<String> compArrayList;
  boolean changed = false;
  
  TileEditor(Main main){
    this.main = main;
    tileMap = main.tileMap;
    setLayout(new BorderLayout());
    
    // leftPanel ///////////////////////////////////////////////////////////////
    leftPanel = new JPanel(new BorderLayout());
    
    baseTilesPanel = new TilesPanel(false);
    leftPanel.add(baseTilesPanel.mainPanel, BorderLayout.CENTER);

    leftEditPanel = new LeftEditPanel();
    leftPanel.add(leftEditPanel, BorderLayout.EAST);
    
    // rightPanel //////////////////////////////////////////////////////////////
    rightPanel = new JPanel(new BorderLayout());
    
    rightEditPanel = new RightEditPanel();
    rightPanel.add(rightEditPanel, BorderLayout.WEST);
    
    compTilesPanel = new TilesPanel(true);
    rightPanel.add(compTilesPanel.mainPanel, BorderLayout.CENTER);
    
    ////////////////////////////////////////////////////////////////////////////
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                    leftPanel, rightPanel);
    splitPane.setResizeWeight(0.5);
    add(splitPane, BorderLayout.CENTER);
  }
  
  void updateCompList(){
    compArrayList.clear();
    TileMap.CompTile comp = tileMap.getCompTile(compTilesPanel.tile);
    for (int i = 0; i < comp.infoSize(); i++){
      TileMap.CompInfo info = comp.getInfo(i);
      compArrayList.add(String.format("%3d%6d%6d%6.2f%3s%2s", i, 
           info.baseID>>3, info.time, info.alpha, 
           ((info.baseID & 1) == 1) ? "Y" : "N", 
           ((info.baseID & 2) == 2) ? "Y" : "N"));
    }
    compList.setListData(compArrayList.toArray());
    compList.setSelectedIndex(0);
  }
    
  void update(){
    baseTilesPanel.update();
    leftEditPanel.update();
    rightEditPanel.update();
    compTilesPanel.update();
  }
  
  class TilesPanel extends JPanel{
    boolean isComp, shiftDown;
    int oldTile, tile, nx, ny;
    JPanel mainPanel, panel1, panel2;
    JLabel nameLabel, typeLabel;
    JTextField nameField, typeField;
    
    TilesPanel(boolean isComposite){ 
      isComp = isComposite;
      
      mainPanel = new JPanel(new BorderLayout());
      
      panel1 = new JPanel();
      panel1.setBorder(BorderFactory.createEmptyBorder(0, 0, -5, 0));
      nameLabel = new JLabel(
           String.format("%-10sName:", isComp ? "CompTile" : "BaseTile"));
      nameLabel.setFont(main.monoFont);
      panel1.add(nameLabel);
      nameField = new JTextField();
      nameField.setPreferredSize(new Dimension(120, 20));
      panel1.add(nameField);
      
      panel2 = new JPanel();
      typeLabel = new JLabel();
      typeLabel.setFont(main.monoFont);
      panel2.add(typeLabel);
      typeField = new JTextField();
      typeField.setPreferredSize(new Dimension(120, 20));
      panel2.add(typeField);

      JPanel infoPanel = new JPanel();
      infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
      infoPanel.add(panel1);
      infoPanel.add(panel2);
      mainPanel.add(infoPanel, BorderLayout.NORTH);
      
      JScrollPane scrollPane = new JScrollPane(this);
      mainPanel.add(scrollPane, BorderLayout.CENTER);
      
      addKeyListener(new KeyAdapter(){
        @Override public void keyPressed(KeyEvent e){
          if (e.getKeyCode() == KeyEvent.VK_SHIFT){ 
            shiftDown = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          }
        }
        @Override public void keyReleased(KeyEvent e){
          if (e.getKeyCode() == KeyEvent.VK_SHIFT){
            shiftDown = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        }
      });
      
      addMouseListener(new MouseAdapter(){
        @Override public void mousePressed(MouseEvent e){
          if (e.getButton() == MouseEvent.BUTTON1){
            if ((e.getX() >= 0) && (e.getX() < main.tileW*nx) &&
                (e.getY() >= 0) && (e.getY() < main.tileH*ny)){ 
              int i = e.getX()/main.tileW, j = e.getY()/main.tileH;
              int oldTile = tile;
              tile = i + j*nx;
              if (shiftDown){
                if (isComp){
                  if (oldTile != tile){
                    tileMap.copyCompTile(oldTile, tile);
                    changed = true;
                  }
                }else{
                  TileMap.BaseTile b = tileMap.getBaseTile(tile);
                  b.setSolid( (b.getSolid() == 0) ? (short)(-1) : 0);
                  changed = true;
                }
              }
            }
          }
        }
        @Override public void mouseEntered(MouseEvent e){
          requestFocusInWindow();
        }
        @Override public void mouseExited(MouseEvent e){
          shiftDown = false;
        }
      });
    }
    
    void init(int nx, int ny){ 
      oldTile = -1;
      this.nx = nx; this.ny = ny; 
      setPreferredSize(new Dimension(nx*main.tileW, ny*main.tileH));
      revalidate();
    }
    
    void update(){
      if (!isComp && (tile > tileMap.nImageTiles())) tile = 0;
      int ID = (isComp) ? tile*8 + 4 : tile*8;
      
      if (oldTile == tile){
        if (!tileMap.getName(ID).equals(nameField.getText()) ||
            !tileMap.getType(ID).equals(typeField.getText()) ){
          tileMap.setName(ID, nameField.getText());
          tileMap.setType(ID, typeField.getText());
          changed = true;
        }
      }else{
        oldTile = tile;
        nameField.setText(tileMap.getName(ID));
        typeField.setText(tileMap.getType(ID));
      }
    }
    
    @Override public void paintComponent(Graphics g){
      Point pos = getMousePosition();
      int ID = isComp ? 4 : 0;
      for (int j = 0; j < ny; j++)
        for (int i = 0; i < nx; i++){
          int dx1 = i*main.tileW, dy1 = j*main.tileH;
          g.setColor(((i+j)%2 == 0) ? main.bg1 : main.bg2);
          g.fillRect(dx1, dy1, main.tileW, main.tileH);
          tileMap.drawTile(g, ID, dx1, dy1);
          if (leftEditPanel.showSolid.isSelected())
            tileMap.drawSolid(g, tileMap.getSolid(ID), main.solidColor,
                 i*main.tileW, j*main.tileH, main.tileW, main.tileH);
          ID+= 8;
        }
      g.setColor(main.selectColor);
      if ((pos != null) && (pos.x < nx*main.tileW) && (pos.y < ny*main.tileH)){
        int i = pos.x/main.tileW, j = pos.y/main.tileH;
        g.drawRect(i*main.tileW-1, j*main.tileH-1, main.tileW+1, main.tileH+1);
      }
      int i = tile%nx, j = tile/nx;
      g.drawRect(i*main.tileW, j*main.tileH, main.tileW-1, main.tileH-1);
      typeLabel.setText(String.format("%8d  Type:", tile));
    }
  }
  
  class LeftEditPanel extends JPanel{
    SolidPanel solidPanel;
    JFormattedTextField timeText, alphaText;
    JButton addButton, removeButton, insertButton, changeButton;
    JCheckBox showSolid, hFlip, vFlip, 
              changeTile, changeTime, changeAlpha, changeHFlip, changeVFlip;
    int w = 120;
    
    LeftEditPanel(){ 
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
      add(Box.createVerticalGlue());
      
      JLabel solidLabel = new JLabel("Edit BaseTile");
      solidLabel.setAlignmentX(0.5f);
      add(solidLabel);
      JLabel solidLabel2 = new JLabel("4x4 Solid Map");
      solidLabel2.setAlignmentX(0.5f);
      add(solidLabel2);
      
      solidPanel = new SolidPanel(w);
      add(solidPanel);

      showSolid = new JCheckBox("Show Solid?");
      showSolid.setMaximumSize(new Dimension(100, 30));
      showSolid.setAlignmentX(0.5f);
      add(showSolid);
      
      add(Box.createVerticalGlue());
      add(new JSeparator(JSeparator.HORIZONTAL));
      
      JLabel tileEditLabel = new JLabel("Modify CompTile");
      tileEditLabel.setAlignmentX(0.5f);
      add(tileEditLabel);
      
      add(new JSeparator(JSeparator.HORIZONTAL));
      
      addButton = new JButton("Add");
      addButton.setMaximumSize(new Dimension(100, 30));
      addButton.setAlignmentX(0.5f);
      addButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          addCompTile();
          compList.setSelectedIndex(compList.getModel().getSize()-1);
        }
      });
      add(addButton);
    
      add(Box.createVerticalGlue());
      
      removeButton = new JButton("Remove");
      removeButton.setMaximumSize(new Dimension(100, 30));
      removeButton.setAlignmentX(0.5f);
      removeButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          int i = compList.getSelectedIndex();
          if (i >= 0){ 
            removeCompTile(i);
            int size = compList.getModel().getSize();
            compList.setSelectedIndex(i < size ? i : size-1);
          }
        }
      });
      add(removeButton);
    
      add(Box.createVerticalGlue());
      
      insertButton = new JButton("Insert");
      insertButton.setMaximumSize(new Dimension(100, 30));
      insertButton.setAlignmentX(0.5f);
      insertButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          int i = compList.getSelectedIndex();
          if (i >= 0) insertCompTile(i); else addCompTile();
          compList.setSelectedIndex(i+1);
        }
      });
      add(insertButton);
      
      add(Box.createVerticalGlue());
      add(new JSeparator(JSeparator.HORIZONTAL));
      
      JLabel timeLabel = new JLabel("Display Time (ms)");
      timeLabel.setAlignmentX(0.5f);
      add(timeLabel);
      
      NumberFormatter timeFormat = new NumberFormatter();
      timeFormat.setMinimum(new Integer(1));
      timeText = new JFormattedTextField(timeFormat);
      timeText.setValue(500);
      timeText.setMaximumSize(new Dimension(80, 30));
      timeText.setAlignmentX(0.5f);
      add(timeText);

      add(Box.createVerticalGlue());
      
      JLabel alphaLabel = new JLabel("Alpha(0 to 1)");
      alphaLabel.setAlignmentX(0.5f);
      add(alphaLabel);
      
      NumberFormatter alphaFormat = new NumberFormatter();
      alphaFormat.setMinimum(new Float(0f));
      alphaFormat.setMaximum(new Float(1f));
      alphaText = new JFormattedTextField(alphaFormat);
      alphaText.setValue(1f);
      alphaText.setMaximumSize(new Dimension(80, 30));
      alphaText.setAlignmentX(0.5f);
      add(alphaText);

      add(Box.createVerticalGlue());
      
      hFlip = new JCheckBox("Horizontal Flip?");
      hFlip.setAlignmentX(0.5f);
      add(hFlip);
      
      vFlip = new JCheckBox("Vertical Flip?");
      vFlip.setAlignmentX(0.5f);
      add(vFlip);
      
      add(Box.createVerticalGlue());
      add(new JSeparator(JSeparator.HORIZONTAL));
      
      changeButton = new JButton("Change");
      changeButton.setMaximumSize(new Dimension(100, 30));
      Insets changeInsets = changeButton.getMargin();
      changeInsets.left = changeInsets.right = 0;
      changeButton.setMargin(changeInsets);
      changeButton.setAlignmentX(0.5f);
      changeButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          int i = compList.getSelectedIndex();
          if (i >= 0) changeCompTile(i);
          int index = (i+1 < compList.getModel().getSize()) ? i+1 : i;
          compList.setSelectedIndex(index);
        }
      });
      add(changeButton);
      
      JLabel changeLabel = new JLabel("Values to Change:");
      changeLabel.setAlignmentX(0.5f);
      add(changeLabel);
      
      JPanel changePanel = new JPanel();
      changePanel.setLayout(new GridLayout(3, 2));
      changeTile = new JCheckBox("Tile ");
      changePanel.add(changeTile);
      changeTime = new JCheckBox("Time ");
      changePanel.add(changeTime);
      changeAlpha = new JCheckBox("Alpha");
      changePanel.add(changeAlpha);
      changeHFlip = new JCheckBox("HFlip");
      changePanel.add(changeHFlip);
      changeVFlip = new JCheckBox("VFlip");
      changePanel.add(changeVFlip);
      add(changePanel);
      
      add(Box.createVerticalGlue());
    }
    
    void addCompTile(){
      int ms = ((Number)timeText.getValue()).intValue();
      float alpha = ((Number)alphaText.getValue()).floatValue();
      tileMap.getCompTile(compTilesPanel.tile).add( (baseTilesPanel.tile<<3) + 
           (hFlip.isSelected() ? 1 : 0) + (vFlip.isSelected() ? 2 : 0), 
           ms, alpha);
      tileMap.resetCompTimes();
      updateCompList();
      changed = true;
    }
    
    void removeCompTile(int i){
      tileMap.getCompTile(compTilesPanel.tile).remove(i);
      tileMap.resetCompTimes();
      updateCompList();
      changed = true;
    }
    
    void insertCompTile(int i){
      int ms = ((Number)timeText.getValue()).intValue();
      float alpha = ((Number)alphaText.getValue()).floatValue();
      tileMap.getCompTile(compTilesPanel.tile).insert(i, 
           (baseTilesPanel.tile<<3) + (hFlip.isSelected() ? 1 : 0) + 
           (vFlip.isSelected() ? 2 : 0), ms, alpha);
      tileMap.resetCompTimes();
      updateCompList();
      changed = true;
    }
    
    void changeCompTile(int i){
      if (!(changeTile.isSelected() || 
            changeTime.isSelected() || changeAlpha.isSelected() ||
            changeHFlip.isSelected() || changeVFlip.isSelected())) return;
      
      TileMap.CompInfo info = 
           tileMap.getCompTile(compTilesPanel.tile).getInfo(i);
      
      if (changeTile.isSelected()){
        info.baseID = (baseTilesPanel.tile<<3) + (info.baseID&7);
        tileMap.resetCompTimes();
      }
      if (changeTime.isSelected()){
        info.time = ((Number)timeText.getValue()).intValue();
        tileMap.resetCompTimes();
      }
      if (changeAlpha.isSelected()){
        info.alpha = ((Number)alphaText.getValue()).floatValue();
      }
      if (changeHFlip.isSelected()){
        if (hFlip.isSelected()) info.baseID |= 1; 
        else info.baseID &= ~1;
      }
      if (changeVFlip.isSelected()){
        if (vFlip.isSelected()) info.baseID |= 2; 
        else info.baseID &= ~2;
      }
      updateCompList();
      changed = true;
    }
  
    void update(){
      solidPanel.update();
    }
    
  }
  
  class SolidPanel extends JPanel{
    int x;
    boolean lmbDown, rmbDown;
    
    SolidPanel(int panelW){ 
      x = (panelW-100)/2; 
      setPreferredSize(new Dimension(panelW, 108));
      setMaximumSize(new Dimension(panelW, 108));
      
      addMouseListener(new MouseAdapter(){
        @Override public void mousePressed(MouseEvent e){
          if (e.getButton() == MouseEvent.BUTTON1) lmbDown = true;
          if (e.getButton() == MouseEvent.BUTTON3) rmbDown = true;
        }
        
        @Override public void mouseReleased(MouseEvent e){
          if (e.getButton() == MouseEvent.BUTTON1) lmbDown = false;
          if (e.getButton() == MouseEvent.BUTTON3) rmbDown = false;
        }
      });
    }
    
    void update(){
      if (baseTilesPanel.tile <= 0) return;  // tile 0 can't be solid
      Point pos = getMousePosition();
      if ((pos != null) && (lmbDown || rmbDown)){
        int mx = pos.x - x, my = pos.y - 4;
        if ((mx >= 0) && (mx < 100) && (my >= 0) && (my < 100)){
          int i = mx/25, j = my/25;
          TileMap.BaseTile b = tileMap.getBaseTile(baseTilesPanel.tile);
          if (lmbDown && !b.isSolid(i, j)){ 
            b.setSolid(i, j); changed = true;
          }else if (rmbDown && b.isSolid(i, j)){
            b.clearSolid(i, j); changed = true;
          }
        }
      }
    }
    
    @Override public void paintComponent(Graphics g){
      int t = baseTilesPanel.tile;
      g.setColor(((t/baseTilesPanel.nx + t%baseTilesPanel.nx)%2 == 0) ? 
           main.bg1 : main.bg2);
      g.fillRect(x, 4, 100, 100);
      if (t <= 0) return;  // tile 0 can't be solid
      tileMap.drawTile(g, t*8, x, 4, 100, 100);
      g.setColor(Color.black); g.drawRect(x-1, 3, 101, 101);
      for (int j = 0; j < 4; j++)
        for (int i = 0; i < 4; i++)
          g.drawRect(x + i*25, 4 + j*25, 24, 24);
      tileMap.drawSolid(g, tileMap.getSolid(t*8), main.solidColor, 
           x, 4, 100, 100);
    }
  }    
  
  class RightEditPanel extends JPanel{
    int tile = -1;
    JButton refreshButton, exitButton, saveButton, restoreButton;
    
    RightEditPanel(){ 
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
      JPanel panel1 = new JPanel();
      
      refreshButton = new JButton("Refresh Image");
      Insets refreshInsets = refreshButton.getMargin();
      refreshInsets.left = refreshInsets.right = 0;
      refreshButton.setMargin(refreshInsets);
      refreshButton.setPreferredSize(new Dimension(100, 30));
      refreshButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmRefreshImage();
        }
      });
      panel1.add(refreshButton);
      
      exitButton = new JButton("Exit");
      exitButton.setPreferredSize(new Dimension(100, 30));
      exitButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmExit();
        }
      });
      panel1.add(exitButton);
      
      panel1.setMaximumSize(new Dimension(230, 30));
      add(panel1);
      
      JPanel panel2 = new JPanel();
      
      saveButton = new JButton("Save");
      saveButton.setPreferredSize(new Dimension(100, 30));
      saveButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmSave();
        }
      });
      panel2.add(saveButton);

      restoreButton = new JButton("Restore");
      restoreButton.setPreferredSize(new Dimension(100, 30));
      restoreButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmRestore();
        }
      });
      panel2.add(restoreButton);
      
      panel2.setMaximumSize(new Dimension(230, 30));
      add(panel2);
      
      add(new PreviewPanel(200));
      
      JLabel listLabel = new JLabel("         Time        Flip");
      listLabel.setFont(main.monoFont);
      listLabel.setAlignmentX(0.5f);
      add(listLabel);
      
      JLabel listLabel2 = new JLabel("i  Tile  (ms) Alpha  H V");
      listLabel2.setFont(main.monoFont);
      listLabel2.setAlignmentX(0.5f);
      add(listLabel2);
      
      add(new JLabel());
      
      compArrayList = new ArrayList<String>();
      compList = new JList();
      compList.setLayoutOrientation(JList.VERTICAL);
      compList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      compList.setFont(main.monoFont);
      JScrollPane compListScrollPane = new JScrollPane(compList);
      compListScrollPane.setPreferredSize(new Dimension(230, 160));
      add(compListScrollPane);
    }
    
    void update(){
      saveButton.setEnabled(main.changeMade());
      restoreButton.setEnabled(main.changeMade());
      if (tile != compTilesPanel.tile){ 
        updateCompList();
        tile = compTilesPanel.tile;
      }
    }
    
  }
  
  class PreviewPanel extends JPanel{
    int x0, size;
    
    PreviewPanel(int panelW){ 
      x0 = 10; size = panelW - 20;
      setPreferredSize(new Dimension(panelW, size+8));
      setMaximumSize(new Dimension(panelW, size+8));
    }
    
    @Override public void paintComponent(Graphics g){
      int w = (main.tileW >= main.tileH) ? size : size*main.tileW/main.tileH,
          h = (main.tileH >= main.tileW) ? size : size*main.tileH/main.tileW,
          x = x0  + (size-w)/2 + 1, y = (size-h)/2 + 4;
      g.setColor(Color.black); g.drawRect(x-1, y-1, w+1, h+1);
      int t = compTilesPanel.tile;
      g.setColor(((t/compTilesPanel.nx + t%compTilesPanel.nx)%2 == 0) ?  
           main.bg1 : main.bg2);
      g.fillRect(x, y, w, h);
      tileMap.drawTile(g, t*8 + 4, x, y, w, h);
      if (leftEditPanel.showSolid.isSelected())
        tileMap.drawSolid(g, tileMap.getSolid(t*8 + 4), main.solidColor, 
             x, y, w, h);
    }
  }    
}
