/*
Copyright (c) 2026 Arman Jussupgaliyev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import java.util.Hashtable;

public class EventsForm extends PagedForm {
  public EventsForm(String title, String url) {
    super(title);
    this.url = url;
  }

  void loadInternal(Thread thread) throws Exception {
    JSONArray events = pagedApi(thread, new StringBuffer(url));
    if (urls == null) urls = new Hashtable();
    else urls.clear();

    for (int i = 0; i < events.size() && thread == this.thread; i++) {
      JSONObject event = events.getObject(i);
      String type = event.getString("type", "");
      String actor = event.getObject("actor").getString("login", "");
      String repo = event.getObject("repo").getString("name", "");
      String title = actor;
      String description = repo;
      String target = repo;
      String metadata = "";
      String created = event.getString("created_at", null);
      if (created != null && created.length() > 0 && !"PublicEvent".equals(type)) {
        metadata = GH.localizeDate(created, 1);
      }

      if ("WatchEvent".equals(type)) {
        boolean yourRepository = GH.login != null && repo.startsWith(GH.login.concat("/"));
        title = withActor(actor, GH.L[yourRepository ? LEventStarredYour : LEventStarred]);
      } else if ("ForkEvent".equals(type)) {
        boolean yourRepository = GH.login != null && repo.startsWith(GH.login.concat("/"));
        title = withActor(actor, GH.L[yourRepository ? LEventForkedYour : LEventForked]);
        description = event.getObject("payload").getObject("forkee").getString("full_name", repo);
        target = description;
      } else if ("PublicEvent".equals(type)) {
        title = withActor(actor, GH.L[LEventMadePublic]);
      } else if ("ReleaseEvent".equals(type)) {
        title = repo.concat(GH.L[LEventReleased]);
        String tag = event.getObject("payload").getObject("release").getString("tag_name", "");
        description = tag;
        if (tag.length() > 0) target = repo.concat("/releases/tag/").concat(tag);
      } else if ("PushEvent".equals(type)) {
        title = withActor(actor, GH.L[LEventPushed]);
        JSONObject payload = event.getObject("payload");
        String commit = payload.getString("head", "");
        String branch = payload.getString("ref", "");
        if (branch.startsWith("refs/heads/")) branch = branch.substring(11);
        String shortCommit = commit.length() > 7 ? commit.substring(0, 7) : commit;
        description = repo.concat("\n").concat(shortCommit);
		if (branch.length() > 0) description = description.concat("  -  ").concat(branch);
        if (commit.length() > 0) target = repo.concat("/commit/").concat(commit);
      }

      RepoCardItem card = new RepoCardItem(title, description, metadata);
      card.setFont(GH.medPlainFont);
      card.setDefaultCommand(GH.mdLinkCmd);
      card.setItemCommandListener(GH.midlet);
      if (target != null && target.length() > 0) urls.put(card, target);
      safeAppend(thread, card);
    }
  }

  private static String withActor(String actor, String action) {
    return actor == null || actor.length() == 0 ? action : actor.concat(" ").concat(action);
  }
}
