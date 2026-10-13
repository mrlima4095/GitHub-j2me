import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.Spacer;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.Ticker;
import java.util.Hashtable;

/** Canvas-rendered item form used by GH2ME screens. */
public class CanvasForm extends Canvas implements CommandListener, ItemStateListener, LangConstants {
  private static final int HEADER_HEIGHT = 36;
  private static final int PAD = 8;
  private static final int SELECTED = 0x1f2f45;
  private static final int BACKGROUND = 0x0d1117;
  private static final int SURFACE = 0x161b22;
  private static final int BORDER = 0x30363d;
  private static final int FOREGROUND = 0xe6edf3;
  private static final int MUTED = 0x8b949e;
  private static final int ACCENT = 0x2f81f7;
  private static final Hashtable ITEM_COMMANDS = new Hashtable();
  private static final Hashtable CHOICE_TYPES = new Hashtable();

  private final Vector items = new Vector();
  private final Vector commands = new Vector();
  private CommandListener commandListener;
  private ItemStateListener itemStateListener;
  private Ticker ticker;
  private Form nativeForm;
  private int selectedIndex;
  private int scrollY;
  private TextField editingField;

  public static void bindDefaultCommand(Item item, Command command) {
    item.setDefaultCommand(command);
    ITEM_COMMANDS.put(item, command);
  }

  public static ChoiceGroup choiceGroup(String label, int type, String[] choices, Image[] images) {
    ChoiceGroup choice = new ChoiceGroup(label, type, choices, images);
    CHOICE_TYPES.put(choice, new Integer(type));
    return choice;
  }

  public CanvasForm(String title) {
    setTitle(title);
  }

  public Displayable getDisplayable() {
    if (GH.legacyUI) return getNativeForm();
    setFullScreenMode(GH.canvasFullscreen);
    repaint();
    return this;
  }

  private Form getNativeForm() {
    if (nativeForm == null) {
      nativeForm = new Form(getTitle());
      nativeForm.setCommandListener(this);
      nativeForm.setItemStateListener(this);
      if (ticker != null) nativeForm.setTicker(ticker);
      for (int i = 0; i < commands.size(); i++) {
        nativeForm.addCommand((Command) commands.elementAt(i));
      }
      for (int i = 0; i < items.size(); i++) {
        nativeForm.append((Item) items.elementAt(i));
      }
    }
    return nativeForm;
  }

  public void setTitle(String title) {
    super.setTitle(title);
    if (nativeForm != null) nativeForm.setTitle(title);
    repaint();
  }

  public void addCommand(Command command) {
    if (!commands.contains(command)) commands.addElement(command);
    super.addCommand(command);
    if (nativeForm != null) nativeForm.addCommand(command);
  }

  public void removeCommand(Command command) {
    commands.removeElement(command);
    super.removeCommand(command);
    if (nativeForm != null) nativeForm.removeCommand(command);
  }

  public int append(Item item) {
    items.addElement(item);
    if (nativeForm != null) nativeForm.append(item);
    repaint();
    return items.size() - 1;
  }

  public int append(String text) {
    return append(new StringItem(null, text));
  }

  public void insert(int index, Item item) {
    items.insertElementAt(item, index);
    if (nativeForm != null) nativeForm.insert(index, item);
    if (selectedIndex >= index) selectedIndex++;
    repaint();
  }

  public void deleteAll() {
    for (int i = 0; i < items.size(); i++) ITEM_COMMANDS.remove(items.elementAt(i));
    items.removeAllElements();
    if (nativeForm != null) nativeForm.deleteAll();
    selectedIndex = 0;
    scrollY = 0;
    repaint();
  }

  public void delete(int index) {
    if (index >= 0 && index < items.size()) {
      ITEM_COMMANDS.remove(items.elementAt(index));
      items.removeElementAt(index);
      if (nativeForm != null) nativeForm.delete(index);
      if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
      if (selectedIndex < 0) selectedIndex = 0;
      ensureSelectedVisible();
      repaint();
    }
  }

  public Item get(int index) {
    return (Item) items.elementAt(index);
  }

  public int size() {
    return items.size();
  }

  public void setCurrentItem(Item item) {
    if (nativeForm != null && GH.display.getCurrent() == nativeForm) GH.display.setCurrentItem(item);
    for (int i = 0; i < items.size(); i++) {
      if (items.elementAt(i) == item) {
        selectedIndex = i;
        ensureSelectedVisible();
        repaint();
        return;
      }
    }
  }

  public void setTicker(Ticker ticker) {
    this.ticker = ticker;
    if (nativeForm != null) nativeForm.setTicker(ticker);
    repaint();
  }

  public Ticker getTicker() {
    return ticker;
  }

  public void setItemStateListener(ItemStateListener listener) {
    itemStateListener = listener;
    if (nativeForm != null) nativeForm.setItemStateListener(this);
  }

  public void setCommandListener(CommandListener listener) {
    commandListener = listener;
    super.setCommandListener(this);
  }

  public void commandAction(Command command, Displayable displayable) {
    if (commandListener != null) commandListener.commandAction(command, this);
  }

  public void itemStateChanged(Item item) {
    if (itemStateListener != null) itemStateListener.itemStateChanged(item);
  }

  public void paint(Graphics g) {
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
      if (title != null) drawTitle(g, title, PAD, headerHeight - 8, width - PAD * 2);
    }

    int bodyTop = headerHeight;
    if (ticker != null) {
      g.setColor(0x17212b);
      g.fillRect(0, bodyTop, width, 22);
      g.setColor(MUTED);
      g.setFont(Font.getDefaultFont());
      g.drawString(ticker.getString(), PAD, bodyTop + 4, Graphics.TOP | Graphics.LEFT);
      bodyTop += 22;
    }

    int oldClipX = g.getClipX();
    int oldClipY = g.getClipY();
    int oldClipWidth = g.getClipWidth();
    int oldClipHeight = g.getClipHeight();
    g.clipRect(0, bodyTop, width, Math.max(0, height - bodyTop));
    int y = bodyTop - scrollY;
    int availableWidth = width - PAD * 2;
    for (int i = 0; i < items.size(); i++) {
      Item item = (Item) items.elementAt(i);
      int itemHeight = itemHeight(item, availableWidth);
      if (y + itemHeight >= bodyTop && y < height) {
        if (i == selectedIndex) {
          g.setColor(SELECTED);
          g.fillRoundRect(4, y + 2, width - 8, itemHeight - 4, 7, 7);
          g.setColor(ACCENT);
          g.fillRoundRect(4, y + 7, 3, itemHeight - 14, 3, 3);
        } else if (!(item instanceof Spacer)) {
          g.setColor(SURFACE);
          g.fillRoundRect(4, y + 2, width - 8, itemHeight - 4, 7, 7);
        }
        if (!(item instanceof Spacer)) {
          g.setColor(BORDER);
          g.drawRoundRect(4, y + 2, width - 8, itemHeight - 4, 7, 7);
        }
        drawItem(g, item, PAD + 4, y + 6, availableWidth - 8, itemHeight - 12);
      }
      y += itemHeight;
    }
    g.setClip(oldClipX, oldClipY, oldClipWidth, oldClipHeight);
  }

  private int itemHeight(Item item, int width) {
    if (item instanceof Spacer) return Math.max(4, item.getMinimumHeight());
    if (item instanceof ImageItem) {
      Image image = ((ImageItem) item).getImage();
      return Math.max(32, image == null ? 32 : image.getHeight() + 8);
    }
    if (item instanceof CanvasMarkdownItem) {
      CanvasMarkdownItem document = (CanvasMarkdownItem) item;
      int total = 16;
      for (int i = 0; i < document.lines.length; i++) {
        Font lineFont = CanvasMarkdownItem.fontFor(document.styles[i]);
        total += Math.max(1, textLines(document.lines[i], lineFont, width - 28)) * (lineFont.getHeight() + 2) + 3;
      }
      return Math.max(44, total);
    }
    if (item instanceof RepoCardItem) {
      RepoCardItem card = (RepoCardItem) item;
      Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
      Font detailFont = GH.smallPlainFont == null ? Font.getDefaultFont() : GH.smallPlainFont;
      int titleLines = Math.min(2, textLines(card.title, titleFont, width - 32));
      int descriptionLines = card.description.length() == 0 ? 0 : Math.min(3, textLines(card.description, detailFont, width - 32));
      int metadataLines = card.metadata.length() == 0 ? 0 : Math.min(2, textLines(card.metadata, detailFont, width - 32));
      return Math.max(58, 15 + titleLines * (titleFont.getHeight() + 2)
          + descriptionLines * (detailFont.getHeight() + 2)
          + metadataLines * (detailFont.getHeight() + 2) + 10);
    }
    Font font = item instanceof StringItem ? ((StringItem) item).getFont() : Font.getDefaultFont();
    if (font == null) font = Font.getDefaultFont();
    int lines = 1;
    if (item instanceof StringItem) {
      StringItem string = (StringItem) item;
      String text = string.getText();
      if (string.getLabel() != null && string.getLabel().length() > 0 && text != null && text.length() > 0) {
        lines = 1 + textLines(text, font, width - 20);
      } else {
        lines = textLines(join(string.getLabel(), text), font, width - 20);
      }
    } else if (item instanceof TextField) {
      TextField field = (TextField) item;
      lines = 1 + textLines(field.getString(), font, width - 20);
    } else if (item instanceof ChoiceGroup) {
      lines = 2;
    } else if (item instanceof Gauge) {
      lines = 2;
    }
    return Math.max(36, lines * (font.getHeight() + 2) + 19);
  }

  private void drawItem(Graphics g, Item item, int x, int y, int width, int height) {
    if (item instanceof Spacer) return;
    if (item instanceof ImageItem) {
      Image image = ((ImageItem) item).getImage();
      if (image != null) {
        int imageY = y + Math.max(0, (height - image.getHeight()) / 2);
        g.drawImage(image, x, imageY, Graphics.TOP | Graphics.LEFT);
        String label = item.getLabel();
        if (label != null) drawText(g, label, x + image.getWidth() + 8, imageY, width - image.getWidth() - 8, Font.getDefaultFont(), FOREGROUND);
      }
      return;
    }
    if (item instanceof CanvasMarkdownItem) {
      drawMarkdown(g, (CanvasMarkdownItem) item, x + 4, y + 3, width - 24, height - 6);
      return;
    }
    if (item instanceof RepoCardItem) {
      RepoCardItem card = (RepoCardItem) item;
      Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
      Font detailFont = GH.smallPlainFont == null ? Font.getDefaultFont() : GH.smallPlainFont;
      int innerWidth = width - 28;
      int lineY = y + 3;
      lineY += drawTextLines(g, card.title, x + 5, lineY, innerWidth, titleFont, 0x58a6ff, 2);
      if (card.description.length() > 0) {
        lineY += 3;
        lineY += drawTextLines(g, card.description, x + 5, lineY, innerWidth, detailFont, FOREGROUND, 3);
      }
      if (card.metadata.length() > 0) {
        lineY += 3;
        drawTextLines(g, card.metadata, x + 5, lineY, innerWidth, detailFont, MUTED, 2);
      }
      return;
    }
    if (item instanceof StringItem) {
      StringItem string = (StringItem) item;
      int appearance = string.getAppearanceMode();
      Font font = string.getFont() == null ? Font.getDefaultFont() : string.getFont();
      String label = string.getLabel();
      String text = string.getText();
      if (label != null && label.length() > 0 && text != null && text.length() > 0) {
        drawText(g, label, x + 4, y + 1, width - 24, Font.getDefaultFont(), MUTED);
        drawText(g, text, x + 4, y + Font.getDefaultFont().getHeight() + 3, width - 24,
            font, appearance == Item.HYPERLINK ? 0x58a6ff : FOREGROUND);
      } else {
        drawText(g, join(label, text), x + 4, y + 4, width - 24,
            font, appearance == Item.HYPERLINK ? 0x58a6ff : FOREGROUND);
      }
      if (ITEM_COMMANDS.get(item) != null) {
        int arrowX = x + width - 13;
        int arrowY = y + height / 2;
        g.setColor(appearance == Item.HYPERLINK ? 0x58a6ff : MUTED);
        g.drawLine(arrowX - 3, arrowY - 4, arrowX + 1, arrowY);
        g.drawLine(arrowX + 1, arrowY, arrowX - 3, arrowY + 4);
      }
      return;
    }
    if (item instanceof TextField) {
      TextField field = (TextField) item;
      drawText(g, field.getLabel(), x + 4, y + 1, width - 24, Font.getDefaultFont(), MUTED);
      drawText(g, field.getString(), x + 4, y + Font.getDefaultFont().getHeight() + 3, width - 24,
          Font.getDefaultFont(), FOREGROUND);
      g.setColor(ACCENT);
      g.drawLine(x + 4, y + height - 2, x + width - 20, y + height - 2);
      return;
    }
    if (item instanceof ChoiceGroup) {
      ChoiceGroup choice = (ChoiceGroup) item;
      StringBuffer text = new StringBuffer();
      if (choice.getLabel() != null) {
        drawText(g, choice.getLabel(), x + 4, y + 1, width - 24, Font.getDefaultFont(), MUTED);
      }
      if (choice.size() > 0) {
        if (choiceType(choice) == Choice.MULTIPLE) {
          boolean[] selected = new boolean[choice.size()];
          choice.getSelectedFlags(selected);
          boolean first = true;
          for (int i = 0; i < selected.length; i++) {
            if (selected[i]) {
              if (!first) text.append(", ");
              text.append(choice.getString(i));
              first = false;
            }
          }
          if (first) text.append("-");
        } else {
          int index = choice.getSelectedIndex();
          if (index >= 0) text.append(choice.getString(index));
        }
      }
      drawText(g, text.toString(), x + 4, y + Font.getDefaultFont().getHeight() + 3, width - 24,
          Font.getDefaultFont(), FOREGROUND);
      int arrowX = x + width - 15;
      int arrowY = y + height / 2;
      g.setColor(ACCENT);
      g.drawLine(arrowX - 3, arrowY - 2, arrowX, arrowY + 2);
      g.drawLine(arrowX, arrowY + 2, arrowX + 3, arrowY - 2);
      return;
    }
    if (item instanceof Gauge) {
      Gauge gauge = (Gauge) item;
      drawText(g, join(gauge.getLabel(), Integer.toString(gauge.getValue())), x + 4, y + 2,
          width - 24, Font.getDefaultFont(), FOREGROUND);
      return;
    }
    drawText(g, item.getLabel(), x + 4, y + 3, width - 24, Font.getDefaultFont(), FOREGROUND);
  }

  private static String join(String label, String text) {
    if (label == null || label.length() == 0) return text == null ? "" : text;
    if (text == null || text.length() == 0) return label;
    return label.concat(": ").concat(text);
  }

  private static int choiceType(ChoiceGroup choice) {
    Integer type = (Integer) CHOICE_TYPES.get(choice);
    return type == null ? Choice.POPUP : type.intValue();
  }

  private static int textLines(String text, Font font, int width) {
    if (text == null || text.length() == 0) return 1;
    int lines = 1;
    int lineWidth = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\n') {
        lines++;
        lineWidth = 0;
        continue;
      }
      int charWidth = font.charWidth(c);
      if (lineWidth + charWidth > width && lineWidth > 0) {
        lines++;
        lineWidth = charWidth;
      } else {
        lineWidth += charWidth;
      }
    }
    return lines;
  }

  private static int drawTextLines(
      Graphics g, String text, int x, int y, int width, Font font, int color, int maxLines) {
    if (text == null || text.length() == 0 || maxLines <= 0) return 0;
    g.setColor(color);
    g.setFont(font);
    StringBuffer line = new StringBuffer();
    int lineWidth = 0;
    int lineY = y;
    int lines = 1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      int charWidth = font.charWidth(c);
      if (c == '\n' || (lineWidth + charWidth > width && line.length() > 0)) {
        g.drawString(line.toString(), x, lineY, Graphics.TOP | Graphics.LEFT);
        if (lines == maxLines) return lines * (font.getHeight() + 2);
        lines++;
        line.setLength(0);
        lineWidth = 0;
        lineY += font.getHeight() + 2;
        if (c == '\n') continue;
      }
      line.append(c);
      lineWidth += charWidth;
    }
    if (line.length() > 0 && lines <= maxLines) g.drawString(line.toString(), x, lineY, Graphics.TOP | Graphics.LEFT);
    return lines * (font.getHeight() + 2);
  }

  private static void drawMarkdown(Graphics g, CanvasMarkdownItem document, int x, int y, int width, int height) {
    int lineY = y;
    int bottom = y + height;
    for (int i = 0; i < document.lines.length && lineY < bottom; i++) {
      int style = document.styles[i];
      Font font = CanvasMarkdownItem.fontFor(style);
      int rows = Math.max(1, textLines(document.lines[i], font, width - (style == CanvasMarkdownItem.QUOTE ? 10 : 0)));
      int blockHeight = rows * (font.getHeight() + 2) + 2;
      if (style == CanvasMarkdownItem.CODE) {
        g.setColor(0x0d1117);
        g.fillRoundRect(x, lineY, width, blockHeight, 4, 4);
      } else if (style == CanvasMarkdownItem.QUOTE) {
        g.setColor(ACCENT);
        g.fillRoundRect(x, lineY + 1, 3, Math.max(3, blockHeight - 3), 3, 3);
      }
      int textX = x + (style == CanvasMarkdownItem.QUOTE ? 9 : 5);
      int color = style == CanvasMarkdownItem.HEADING ? FOREGROUND
          : style == CanvasMarkdownItem.CODE ? 0xa5d6ff
          : style == CanvasMarkdownItem.QUOTE ? MUTED : FOREGROUND;
      drawTextLines(g, document.lines[i], textX, lineY + 1, width - (textX - x) - 5, font, color, rows);
      lineY += blockHeight + 2;
    }
  }

  private static void drawText(Graphics g, String text, int x, int y, int width, Font font, int color) {
    if (text == null) return;
    g.setColor(color);
    g.setFont(font);
    StringBuffer line = new StringBuffer();
    int lineWidth = 0;
    int lineY = y;
    int maxY = y + Math.max(font.getHeight(), g.getClipHeight());
    for (int i = 0; i < text.length() && lineY < maxY; i++) {
      char c = text.charAt(i);
      int charWidth = font.charWidth(c);
      if (c == '\n' || (lineWidth + charWidth > width && line.length() > 0)) {
        g.drawString(line.toString(), x, lineY, Graphics.TOP | Graphics.LEFT);
        line.setLength(0);
        lineWidth = 0;
        lineY += font.getHeight() + 2;
        if (c == '\n') continue;
      }
      line.append(c);
      lineWidth += charWidth;
    }
    if (line.length() > 0 && lineY < maxY) g.drawString(line.toString(), x, lineY, Graphics.TOP | Graphics.LEFT);
  }

  private static void drawTitle(Graphics g, String title, int x, int baseline, int width) {
    Font font = g.getFont();
    while (title.length() > 0 && font.stringWidth(title) > width) {
      title = title.substring(0, title.length() - 1);
    }
    g.drawString(title, x, baseline, Graphics.BOTTOM | Graphics.LEFT);
  }

  private int bodyTop() {
    return headerHeight() + (ticker == null ? 0 : 22);
  }

  private int headerHeight() {
    return GH.canvasFullscreen ? HEADER_HEIGHT : 4;
  }

  private int itemAtY(int y) {
    int target = y + scrollY - bodyTop();
    if (target < 0) return -1;
    int position = 0;
    for (int i = 0; i < items.size(); i++) {
      int height = itemHeight((Item) items.elementAt(i), getWidth() - PAD * 2);
      if (target < position + height) return i;
      position += height;
    }
    return -1;
  }

  private void ensureSelectedVisible() {
    if (selectedIndex < 0 || selectedIndex >= items.size()) return;
    int top = 0;
    for (int i = 0; i < selectedIndex; i++) top += itemHeight((Item) items.elementAt(i), getWidth() - PAD * 2);
    int selectedHeight = itemHeight((Item) items.elementAt(selectedIndex), getWidth() - PAD * 2);
    int viewHeight = getHeight() - bodyTop();
    if (selectedHeight > viewHeight && items.elementAt(selectedIndex) instanceof CanvasMarkdownItem) {
      int max = top + selectedHeight - viewHeight;
      if (scrollY < top) scrollY = top;
      else if (scrollY > max) scrollY = max;
    } else if (top < scrollY) scrollY = top;
    else if (top + selectedHeight > scrollY + viewHeight) scrollY = top + selectedHeight - viewHeight;
    if (scrollY < 0) scrollY = 0;
  }

  protected void keyPressed(int keyCode) {
    int action;
    try {
      action = getGameAction(keyCode);
    } catch (Exception e) {
      action = 0;
    }
    if ((action == UP || action == DOWN || keyCode == KEY_NUM2 || keyCode == KEY_NUM8)
        && scrollSelectedDocument(action == UP || keyCode == KEY_NUM2 ? -1 : 1)) {
      repaint();
    } else if (action == UP || keyCode == KEY_NUM2) {
      if (selectedIndex > 0) selectedIndex--;
      ensureSelectedVisible();
      repaint();
    } else if (action == DOWN || keyCode == KEY_NUM8) {
      if (selectedIndex < items.size() - 1) selectedIndex++;
      ensureSelectedVisible();
      repaint();
    } else if (action == LEFT) {
      changeChoice(-1);
    } else if (action == RIGHT) {
      changeChoice(1);
    } else if (action == FIRE) {
      activateSelected();
    }
  }

  private boolean scrollSelectedDocument(int direction) {
    if (selectedIndex < 0 || selectedIndex >= items.size()
        || !(items.elementAt(selectedIndex) instanceof CanvasMarkdownItem)) return false;
    int top = 0;
    int width = getWidth() - PAD * 2;
    for (int i = 0; i < selectedIndex; i++) top += itemHeight((Item) items.elementAt(i), width);
    int documentHeight = itemHeight((Item) items.elementAt(selectedIndex), width);
    int viewHeight = getHeight() - bodyTop();
    int minimum = top;
    int maximum = Math.max(minimum, top + documentHeight - viewHeight);
    int next = scrollY + direction * Math.max(18, Font.getDefaultFont().getHeight() * 2);
    if (next < minimum) next = minimum;
    if (next > maximum) next = maximum;
    if (next == scrollY) return true;
    scrollY = next;
    return true;
  }

  protected void pointerPressed(int x, int y) {
    int row = itemAtY(y);
    if (row >= 0) {
      selectedIndex = row;
      ensureSelectedVisible();
      repaint();
    }
  }

  protected void pointerReleased(int x, int y) {
    int row = itemAtY(y);
    if (row >= 0 && row == selectedIndex) activateSelected();
  }

  private void activateSelected() {
    if (selectedIndex < 0 || selectedIndex >= items.size()) return;
    Item item = (Item) items.elementAt(selectedIndex);
    if (item instanceof TextField) {
      edit((TextField) item);
    } else if (item instanceof ChoiceGroup) {
      changeChoice(1);
    } else if (item instanceof StringItem) {
      Command command = (Command) ITEM_COMMANDS.get(item);
      if (command != null) GH.midlet.commandAction(command, item);
    }
  }

  private void changeChoice(int direction) {
    if (selectedIndex < 0 || selectedIndex >= items.size()) return;
    Item item = (Item) items.elementAt(selectedIndex);
    if (!(item instanceof ChoiceGroup)) return;
    ChoiceGroup choice = (ChoiceGroup) item;
    int count = choice.size();
    if (count == 0) return;
    if (choiceType(choice) == Choice.MULTIPLE) {
      boolean[] selected = new boolean[count];
      choice.getSelectedFlags(selected);
      int index = selectedIndex < count ? selectedIndex : 0;
      choice.setSelectedIndex(index, !selected[index]);
    } else {
      int index = choice.getSelectedIndex() + direction;
      if (index < 0) index = count - 1;
      if (index >= count) index = 0;
      choice.setSelectedIndex(index, true);
    }
    if (itemStateListener != null) itemStateListener.itemStateChanged(item);
    repaint();
  }

  private void edit(final TextField field) {
    if ((field.getConstraints() & TextField.UNEDITABLE) != 0) return;
    editingField = field;
    final TextBox editor = new TextBox(field.getLabel(), field.getString(), field.getMaxSize(), field.getConstraints());
    final Command ok = new Command(GH.L[LOk], Command.OK, 1);
    final Command cancel = new Command(GH.L[LCancel], Command.CANCEL, 1);
    editor.addCommand(ok);
    editor.addCommand(cancel);
    editor.setCommandListener(new CommandListener() {
      public void commandAction(Command command, Displayable displayable) {
        if (command == ok) {
          field.setString(editor.getString());
          if (itemStateListener != null) itemStateListener.itemStateChanged(field);
        }
        GH.display(CanvasForm.this, true);
        repaint();
      }
    });
    GH.display(editor);
  }
}
