package chasmeditor;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.*;
import mapdata.TileMap;

public class MapEditor extends JPanel{
  Main main; 
  TileMap tileMap;
  BufferedImage mapImage = null;
  JSplitPane mainSplitPane, tilesMapSplitPane;
  JTabbedPane tilesTabbedPane, mapTabbedPane;
  OptionsPanel optionsPanel;
  TilesPanel baseTilesPanel, compTilesPanel;
  JScrollPane baseTilesScrollPane, compTilesScrollPane;
  MapPanel mapPanel;
  LayersPanel layersPanel;
  MapSelectPanel mapSelectPanel;
  ViewPanel viewPanel;
  final int maxSelectionSize = 64;
  int[][] selectionTiles;
  JButton refreshButton, settingsButton, 
          exitButton, saveButton, restoreButton, 
          undoButton, redoButton, gridButton;
  JLabel tileInfo;
  JCheckBox showSolid;
  History hist = new History();
  // layers
  int selectedLayer, currentLayer;
  boolean[] isVisible;
  boolean changed = false;
  
  MapEditor(Main main){
    this.main = main;
    tileMap = main.tileMap;
    setLayout(new BorderLayout());
    
    mapImage = new BufferedImage(480, 480, BufferedImage.TYPE_INT_ARGB);
    
    optionsPanel = new OptionsPanel();
    add(optionsPanel, BorderLayout.NORTH);
    
    baseTilesPanel = new TilesPanel(false);
    baseTilesScrollPane = new JScrollPane(baseTilesPanel);
    
    compTilesPanel = new TilesPanel(true);
    compTilesScrollPane = new JScrollPane(compTilesPanel);
    
    tilesTabbedPane = new JTabbedPane(JTabbedPane.TOP);
    tilesTabbedPane.add("Base Tiles", baseTilesScrollPane);
    tilesTabbedPane.add("Composite Tiles", compTilesScrollPane);
    
    mapPanel = new MapPanel();
    mapPanel.setPreferredSize(new Dimension(mapPanel.w0, mapPanel.h0));

    layersPanel = new LayersPanel();
    
    mapSelectPanel = new MapSelectPanel();

    mapTabbedPane = new JTabbedPane(JTabbedPane.TOP);
    mapTabbedPane.add("Map View", mapPanel);
    mapTabbedPane.add("Map Layers", layersPanel);
    mapTabbedPane.add("Map Select", mapSelectPanel);
    
    tilesMapSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,  
      tilesTabbedPane, mapTabbedPane);
    tilesMapSplitPane.setPreferredSize(new Dimension(mapPanel.w0, mapPanel.h0));
    tilesMapSplitPane.setResizeWeight(0.5);

    viewPanel = new ViewPanel();
    
    mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,  
      tilesMapSplitPane, viewPanel);
    add(mainSplitPane, BorderLayout.CENTER);
    
    // select BaseTile panel action
    Action baseTileAction = new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        tilesTabbedPane.setSelectedIndex(0);
      }
    };
    getActionMap().put("selectBaseTile", baseTileAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
         put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "selectBaseTile");
    
    // select CompTile panel action
    Action compTileAction = new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        tilesTabbedPane.setSelectedIndex(1);
      }
    };
    getActionMap().put("selectCompTile", compTileAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
         put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "selectCompTile");
    
    // select previous map panel
    Action prevMapAction = new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        int i = mapTabbedPane.getSelectedIndex();
        mapTabbedPane.setSelectedIndex( (i==0) ? 2 : i-1);
      }
    };
    getActionMap().put("prevMap", prevMapAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
         put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "prevMap");
    
    // select next map panel
    Action nextMapAction = new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        int i = mapTabbedPane.getSelectedIndex();
        mapTabbedPane.setSelectedIndex( (i==2) ? 0 : i+1);
      }
    };
    getActionMap().put("nextMap", nextMapAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
         put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "nextMap");
    
    // select previous current map layer
    Action prevLayerAction = new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        mapTabbedPane.setSelectedIndex(1);
        currentLayer = (currentLayer == 0) ? tileMap.totalLayers() - 1 
                                           : currentLayer - 1;
        layersPanel.currentLayerButton[currentLayer].setSelected(true);
        layersPanel.innerPanel.scrollRectToVisible(
             layersPanel.currentLayerButton[currentLayer].getBounds());
      }
    };
    getActionMap().put("prevLayer", prevLayerAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
         put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "prevLayer");
    
    // select next current map layer
    Action nextLayerAction = new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        mapTabbedPane.setSelectedIndex(1);
        currentLayer = (currentLayer == tileMap.totalLayers()-1) ? 0 
                                           : currentLayer+1;
        layersPanel.currentLayerButton[currentLayer].setSelected(true);
        layersPanel.innerPanel.scrollRectToVisible(
             layersPanel.currentLayerButton[currentLayer].getBounds());
      }
    };
    getActionMap().put("nextLayer", nextLayerAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
         put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "nextLayer");
    
    // toggle select all layers
    Action selectAllAction = new AbstractAction(){
      @Override public void actionPerformed(ActionEvent e){
        mapTabbedPane.setSelectedIndex(1);
        layersPanel.selectAllBox.setSelected(
             !layersPanel.selectAllBox.isSelected());
      }
    };
    getActionMap().put("selectAllLayers", selectAllAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
         put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "selectAllLayers");
  }
  
  void update(){
    saveButton.setEnabled(main.changeMade());
    restoreButton.setEnabled(main.changeMade());
    undoButton.setEnabled(hist.undoLeft());
    redoButton.setEnabled(hist.redoLeft());
    mapPanel.update();
    layersPanel.update();
    mapSelectPanel.update();
    viewPanel.update();
  }  
  
  void scrollToSelectedTile(){
    int ID = selectionTiles[0][selectedLayer];
    tilesTabbedPane.setSelectedIndex( ((ID&4) == 0) ? 0 : 1 );
    TilesPanel tp = ((ID&4) == 0) ? baseTilesPanel : compTilesPanel;
    tp.scrollRectToVisible(new Rectangle(((ID/8)%tp.nx)*main.tileW, 
         ((ID/8)/tp.nx)*main.tileH, main.tileW, main.tileH));
  }
  
  class OptionsPanel extends JPanel{
    OptionsPanel(){
      setLayout(new FlowLayout(FlowLayout.LEADING, 4, 5));

      tileInfo = new JLabel("TILE: -", JLabel.CENTER);
      tileInfo.setPreferredSize(new Dimension(105, 25));
      tileInfo.setFont(main.monoFont);
      add(tileInfo);
    
      showSolid = new JCheckBox("Show Solid?");
      add(showSolid);
      
      add(new JLabel("  "));
      
      refreshButton = new JButton("Refresh Image");
      refreshButton.setMargin(new Insets(0, 0, 0, 0));
      refreshButton.setPreferredSize(new Dimension(100, 25));
      refreshButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmRefreshImage();
        }
      });
      add(refreshButton);
      
      add(new JLabel("  "));
      
      exitButton = new JButton("Exit");
      exitButton.setPreferredSize(new Dimension(80, 25));
      exitButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmExit();
        }
      });
      add(exitButton);

      saveButton = new JButton("Save");
      saveButton.setMargin(new Insets(0, 0, 0, 0));
      saveButton.setPreferredSize(new Dimension(80, 25));
      saveButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmSave();
        }
      });
      add(saveButton);
    
      restoreButton = new JButton("Restore");
      restoreButton.setMargin(new Insets(0, 0, 0, 0));
      restoreButton.setPreferredSize(new Dimension(80, 25));
      restoreButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          main.confirmRestore();
        }
      });
      add(restoreButton);
    
      add(new JLabel("  "));
    
      // undo action with Ctrl+Z or undo button
      Action undoAction = new AbstractAction(){
        @Override public void actionPerformed(ActionEvent e){
          if (hist.undoLeft()){
            boolean endGroup = false;
            while (!endGroup){
              endGroup = hist.undo();
              if (hist.currentTopMapIndex() != tileMap.currentTopMapIndex())
                main.setTopMap(hist.currentTopMapIndex());
              tileMap.set(hist.getTile(), hist.getLayer(), 
                          hist.getI(), hist.getJ());
              viewPanel.ensureInView(hist.getI(), hist.getJ());
            }
          }
        }
      };
      
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
           put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
           InputEvent.CTRL_DOWN_MASK), "undoActionKey");
      getActionMap().put("undoActionKey", undoAction);

      undoButton = new JButton("Undo");
      undoButton.setPreferredSize(new Dimension(70, 25));
      undoButton.addActionListener(undoAction);
      add(undoButton);
    
      // redo action with Ctrl+Shift+Z or redo button
      Action redoAction = new AbstractAction(){
        @Override public void actionPerformed(ActionEvent e){
          if (hist.redoLeft()){
            boolean endGroup = false;
            while (!endGroup){
              endGroup = hist.redo();
              if (hist.currentTopMapIndex() != tileMap.currentTopMapIndex())
                main.setTopMap(hist.currentTopMapIndex());
              tileMap.set(hist.getTile(), hist.getLayer(), 
                          hist.getI(), hist.getJ());
              viewPanel.ensureInView(hist.getI(), hist.getJ());
            }
          }
        }
      };
      
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
           put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
           InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), 
           "redoActionKey");
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).
           put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
           InputEvent.CTRL_DOWN_MASK), "redoActionKey");
      getActionMap().put("redoActionKey", redoAction);
      
      redoButton = new JButton("Redo");
      redoButton.setPreferredSize(new Dimension(70, 25));
      redoButton.addActionListener(redoAction);
      add(redoButton);
      
      add(new JLabel("  "));
      
      gridButton = new JButton("Show Grid");
      gridButton.setMargin(new Insets(0, 0, 0, 0));
      gridButton.setPreferredSize(new Dimension(90, 25));
      gridButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          viewPanel.toggleGrid();
        }
      });
      add(gridButton);
    }
  }
  
  class TilesPanel extends JPanel{
    boolean shiftDown;
    int nx, ny, compAdd;
    
    TilesPanel(boolean isComposite){
      compAdd = (isComposite) ? 4 : 0;
      
      addKeyListener(new KeyAdapter(){
        @Override public void keyPressed(KeyEvent e){
          if (e.getKeyCode() == KeyEvent.VK_SHIFT){ 
            shiftDown = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
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
              int oldID = selectionTiles[0][selectedLayer]/4*4; 
              viewPanel.wSel = 1; viewPanel.hSel = 1;
              selectedLayer = currentLayer;
              selectionTiles[0][selectedLayer] = (i + j*nx)*8 + compAdd;
              int newID = selectionTiles[0][selectedLayer]/4*4; 
              if (shiftDown && (oldID != newID)){ 
                if (main.confirm("Swap usage of tiles:  " + oldID/8 + 
                     (((oldID&4)==4) ? " C" : " B") + "  and  " + newID/8 +
                     (((newID&4)==4) ? " C" : " B") + "  everywhere?")){
                  changed = true;
                  // swap tiles including hflip and vflip variations
                  tileMap.swapTiles(oldID, newID);
                  tileMap.swapTiles(oldID+1, newID+1);
                  tileMap.swapTiles(oldID+2, newID+2);
                  tileMap.swapTiles(oldID+3, newID+3);
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
      this.nx = nx; this.ny = ny; 
      setPreferredSize(new Dimension(nx*main.tileW, ny*main.tileH));
      revalidate();
    }
    
    @Override public void paintComponent(Graphics g){
      Point pos = getMousePosition(); 
      int ID = compAdd;
      for (int j = 0; j < ny; j++)
        for (int i = 0; i < nx; i++){
          int dx1 = i*main.tileW, dy1 = j*main.tileH;
          g.setColor(((i+j)%2 == 0) ? main.bg1 : main.bg2);
          g.fillRect(dx1, dy1, main.tileW, main.tileH);
          tileMap.drawTile(g, ID, dx1, dy1);
          if (showSolid.isSelected()) 
            tileMap.drawSolid(g, tileMap.getSolid(ID), main.solidColor,
                 i*main.tileW, j*main.tileH, main.tileW, main.tileH);
          ID+= 8;
        }
      g.setColor(main.selectColor);
      if ((pos != null) && (pos.x < nx*main.tileW) && (pos.y < ny*main.tileH)){
        int i = pos.x/main.tileW, j = pos.y/main.tileH;
        g.drawRect(i*main.tileW-1, j*main.tileH-1, main.tileW+1, main.tileH+1);
      }
      ID = selectionTiles[0][selectedLayer];
      if (compAdd == (ID&4)){
        int i = (ID/8)%nx, j = (ID/8)/nx;
        g.drawRect(i*main.tileW, j*main.tileH, main.tileW-1, main.tileH-1);
      }
      tileInfo.setText(
           String.format("TILE:%5d " + (((ID&4)==0) ? "B" : "C"), ID/8));
    }
  }
  
  class MapPanel extends JPanel{
    final int w0 = 300, h0 = 300;
    int i0, j0, w = w0, h = h0, vw, vh;
    
    MapPanel(){ 
      addMouseListener(new MouseAdapter(){
        @Override public void mousePressed(MouseEvent e){
          if (e.getButton() == MouseEvent.BUTTON1){
            viewPanel.x0 = (i0 + e.getX() - vw/2)*viewPanel.tw; 
            viewPanel.y0 = (j0 + e.getY() - vh/2)*viewPanel.th;
          }
        }
      });
    }
    
    void update(){
      vw = (viewPanel.w + viewPanel.tw/2)/viewPanel.tw;
      vh = (viewPanel.h + viewPanel.th/2)/viewPanel.th;
      i0 = viewPanel.x0/viewPanel.tw + (vw - w)/2;
      j0 = viewPanel.y0/viewPanel.th + (vh - h)/2;
      w = (mapPanel.getWidth() > 480) ? 480 : mapPanel.getWidth();
      h = (mapPanel.getHeight() > 480) ? 480 : mapPanel.getHeight();
    }
    
    @Override public void paintComponent(Graphics g){
      g.setColor(Color.black);
      g.fillRect(0, 0, w, h);
      tileMap.setLayerRange(0, tileMap.totalLayers()-1);
      tileMap.drawAvgColors(i0, j0, w, h, mapImage, 0, 0);
      g.drawImage(mapImage, 0, 0, null);
      g.setColor(main.selectColor);
      Point pos = getMousePosition(); 
      if (pos != null){
        g.drawRect(pos.x - vw/2 - 1, pos.y - vh/2 - 1, vw + 1, vh + 1);
      }
      g.drawRect(viewPanel.x0/viewPanel.tw - i0, 
                 viewPanel.y0/viewPanel.th - j0, vw - 1, vh - 1);
    }
  }
  
  class LayersPanel extends JPanel{
    JCheckBox selectAllBox;
    JPanel innerPanel;
    JRadioButton[] currentLayerButton;
    ButtonGroup visibleRadioGroup;
    JCheckBox[] visibleLayerBox;
    JLabel[] mouseLayerLabel, selectionLayerLabel;
    
    LayersPanel(){
      setLayout(new BorderLayout());
      setMinimumSize(new Dimension(20, 20));
      
      selectAllBox = new JCheckBox("Draw All Selected Layers");
      selectAllBox.setHorizontalAlignment(SwingConstants.CENTER);
      selectAllBox.setPreferredSize(new Dimension(60, 30));
      selectAllBox.setSelected(true);
      add(selectAllBox, BorderLayout.NORTH);
      
      innerPanel = new JPanel();
      innerPanel.setLayout(new GridLayout(0, 4));
      
      JScrollPane splitPane = new JScrollPane(innerPanel);
      add(splitPane, BorderLayout.CENTER);
    }
    
    final void initLayers(){
      int nLayers = tileMap.totalLayers();
      selectionTiles = new int[maxSelectionSize*maxSelectionSize][nLayers];
      if (selectedLayer >= nLayers) selectedLayer = 0;
      viewPanel.wSel = 1; viewPanel.hSel = 1;
           
      if (isVisible != null) 
        if (isVisible.length == nLayers) return;
      
      currentLayerButton = new JRadioButton[nLayers];
      visibleRadioGroup = new ButtonGroup();
      visibleLayerBox = new JCheckBox[nLayers];
      mouseLayerLabel = new JLabel[nLayers];
      selectionLayerLabel = new JLabel[nLayers];
    
      innerPanel.removeAll();
      innerPanel.add(new JLabel("Current", JLabel.CENTER));
      innerPanel.add(new JLabel("Visible", JLabel.CENTER));
      innerPanel.add(new JLabel("Selected", JLabel.CENTER));
      innerPanel.add(new JLabel("At Mouse", JLabel.CENTER));
      
      for (int i = 0; i < nLayers; i++){
        currentLayerButton[i] = new JRadioButton("" + i);
        currentLayerButton[i].setHorizontalAlignment(SwingConstants.CENTER);
        innerPanel.add(currentLayerButton[i]);
        visibleRadioGroup.add(currentLayerButton[i]);
        
        visibleLayerBox[i] = new JCheckBox("" + i);
        visibleLayerBox[i].setHorizontalAlignment(SwingConstants.CENTER);
        visibleLayerBox[i].setSelected(true);
        innerPanel.add(visibleLayerBox[i]);
        
        selectionLayerLabel[i] = new JLabel("");
        selectionLayerLabel[i].setHorizontalAlignment(SwingConstants.CENTER);
        innerPanel.add(selectionLayerLabel[i]);
        
        mouseLayerLabel[i] = new JLabel("");
        mouseLayerLabel[i].setHorizontalAlignment(SwingConstants.CENTER);
        innerPanel.add(mouseLayerLabel[i]);
      }
      
      currentLayer = 0;
      currentLayerButton[currentLayer].setSelected(true);
      isVisible = new boolean[nLayers];
    }
    
    void update(){
      for (int i = 0; i < tileMap.totalLayers(); i++){
        if (currentLayerButton[i].isSelected()) currentLayer = i;
        isVisible[i] = visibleLayerBox[i].isSelected();
        selectionLayerLabel[i].setForeground( 
             (i == selectedLayer) ? Color.red : Color.black);
        int id = selectionTiles[0][i];
        selectionLayerLabel[i].setText((id/8) + (((id&4) == 4) ? " C" : " B") +
             " " + (((id&1) == 1) ? "H" : "") + (((id&2) == 2) ? "V" : ""));
      }
    }
  }
  
  class MapSelectPanel extends JPanel{
    JPanel mapListPanel, optionsPanel;
    JButton addButton, removeButton, insertButton;
            
    JList mapList;
    ArrayList<String> mapArrayList;
    
    MapSelectPanel(){
      setLayout(new BorderLayout());
      
      mapArrayList = new ArrayList<String>();
      mapList = new JList();
      mapList.setLayoutOrientation(JList.VERTICAL);
      mapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      mapList.setFont(main.monoFont);
      JScrollPane mapListScrollPane = new JScrollPane(mapList);
      add(mapListScrollPane, BorderLayout.CENTER);
      
      optionsPanel = new JPanel();
      optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
      
      optionsPanel.add(Box.createRigidArea(new Dimension(120, 10)));
      
      addButton = new JButton("add map");
      addButton.setAlignmentX(0.5f);
      addButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          tileMap.addTopMap();
          main.setTopMap(tileMap.totalTopMaps()-1);
          changed = true;
        }
      });
      optionsPanel.add(addButton);
      
      optionsPanel.add(Box.createRigidArea(new Dimension(120, 10)));
      
      removeButton = new JButton("remove map");
      removeButton.setAlignmentX(0.5f);
      removeButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          int i = mapList.getSelectedIndex();
          tileMap.removeTopMap(i);
          if (i > 0) i--;
          main.setTopMap(i);
          changed = true;
        }
      });
      optionsPanel.add(removeButton);
      
      optionsPanel.add(Box.createRigidArea(new Dimension(120, 10)));
      
      insertButton = new JButton("insert map");
      insertButton.setAlignmentX(0.5f);
      insertButton.addActionListener(new ActionListener(){
        @Override public void actionPerformed(ActionEvent e){
          int i = mapList.getSelectedIndex();
          tileMap.insertTopMap(i);
          main.setTopMap(i);
          changed = true;
        }
      });
      optionsPanel.add(insertButton);
      
      add(optionsPanel, BorderLayout.WEST);
      
      updateMapList();
    }
    
    final void updateMapList(){
      if (mapArrayList.size() != tileMap.totalTopMaps()){
        mapArrayList.clear();
        for (int i = 0; i < tileMap.totalTopMaps(); i++){
          mapArrayList.add(String.format("  MAP%3d", i));
        }
        mapList.setListData(mapArrayList.toArray());
      }
      mapList.setSelectedIndex(tileMap.currentTopMapIndex());
    }
    
    void update(){
      if (tileMap.currentTopMapIndex() != mapList.getSelectedIndex()){ 
        main.setTopMap(mapList.getSelectedIndex());
      }
    }
    
  }
  
  class ViewPanel extends JPanel{
    Point pos = null;
    int x0 = 0, y0 = 0, w, h, tileID, tw = 16, th = 16, gridSize = 0;
    double scale = 1.0;
    int iSel, jSel, i0Sel, j0Sel, wSel = 1, hSel = 1;
    boolean lmbDown = false, mmbDown = false, rmbDown = false, 
            spaceDown = false;
    int dragMouseX0, dragMouseY0, dragX0, dragY0;
    
    ViewPanel(){
      setFocusTraversalKeysEnabled(false);
      
      addKeyListener(new KeyAdapter(){
        @Override public void keyPressed(KeyEvent e){
          int key = e.getKeyCode();
          if (key == KeyEvent.VK_SPACE){ 
            if (spaceDown) return;
            spaceDown = true;
            if (mmbDown) return;
            Point pos = getMousePosition();
            if (pos == null) return;
            dragX0 = x0; dragY0 = y0;
            dragMouseX0 = pos.x; dragMouseY0 = pos.y;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
          }else if (key == KeyEvent.VK_UP){
            y0-= 20;
          }else if (key == KeyEvent.VK_DOWN){
            y0+= 20;
          }else if (key == KeyEvent.VK_LEFT){
            x0-= 20;
          }else if (key == KeyEvent.VK_RIGHT){
            x0+= 20;
          }else if (key == KeyEvent.VK_HOME){
            setScale(1.0, true);
          }else if (key == KeyEvent.VK_PAGE_UP){
            setScale((double)(tw+1)/main.tileW, true);
          }else if (key == KeyEvent.VK_PAGE_DOWN){
            if (tw > 0) setScale((double)(tw-1)/main.tileW, true);
          // flip all selection tiles horizontally
          }else if ((key == KeyEvent.VK_H) || (key == KeyEvent.VK_C)){
            if (layersPanel.selectAllBox.isSelected()){  // all layers
              // flip each tile
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel; ii++)
                  for (int layer = 0; layer < tileMap.totalLayers(); layer++)
                    selectionTiles[ii + jj*wSel][layer]^= 1;
              // flip selection
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel/2; ii++){
                  int[] tmp = selectionTiles[ii + jj*wSel];
                  selectionTiles[ii + jj*wSel] = 
                       selectionTiles[wSel-1 - ii + jj*wSel];
                  selectionTiles[wSel-1 - ii + jj*wSel] = tmp;
                }
            }else{ // only selected layer
              // flip each tile
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel; ii++)
                  selectionTiles[ii + jj*wSel][selectedLayer]^= 1;
              // flip selection
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel/2; ii++){
                  int tmp = selectionTiles[ii + jj*wSel][selectedLayer];
                  selectionTiles[ii + jj*wSel][selectedLayer] = 
                       selectionTiles[wSel-1 - ii + jj*wSel][selectedLayer];
                  selectionTiles[wSel-1 - ii + jj*wSel][selectedLayer] = tmp;
                }
            }
          // flip all selection tiles vertically
          }else if (key == KeyEvent.VK_V){
            if (layersPanel.selectAllBox.isSelected()){  // all layers
              // flip each tile
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel; ii++)
                  for (int layer = 0; layer < tileMap.totalLayers(); layer++)
                    selectionTiles[ii + jj*wSel][layer]^= 2;
              // flip selection
              for (int ii = 0; ii < wSel; ii++)
                for (int jj = 0; jj < hSel/2; jj++){
                  int[] tmp = selectionTiles[ii + jj*wSel];
                  selectionTiles[ii + jj*wSel] = 
                       selectionTiles[ii + (hSel-1 - jj)*wSel];
                  selectionTiles[ii + (hSel-1 - jj)*wSel] = tmp;
                }
            }else{ // only selected layer
              // flip each tile
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel; ii++)
                  selectionTiles[ii + jj*wSel][selectedLayer]^= 2;
              // flip selection
              for (int ii = 0; ii < wSel; ii++)
                for (int jj = 0; jj < hSel/2; jj++){
                  int tmp = selectionTiles[ii + jj*wSel][selectedLayer];
                  selectionTiles[ii + jj*wSel][selectedLayer] = 
                       selectionTiles[ii + (hSel-1 - jj)*wSel][selectedLayer];
                  selectionTiles[ii + (hSel-1 - jj)*wSel][selectedLayer] = tmp;
                }
            }
          }
        }
        @Override public void keyReleased(KeyEvent e){
          if (e.getKeyCode() == KeyEvent.VK_SPACE){ 
            spaceDown = false;
            if (!mmbDown)
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }
        }
      });
      
      addMouseListener(new MouseAdapter(){
        @Override public void mousePressed(MouseEvent e){
          if (e.getButton() == MouseEvent.BUTTON1){ 
            lmbDown = true;
          }else if (e.getButton() == MouseEvent.BUTTON2){
            mmbDown = true; 
            if (spaceDown) return;
            dragX0 = x0; dragY0 = y0;
            dragMouseX0 = e.getX(); dragMouseY0 = e.getY();
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
          }else if (e.getButton() == MouseEvent.BUTTON3){
            rmbDown = true;
            int x = e.getX() + x0, y = e.getY() + y0,
            i = (x < 0) ? (x+1)/tw-1 : x/tw, 
            j = (y < 0) ? (y+1)/th-1 : y/th;
            selectionTiles[0][selectedLayer] = tileID;
            scrollToSelectedTile();
            i0Sel = iSel = i; j0Sel = jSel = j;
            wSel = 1; hSel = 1;
          }
        }
        @Override public void mouseReleased(MouseEvent e){
          if (e.getButton() == MouseEvent.BUTTON1) 
            lmbDown = false;
          else if (e.getButton() == MouseEvent.BUTTON2){ 
            mmbDown = false;
            if (!spaceDown)
              setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          }else if (e.getButton() == MouseEvent.BUTTON3){
            rmbDown = false;
            selectedLayer = currentLayer;
            for (int jj = 0; jj < hSel; jj++)
              for (int ii = 0; ii < wSel; ii++)
                selectionTiles[ii + jj*wSel] = 
                     tileMap.get(iSel+ii, jSel+jj).clone();
          }
        }
        @Override public void mouseEntered(MouseEvent e){
          requestFocusInWindow();
        }
        @Override public void mouseExited(MouseEvent e){
          spaceDown = false;
        }
      });
      
      addMouseWheelListener(new MouseAdapter(){
        @Override public void mouseWheelMoved(MouseWheelEvent e){
          int i = tw-e.getWheelRotation();
          setScale((double)((i > 0) ? i : 1)/main.tileW, true);
        }
      });
    }
    
    void setView(int x, int y, double newScale){ 
      x0 = x; y0 = y; setScale(1.0, false); 
    }
    
    void setScale(double newScale, boolean scaleCentered){
      int tw0 = tw, th0 = th;
      scale = newScale;
      tw = (int)(main.tileW*scale + 0.5);
      th = (int)(main.tileH*scale + 0.5);
      if (tw < 1) tw = 1; if (th < 1) th = 1;
      if (scaleCentered){
        x0 = (int)Math.round((x0 + w/2.0)*tw/tw0 - w/2.0);
        y0 = (int)Math.round((y0 + h/2.0)*th/th0 - h/2.0);
      }
    }
    
    void ensureInView(int i, int j){
      int x = i*tw, y = j*th;
      if ((x < x0) || (x+tw >= x0+w)) x0 = x + tw/2 - w/2;
      if ((y < y0) || (y+th >= y0+h)) y0 = y + th/2 - h/2;
    }
    
    void update(){
      pos = getMousePosition(); 
      w = getWidth(); h = getHeight();
      
      if (pos == null){
        tileID = -1;
      }else{
        int x = pos.x + x0, y = pos.y + y0,
            i = (x < 0) ? (x+1)/tw-1 : x/tw, 
            j = (y < 0) ? (y+1)/th-1 : y/th;
        tileID = tileMap.get(i, j)[currentLayer];
        // scroll the map
        if (mmbDown || spaceDown){
          x0 = dragX0 + dragMouseX0-pos.x;
          y0 = dragY0 + dragMouseY0-pos.y;
        // tile/area selection
        }else if (rmbDown){
          if (i < i0Sel){ 
            if (i0Sel - i + 1 > maxSelectionSize) 
              i0Sel = i + maxSelectionSize - 1;
            iSel = i; wSel = i0Sel - i + 1; 
          }else if (i > i0Sel){
            if (i - i0Sel + 1 > maxSelectionSize) 
              i0Sel = i - maxSelectionSize + 1;
            iSel = i0Sel; wSel = i - i0Sel + 1; 
          }else{
            iSel = i; wSel = 1;
          }  
          if (j < j0Sel){ 
            if (j0Sel - j + 1 > maxSelectionSize) 
              j0Sel = j + maxSelectionSize - 1;
            jSel = j; hSel = j0Sel - j + 1; 
          }else if (j > j0Sel){
            if (j - j0Sel + 1 > maxSelectionSize) 
              j0Sel = j - maxSelectionSize + 1;
            jSel = j0Sel; hSel = j - j0Sel + 1; 
          }else{
            jSel = j; hSel = 1;
          }  
        }else{
          // draw selection
          if (lmbDown){
            hist.setTopMap(tileMap.currentTopMapIndex());
            hist.startGroup();
            if (layersPanel.selectAllBox.isSelected()){
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel; ii++){
                  int[] oldLayer = tileMap.get(iSel+ii, jSel+jj),
                        newLayer = selectionTiles[ii + jj*wSel];
                  for (int iLayer=0; iLayer < tileMap.totalLayers(); iLayer++){
                    if (oldLayer[iLayer] != newLayer[iLayer]){
                      hist.add(oldLayer[iLayer], newLayer[iLayer], 
                               iLayer, iSel+ii, jSel+jj);
                    }
                  }
                  tileMap.set(newLayer, iSel+ii, jSel+jj);
                }
            }else{  // single layer
              for (int jj = 0; jj < hSel; jj++)
                for (int ii = 0; ii < wSel; ii++){
                  int oldTile = tileMap.get(iSel+ii, jSel+jj)[currentLayer],
                      newTile = selectionTiles[ii + jj*wSel][selectedLayer];
                  if (oldTile != newTile){
                    hist.add(oldTile, newTile, currentLayer, iSel+ii, jSel+jj);
                    tileMap.set(newTile, currentLayer, iSel+ii, jSel+jj);
                  }
                }
            }
            hist.endGroup();
          }
          iSel = i - (wSel-1)/2; jSel = j - (hSel-1)/2;
        }
      }
    }
    
    @Override public void paintComponent(Graphics g){
      g.setColor(Color.black);
      g.fillRect(0, 0, w, h);
      int i0 = (x0 < 0) ? (x0+1)/tw-1 : x0/tw, 
          j0 = (y0 < 0) ? (y0+1)/th-1 : y0/th;
      for (int j = j0; j*th < y0+h; j++)
        for (int i = i0; i*tw < x0+w; i++){
          int[] layer = tileMap.get(i, j);
          int x = i*tw - x0, y = j*th - y0;
          for (int iLayer = 0; iLayer < tileMap.totalLayers(); iLayer++){
            if (isVisible[iLayer])
              tileMap.drawTile(g, layer[iLayer], x, y, tw, th);
          }
          if (showSolid.isSelected()) 
            tileMap.drawSolid(g, tileMap.getLayerSolid(layer), 
                              main.solidColor, x, y, tw, th);
        }
      
      if (pos != null){
        // draw tile selection
        int xi0 = i0*tw - x0, yj0 = j0*th - y0,
            i = i0 + (pos.x - xi0)/tw, j = j0 + (pos.y - yj0)/th,
            x = i*tw - x0, y = j*th - y0;
        Graphics2D g2d = (Graphics2D)g;
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 
                                                    (float)main.pulse/256));
        if (!rmbDown){
          if (layersPanel.selectAllBox.isSelected()){
            for (int jj = 0; jj < hSel; jj++)
              for (int ii = 0; ii < wSel; ii++)
                for (int iLayer = 0; iLayer < tileMap.totalLayers(); iLayer++)
                  tileMap.drawTile(g, selectionTiles[ii + jj*wSel][iLayer], 
                    xi0 + (iSel+ii-i0)*tw, yj0 + (jSel+jj-j0)*th, tw, th);
          }else{
            for (int jj = 0; jj < hSel; jj++)
              for (int ii = 0; ii < wSel; ii++)
                tileMap.drawTile(g, selectionTiles[ii + jj*wSel][selectedLayer],
                  xi0 + (iSel+ii-i0)*tw, yj0 + (jSel+jj-j0)*th, tw, th);
          }
        }
        g2d.setComposite(oldComposite);
        // draw view grid if enabled
        drawGrid(g);
        // draw select box
        g.setColor(main.selectColor);
        g.drawRect(x-1, y-1, tw+1, th+1);
        if ((wSel > 1) || (hSel > 1))
          g.drawRect(xi0 + tw*(iSel-i0) - 1, yj0 + th*(jSel-j0) - 1, 
                     tw*wSel + 1, th*hSel + 1);
        // draw coordinates and scale
        g.setColor(new Color(0, 0, 0, 128));
        g.fillRect(5, 5, 94, 40);
        g.setColor(Color.white);
        centerString(g, String.format("(%d, %d)", i, j), 52, 20);
        centerString(g, String.format("Scale:%6.2f",  scale), 52, 38);
        // update layers panel "at mouse"
        int[] layer = tileMap.get(i, j);
        for (int iLayer = 0; iLayer < tileMap.totalLayers(); iLayer++){
          int id = layer[iLayer];
          layersPanel.mouseLayerLabel[iLayer].setText(
               (id/8) + (((id&4) == 4) ? " C" : " B") + " " + 
               (((id&1) == 1) ? "H" : "") + (((id&2) == 2) ? "V" : ""));
        }
      }else{
        // draw view grid if enabled
        drawGrid(g);  
        // update layers panel "at mouse"
        for (int iLayer = 0; iLayer < tileMap.totalLayers(); iLayer++)
          layersPanel.mouseLayerLabel[iLayer].setText("");
      }
      
    }
    
    void centerString(Graphics g, String s, int x, int y){
      g.drawString(s, x - g.getFontMetrics().stringWidth(s)/2, y);
    }

    void toggleGrid(){
      gridSize = (gridSize == 0) ? 1 : (gridSize == 1) ? 4 : 
                 (gridSize == 4) ? 10 : (gridSize == 10) ? 16 : 0;
    }
    
    void drawGrid(Graphics g){
      gridButton.setText((gridSize==0) ? "Show Grid" : 
           ("Grid: " + gridSize + "x" + gridSize));
      if (gridSize==0) return;
      int gridW = gridSize*tw, gridH = gridSize*th;
      if ((gridW < 8) || (gridH < 8)) return;
      g.setColor(Color.magenta);
      int i = (x0 < 0) ? (x0 + 1)/gridW - 1 : x0/gridW, 
          j = (y0 < 0) ? (y0 + 1)/gridH - 1 : y0/gridH;
      for (int x = i*gridW-x0-1; x < w; x+= gridW) g.fillRect(x, 0, 2, h);
      for (int y = j*gridH-y0-1; y < h; y+= gridH) g.fillRect(0, y, w, 2);
    }
  }
  
  class History{
    final int SIZE = 50000*5; // undo up to SIZE/5 tile changes
    int[] array = new int[SIZE];
    int i0 = 0, iEnd = 0, i = 0, currentTopMap = 0,
        tile, iLayer, tileI, tileJ, iTopMap;  
    boolean i0Moved = false, first;
    
    void reset(){ i0 = iEnd = i = 0; i0Moved = false; }
    boolean isChanged(){ return undoLeft() || i0Moved; }
    boolean undoLeft(){ return i0 != i; }
    boolean redoLeft(){ return iEnd != i; }
    
    // get info for last call to undo or redo
    int getTile(){ return tile; }
    int getLayer(){ return iLayer; }
    int currentTopMapIndex(){ return iTopMap; }
    int getI(){ return tileI; }
    int getJ(){ return tileJ; }
    
    void setTopMap(int i){ currentTopMap = i; }
    
    void startGroup(){ first = true; }
    void endGroup(){
      int iPrev = (i==0) ? SIZE-5 : i-5; 
      array[iPrev+4]|= 2; 
    }
    
    void add(int t0, int tEnd, int iLayer, int x, int y){
      array[i] = (t0<<16) + tEnd; array[i+1] = (iLayer<<16) + currentTopMap;
      array[i+2] = x; array[i+3] = y; 
      if (first){ first = false; array[i+4] = 1; } else array[i+4] = 0;
      
      iEnd = i = (i+5)%SIZE;
      if (iEnd == i0){ 
        i0Moved = true;
        if (array[i0] < 0){
          i0 = (i0+5)%SIZE;
          while (array[i0] >= 0) i0 = (i0+5)%SIZE;
        }
        i0 = (i0+5)%SIZE;
      }
    }
    
    // return true if first in group
    boolean undo(){
      if (i == i0) return true;
      i = (i==0) ? SIZE-5 : i-5;
      tile = (array[i] >>> 16);
      iLayer = (array[i+1] >>> 16); iTopMap = (array[i+1] & 0xffff);
      tileI = array[i+2]; tileJ = array[i+3];
      return ((array[i+4]&1) == 1);
    }
    
    // return true if last in group
    boolean redo(){
      if (i == iEnd) return true;
      tile = (array[i] & 0xffff);
      iLayer = (array[i+1] >>> 16); iTopMap = (array[i+1] & 0xffff);
      tileI = array[i+2]; tileJ = array[i+3];
      boolean end = ((array[i+4]&2) == 2);
      i = (i+5)%SIZE;
      return end;
    }
  }
  
}

