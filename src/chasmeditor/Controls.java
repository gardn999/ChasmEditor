package chasmeditor;

import java.awt.*;
import javax.swing.*;

public class Controls extends JPanel{
  Main main;
  JPanel leftPanel, rightPanel, currentPanel;
  
  Controls(Main main){
    this.main = main;
    
    // left text
    leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    currentPanel = leftPanel;
    addGlue();
    
    addLabel("(LMB/RMB = Left/Right Mouse Button)", 20);
    
    addSep(0);
    addLabel("Map Editor - Panel Selection", 0);
    addSep(8);
    addLabel("BaseTile/Comptile Panels: W/R", 8);
    addLabel("     Prev/Next Map Panel: S/F", 20);

    addSep(0);
    addLabel("Map Editor - Map Edit Panel(Right Panel)", 0);
    addSep(8);
    addLabel("Create Map Selection to Draw With:    ", 0);
    addLabel("    Drag Mouse With RMB               ", 8);
    addLabel("Draw Selection: LMB                   ", 8);
    addLabel("     Move View: Arrow Keys or Drag    ", 0);
    addLabel("                Mouse With Space or   ", 0);
    addLabel("                Middle Mouse Button   ", 8);
    addLabel("Flip Selection: Horizontal(H or C)    ", 0);
    addLabel("                Vertical(V)           ", 8);
    addLabel("   Zoom In/Out: Page Up/Down or       ", 0);
    addLabel("                Mouse Wheel           ", 8);
    addLabel("   Reset Scale: Home                  ", 8);
    addLabel("          Undo: Ctrl+Z                ", 8);
    addLabel("          Redo: Ctrl+Shift+Z or Ctrl+Y", 0);
    
    addGlue();
    
    // right text
    rightPanel = new JPanel();
    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
    currentPanel = rightPanel;
    addGlue();
    
    addSep(0);
    addLabel("Map Editor - Base/Composite Tiles Panels", 0);
    addSep(5);
    addLabel("             Select a Tile: LMB      ", 5);
    addLabel("Swap Tile Usage Everywhere: Shift+LMB", 15);
    
    addSep(0);
    addLabel("Map Editor - Map View Panel", 0);
    addSep(5);
    addLabel("Select View For Map Edit Panel: LMB", 15);
    
    addSep(0);
    addLabel("Map Editor - Map Layers Panel", 0);
    addSep(5);
    addLabel("Increase/Decrease Current Layer: D/E", 5);
    addLabel("Toggle Draw All Visible Layers: A  ",  5);
    addLabel("Toggle On/Off All Visible Layers: Z  ", 15);
    
    addSep(0);
    addLabel("Tile Editor - BaseTiles Panel", 0);
    addSep(5);
    addLabel("             Select a Tile: LMB      ", 0);
    addLabel("Toggle Set/Clear All Solid: Shift+LMB", 15);
    
    addSep(0);
    addLabel("Tile Editor - BaseTile 4x4 Solid Map", 0);
    addSep(5);
    addLabel("Set/Clear Solid in 4x4 Map: LMB/RMB", 15);
    
    addSep(0);
    addLabel("Tile Editor - CompTiles Panel", 0);
    addSep(5);
    addLabel("Select a Tile: LMB      ", 0);
    addLabel("    Copy Tile: Shift+LMB", 0);
    
    addGlue();
    
    // add to main panel
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(Box.createHorizontalGlue());
    add(Box.createHorizontalGlue());
    add(leftPanel);
    add(Box.createHorizontalGlue());
    add(rightPanel);
    add(Box.createHorizontalGlue());
    add(Box.createHorizontalGlue());
  }
  
  final void addLabel(String text, int pixels){
    JLabel label = new JLabel(text);
    label.setFont(main.monoFont);
    label.setAlignmentX(0.5f);
    currentPanel.add(label);
    currentPanel.add(Box.createRigidArea(new Dimension(20, pixels)));
  }
  
  final void addSep(int pixels){
    JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
    sep.setMaximumSize(new Dimension(320, 20));
    currentPanel.add(sep);
    currentPanel.add(Box.createRigidArea(new Dimension(20, pixels)));
  }
  
  final void addGlue(){
    currentPanel.add(Box.createVerticalGlue());
  }
}
