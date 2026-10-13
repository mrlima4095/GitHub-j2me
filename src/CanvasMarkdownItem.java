import java.util.Vector;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.StringItem;

/** A complete text/Markdown document represented by one scrollable Canvas item. */
public final class CanvasMarkdownItem extends StringItem {
  static final int BODY = 0;
  static final int HEADING = 1;
  static final int CODE = 2;
  static final int QUOTE = 3;
  static final int BULLET = 4;

  final String[] lines;
  final byte[] styles;

  public CanvasMarkdownItem(String source, boolean markdown) {
    super(null, source == null ? "" : source, StringItem.PLAIN);
    Vector textLines = new Vector();
    Vector lineStyles = new Vector();
    String content = source == null ? "" : source;
    boolean inCode = false;
    int start = 0;
    while (start <= content.length()) {
      int end = content.indexOf('\n', start);
      if (end < 0) end = content.length();
      String line = content.substring(start, end);
      if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
      int style = BODY;
      if (markdown) {
        String trim = line.trim();
        if (trim.startsWith("```")) {
          inCode = !inCode;
          if (end == content.length()) break;
          start = end + 1;
          continue;
        }
        if (inCode) {
          style = CODE;
        } else if (trim.startsWith("### ") || trim.startsWith("## ") || trim.startsWith("# ")) {
          int hash = 0;
          while (hash < trim.length() && trim.charAt(hash) == '#') hash++;
          line = trim.substring(hash).trim();
          style = HEADING;
        } else if (trim.startsWith(">")) {
          line = trim.substring(1).trim();
          style = QUOTE;
        } else if (trim.startsWith("- ") || trim.startsWith("* ") || trim.startsWith("+ ")) {
          line = "-  ".concat(trim.substring(2));
          style = BULLET;
        } else {
          line = trim;
        }
        if (style != CODE) line = stripInlineMarkup(line);
      }
      textLines.addElement(line);
      lineStyles.addElement(new Integer(style));
      if (end == content.length()) break;
      start = end + 1;
    }
    if (textLines.size() == 0) {
      textLines.addElement("");
      lineStyles.addElement(new Integer(BODY));
    }
    lines = new String[textLines.size()];
    styles = new byte[lineStyles.size()];
    for (int i = 0; i < lines.length; i++) {
      lines[i] = (String) textLines.elementAt(i);
      styles[i] = (byte) ((Integer) lineStyles.elementAt(i)).intValue();
    }
  }

  private static String stripInlineMarkup(String line) {
    StringBuffer out = new StringBuffer(line.length());
    int i = 0;
    while (i < line.length()) {
      if (line.charAt(i) == '[') {
        int close = line.indexOf("](", i + 1);
        int end = close == -1 ? -1 : line.indexOf(')', close + 2);
        if (close != -1 && end != -1) {
          out.append(line.substring(i + 1, close));
          i = end + 1;
          continue;
        }
      }
      char c = line.charAt(i++);
      if (c != '*' && c != '_' && c != '~' && c != '`') out.append(c);
    }
    return out.toString();
  }

  static Font fontFor(int style) {
    if (style == HEADING) return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    if (style == CODE) return Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    return Font.getDefaultFont();
  }
}
