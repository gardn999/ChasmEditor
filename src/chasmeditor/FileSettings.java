package chasmeditor;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.NumberFormat;
import javax.swing.*;

public class FileSettings extends JPanel{
  Main main;
  JLabel mapFileLabel, tileImageLabel;
  JFormattedTextField tileWField, tileHField, layerField;            
  JButton newMapButton, saveAsButton, loadMapButton,
          exitButton, saveButton, restoreButton,
          addLayerButton, removeLayerButton;
  JLabel totalLayersLabel, layerLabel;
  boolean changed = false;
  
  FileSettings(Main main){
    this.main = main;
    initPanel();
  }

  final void initPanel(){
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    
    add(Box.createVerticalGlue());
    add(Box.createVerticalGlue());
      
    // map file selection
    JLabel mapLabel = new JLabel("SELECT MAP FILE");
    mapLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    add(mapLabel);
    add(Box.createVerticalGlue());
    
    JPanel mapFilePanel = new JPanel();
    mapFilePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
    
    newMapButton = new JButton("New Map...");
    newMapButton.setFocusable(false);
    newMapButton.setPreferredSize(new Dimension(100, 25));
    newMapButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        if (main.changeMade() && !main.mapFilePath.equals("")) 
          main.confirmSave();
        String path = selectFile("Choose name for new map file.");
        if (path != null){ 
          File newMapFile = new File(path);
          if (newMapFile.exists())
            if (!main.confirm("Overwrite file: \"" + path + "\"?"))
              return;
          main.mapFilePath = path;
          main.initMap();
          main.saveMap();
          main.initPanels();
          main.mapEditor.viewPanel.setView(0, 0, 1.0);
        }
      }
    });
    mapFilePanel.add(newMapButton);

    saveAsButton = new JButton("Save As...");
    saveAsButton.setFocusable(false);
    saveAsButton.setPreferredSize(new Dimension(100, 25));
    saveAsButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        main.doSaveAsDialog();
      }
    });
    mapFilePanel.add(saveAsButton);
    
    loadMapButton = new JButton("Load Map...");
    loadMapButton.setFocusable(false);
    loadMapButton.setPreferredSize(new Dimension(100, 25));
    loadMapButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        String path = selectFile("Select map file to load.");
        if (path != null){ 
          File mapFile = new File(path);
          if (mapFile.exists()){ 
            if (main.changeMade()) main.confirmSave();
            main.loadMap(path);
            main.initPanels();
            main.mapEditor.viewPanel.setView(0, 0, 1.0);
          }
        }
      }
    });
    mapFilePanel.add(loadMapButton);

    add(mapFilePanel);
    
    JPanel mapFilePanel2 = new JPanel();
    mapFilePanel2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
    
    exitButton = new JButton("Exit");
    exitButton.setPreferredSize(new Dimension(100, 25));
    exitButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        main.confirmExit();
      }
    });
    mapFilePanel2.add(exitButton);

    saveButton = new JButton("Save");
    saveButton.setMargin(new Insets(0, 0, 0, 0));
    saveButton.setPreferredSize(new Dimension(100, 25));
    saveButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        main.confirmSave();
      }
    });
    mapFilePanel2.add(saveButton);
    
    restoreButton = new JButton("Restore");
    restoreButton.setMargin(new Insets(0, 0, 0, 0));
    restoreButton.setPreferredSize(new Dimension(100, 25));
    restoreButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        main.confirmRestore();
      }
    });
    mapFilePanel2.add(restoreButton);

    add(mapFilePanel2);
    
    mapFileLabel = new JLabel();
    mapFileLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    add(mapFileLabel);
    
    add(Box.createVerticalGlue());
    add(new JSeparator(JSeparator.HORIZONTAL));
    
    // layers
    totalLayersLabel = new JLabel("TOTAL MAP LAYERS:");
    totalLayersLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    add(totalLayersLabel);
    
    add(Box.createVerticalGlue());
    
    JPanel layerPanel = new JPanel();
    
    layerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
    addLayerButton = new JButton("Add Layer");
    addLayerButton.setPreferredSize(new Dimension(120, 25));
    addLayerButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
    addLayerButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        addLayer();
      }
    });
    layerPanel.add(addLayerButton);
    
    removeLayerButton = new JButton("Remove Layer");
    removeLayerButton.setPreferredSize(new Dimension(120, 25));
    removeLayerButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
    removeLayerButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        removeLayer();
      }
    });
    layerPanel.add(removeLayerButton);
    
    layerLabel = new JLabel("   Index to Add / Remove Layer at:");
    layerPanel.add(layerLabel);
    
    layerField = new JFormattedTextField(NumberFormat.getIntegerInstance());
    layerField.setPreferredSize(new Dimension(40, 25));
    layerField.setValue(main.tileMap.totalLayers());
    layerPanel.add(layerField);
    
    add(layerPanel);
    
    add(new JSeparator(JSeparator.HORIZONTAL));
    
    JPanel tilePanel = new JPanel();
    tilePanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
    tilePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
    
    // tile image selection
    JLabel imageLabel = new JLabel("TILE IMAGE SETTINGS");
    imageLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    add(imageLabel);
    add(Box.createVerticalGlue());
    
    JButton selectImageButton = new JButton("Select Tile Image File");
    selectImageButton.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    selectImageButton.setFocusable(false);
    selectImageButton.setPreferredSize(new Dimension(180, 25));
    selectImageButton.addActionListener(new ActionListener(){
      @Override public void actionPerformed(ActionEvent e){
        String path = selectFile("Select tile image to load.");
        if (path != null) main.loadTileImage(path, getTileW(), getTileH());
      }
    });
    tilePanel.add(selectImageButton);
    
    // tile width and height
    JLabel tileWLabel = new JLabel("Tile Width:");
    tileWLabel.setHorizontalAlignment(JLabel.RIGHT);
    tileWLabel.setPreferredSize(new Dimension(80, 25));
    tilePanel.add(tileWLabel);
    
    tileWField = new JFormattedTextField(NumberFormat.getIntegerInstance());
    tileWField.setPreferredSize(new Dimension(50, 25));
    tilePanel.add(tileWField);

    JLabel tileHLabel = new JLabel("Tile Height:");
    tileHLabel.setHorizontalAlignment(JLabel.RIGHT);
    tileHLabel.setPreferredSize(new Dimension(80, 25));
    tilePanel.add(tileHLabel);
    
    tileHField = new JFormattedTextField(NumberFormat.getIntegerInstance());
    tileHField.setPreferredSize(new Dimension(50, 25));
    tilePanel.add(tileHField);
    
    add(tilePanel);
    
    tileImageLabel = new JLabel();
    tileImageLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    add(tileImageLabel);
    
    add(Box.createVerticalGlue());
    add(Box.createVerticalGlue());
    add(Box.createVerticalGlue());
  }
  
  void update(){
    mapFileLabel.setText( (main.mapFilePath.equals("")) ? 
         "(no file selected)" : main.mapFilePath);
    tileImageLabel.setText( (main.tileImagePath.equals("")) ? 
         "(no image selected)" : main.tileImagePath);
    saveButton.setEnabled(main.changeMade());
    restoreButton.setEnabled(main.changeMade());
    totalLayersLabel.setText("TOTAL MAP LAYERS:   " + 
         main.tileMap.totalLayers());
    setLayerFieldBounds();
    if ((main.tileW != getTileW()) || (main.tileH != getTileH()))
      main.loadTileImage(main.tileImagePath, getTileW(), getTileH());
  }
  
  void addLayer(){
    int nLayers = main.tileMap.totalLayers();
    if (nLayers >= main.MAX_LAYERS) return;
    setLayerFieldBounds();
    int n = ((Number)layerField.getValue()).intValue();
    main.tileMap.insertLayer(n);
    if (main.tileMap.totalLayers() != nLayers){ 
      layerField.setValue(n+1);
      changed = true;
      main.mapEditor.layersPanel.initLayers();
    }
  }
  
  void removeLayer(){
    setLayerFieldBounds();
    int n = ((Number)layerField.getValue()).intValue(),
        nLayers = main.tileMap.totalLayers();
    if (n >= nLayers) n = nLayers-1;
    main.tileMap.removeLayer(n);
    if (main.tileMap.totalLayers() != nLayers){ 
      changed = true;
      main.mapEditor.layersPanel.initLayers();
    }
  }
  
  void setLayerFieldBounds(){
    int n = ((Number)layerField.getValue()).intValue();
    if (n > main.tileMap.totalLayers()) 
      layerField.setValue(main.tileMap.totalLayers());
    else if (n < 0) 
      layerField.setValue(0);
  }
  
  String selectFile(String title){
    try{
      JFileChooser jfc = new JFileChooser(new File("."));
      jfc.setDialogTitle(title);
      int returnVal = jfc.showDialog(this, "Select");
      if (returnVal == JFileChooser.APPROVE_OPTION){
        return jfc.getSelectedFile().getPath();
      }else 
        return null;
    }catch(HeadlessException ex){ 
      System.err.println(ex.getMessage()); 
      return null;
    }
  }
    
  int getTileW(){ return ((Number)tileWField.getValue()).intValue(); }
  int getTileH(){ return ((Number)tileHField.getValue()).intValue(); }
}

