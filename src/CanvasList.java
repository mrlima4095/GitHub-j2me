import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

/** Canvas-backed list for bookmarks and file/repository browsing. */
public final class CanvasList extends Canvas implements CommandListener {
  private static final int ROW_HEIGHT = 42;
  private static final int HEADER_HEIGHT = 38;
  private static final int BACKGROUND = 0x0d1117;
  private static final int SURFACE = 0x161b22;
  private static final int SELECTED = 0x1f2f45;
  private static final int BORDER = 0x30363d;
  private static final int FOREGROUND = 0xe6edf3;
  private static final int MUTED = 0x8b949e;
  private static final int ACCENT = 0x2f81f7;
  private final Vector labels = new Vector();
  private final Vector images = new Vector();
  private CommandListener commandListener;
  private int selectedIndex;
  private int topIndex;
  private Command selectCommand;
  private List nativeList;
  private final Vector commands = new Vector();

  public CanvasList(String title, int listType) {
    setTitle(title);
  }

  public Displayable getDisplayable() {
    if (GH.legacyUI) return getNativeList();
    setFullScreenMode(GH.canvasFullscreen);
    repaint();
    return this;
  }

  private List getNativeList() {
    if (nativeList == null) {
      nativeList = new List(getTitle(), List.IMPLICIT);
      nativeList.setCommandListener(this);
      if (selectCommand != null) nativeList.setSelectCommand(selectCommand);
      for (int i = 0; i < commands.size(); i++) nativeList.addCommand((Command) commands.elementAt(i));
      for (int i = 0; i < labels.size(); i++) nativeList.append(getString(i), getImage(i));
      if (labels.size() > 0) nativeList.setSelectedIndex(selectedIndex, true);
    }
    return nativeList;
  }

  public void setTitle(String title) {
    super.setTitle(title);
    if (nativeList != null) nativeList.setTitle(title);
    repaint();
  }

  public void addCommand(Command command) {
    if (!commands.contains(command)) commands.addElement(command);
    super.addCommand(command);
    if (nativeList != null) nativeList.addCommand(command);
  }

  public void removeCommand(Command command) {
    commands.removeElement(command);
    super.removeCommand(command);
    if (nativeList != null) nativeList.removeCommand(command);
  }

  public void setFitPolicy(int policy) {}

  public void setSelectCommand(Command command) {
    selectCommand = command;
    if (nativeList != null) nativeList.setSelectCommand(command);
  }

  public void setCommandListener(CommandListener listener) {
    commandListener = listener;
    super.setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    if (displayable == nativeList && nativeList != null) selectedIndex = nativeList.getSelectedIndex();
    if (commandListener != null) commandListener.commandAction(command, this);
  }

  public int append(String text, Image image) {
    labels.addElement(text == null ? "" : text);
    Object storedImage = image == null ? (Object) this : (Object) image;
    images.addElement(storedImage);
    if (nativeList != null) nativeList.append(text == null ? "" : text, image);
    repaint();
    return labels.size() - 1;
  }

  public void insert(int index, String text, Image image) {
    labels.insertElementAt(text == null ? "" : text, index);
    Object storedImage = image == null ? (Object) this : (Object) image;
    images.insertElementAt(storedImage, index);
    if (nativeList != null) nativeList.insert(index, text == null ? "" : text, image);
    if (selectedIndex >= index) selectedIndex++;
    repaint();
  }

  public void delete(int index) {
    if (index < 0 || index >= labels.size()) return;
    labels.removeElementAt(index);
    images.removeElementAt(index);
    if (nativeList != null) nativeList.delete(index);
    if (selectedIndex >= labels.size()) selectedIndex = labels.size() - 1;
    if (selectedIndex < 0) selectedIndex = 0;
    keepSelectionVisible();
    repaint();
  }

  public void deleteAll() {
    labels.removeAllElements();
    images.removeAllElements();
    if (nativeList != null) nativeList.deleteAll();
    selectedIndex = topIndex = 0;
    repaint();
  }

  public int size() {
    return labels.size();
  }

  public int getSelectedIndex() {
    return labels.size() == 0 ? -1 : selectedIndex;
  }

  public void setSelectedIndex(int index, boolean selected) {
    if (selected && index >= 0 && index < labels.size()) {
      selectedIndex = index;
      if (nativeList != null) nativeList.setSelectedIndex(index, true);
      keepSelectionVisible();
      repaint();
    }
  }

  public String getString(int index) {
    return (String) labels.elementAt(index);
  }

  public Image getImage(int index) {
    Object image = images.elementAt(index);
    return image == this ? null : (Image) image;
  }

  protected void paint(Graphics g) {
    int width = getWidth();
    int height = getHeight();
    int headerHeight = headerHeight();
    g.setColor(BACKGROUND);
    g.fillRect(0, 0, width, height);
    g.setColor(ACCENT);
    g.fillRect(0, headerHeight - 2, width, 2);
    if (GH.canvasFullscreen) {
      g.setColor(SURFACE);
      g.fillRect(0, 0, width, headerHeight - 2);
      g.setColor(ACCENT);
      g.fillRect(0, headerHeight - 2, width, 2);
      g.setColor(FOREGROUND);
      g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
      String title = getTitle();
      if (title != null) drawTruncated(g, title, 9, headerHeight - 8, width - 18, Graphics.BOTTOM);
    }

    int rows = Math.max(1, (height - headerHeight) / ROW_HEIGHT);
    if (selectedIndex < topIndex) topIndex = selectedIndex;
    if (selectedIndex >= topIndex + rows) topIndex = selectedIndex - rows + 1;
    for (int i = topIndex; i < labels.size() && i < topIndex + rows; i++) {
      int y = headerHeight + (i - topIndex) * ROW_HEIGHT;
      if (i == selectedIndex) {
        g.setColor(SELECTED);
        g.fillRoundRect(4, y + 2, width - 13, ROW_HEIGHT - 4, 8, 8);
        g.setColor(ACCENT);
        g.fillRoundRect(4, y + 9, 3, ROW_HEIGHT - 18, 3, 3);
      } else {
        g.setColor(BORDER);
        g.drawLine(12, y + ROW_HEIGHT - 1, width - 12, y + ROW_HEIGHT - 1);
      }
      Image image = getImage(i);
      int textX = 13;
      if (image != null) {
        int imageHeight = Math.min(image.getHeight(), ROW_HEIGHT - 14);
        int imageWidth = Math.min(image.getWidth(), ROW_HEIGHT - 14);
        g.drawImage(image, 13, y + (ROW_HEIGHT - imageHeight) / 2, Graphics.TOP | Graphics.LEFT);
        textX += imageWidth + 8;
      }
      g.setColor(FOREGROUND);
      g.setFont(Font.getDefaultFont());
      drawTruncated(g, getString(i), textX, y + (ROW_HEIGHT - g.getFont().getHeight()) / 2,
          width - textX - 17, Graphics.TOP);
    }

    if (labels.size() > rows) {
      int trackHeight = height - headerHeight - 8;
      int thumbHeight = Math.max(18, trackHeight * rows / labels.size());
      int thumbY = headerHeight + 4 + (trackHeight - thumbHeight) * topIndex / Math.max(1, labels.size() - rows);
      g.setColor(BORDER);
      g.fillRoundRect(width - 4, headerHeight + 4, 2, trackHeight, 2, 2);
      g.setColor(MUTED);
      g.fillRoundRect(width - 4, thumbY, 2, thumbHeight, 2, 2);
    }
  }

  private void drawTruncated(Graphics g, String text, int x, int y, int maxWidth, int verticalAnchor) {
    if (g.getFont().stringWidth(text) <= maxWidth) {
      g.drawString(text, x, y, verticalAnchor | Graphics.LEFT);
      return;
    }
    int end = text.length();
    while (end > 0 && g.getFont().stringWidth(text.substring(0, end).concat("…")) > maxWidth) end--;
    g.drawString(text.substring(0, end).concat("…"), x, y, verticalAnchor | Graphics.LEFT);
  }

  protected void keyPressed(int keyCode) {
    if (labels.size() == 0) return;
    int action;
    try {
      action = getGameAction(keyCode);
    } catch (Exception e) {
      action = 0;
    }
    if ((action == UP || keyCode == KEY_NUM2) && selectedIndex > 0) {
      selectedIndex--;
      keepSelectionVisible();
      repaint();
    } else if ((action == DOWN || keyCode == KEY_NUM8) && selectedIndex < labels.size() - 1) {
      selectedIndex++;
      keepSelectionVisible();
      repaint();
    } else if (action == FIRE) {
      if (commandListener != null) commandListener.commandAction(selectCommand == null ? List.SELECT_COMMAND : selectCommand, this);
    }
  }

  protected void pointerPressed(int x, int y) {
    int headerHeight = headerHeight();
    if (y < headerHeight) return;
    int index = topIndex + (y - headerHeight) / ROW_HEIGHT;
    if (index >= 0 && index < labels.size()) {
      selectedIndex = index;
      repaint();
    }
  }

  protected void pointerReleased(int x, int y) {
    int headerHeight = headerHeight();
    if (y < headerHeight) return;
    int index = topIndex + (y - headerHeight) / ROW_HEIGHT;
    if (index == selectedIndex && commandListener != null) {
      commandListener.commandAction(selectCommand == null ? List.SELECT_COMMAND : selectCommand, this);
    }
  }

  private void keepSelectionVisible() {
    int rows = Math.max(1, (getHeight() - headerHeight()) / ROW_HEIGHT);
    if (selectedIndex < topIndex) topIndex = selectedIndex;
    if (selectedIndex >= topIndex + rows) topIndex = selectedIndex - rows + 1;
    if (topIndex < 0) topIndex = 0;
  }

  private int headerHeight() {
    return GH.canvasFullscreen ? HEADER_HEIGHT : 4;
  }
}
