import javax.microedition.lcdui.StringItem;

/** One focusable repository summary, rendered as a single Canvas card. */
public final class RepoCardItem extends StringItem {
  public final String title;
  public final String description;
  public final String metadata;

  public RepoCardItem(String title, String description, String metadata) {
    this(title, description, metadata, true);
  }

  public RepoCardItem(String title, String description, String metadata, boolean clickable) {
    super(title, combine(description, metadata), clickable ? StringItem.BUTTON : StringItem.PLAIN);
    this.title = title == null ? "" : title;
    this.description = description == null ? "" : description;
    this.metadata = metadata == null ? "" : metadata;
  }

  private static String combine(String description, String metadata) {
    StringBuffer text = new StringBuffer();
    if (description != null && description.length() > 0) text.append(description);
    if (metadata != null && metadata.length() > 0) {
      if (text.length() > 0) text.append('\n');
      text.append(metadata);
    }
    return text.toString();
  }
}
