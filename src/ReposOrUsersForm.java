/*
Copyright (c) 2025 Arman Jussupgaliyev

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
import java.util.Hashtable;

import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

// list of repositories (forks, etc) or users (stargazers, followers, etc.)
public class ReposOrUsersForm extends PagedForm {

	String sort;
	boolean mini;
	int mode;

	// mode: 0, 1 - repos, 2 - users
	public ReposOrUsersForm(String url, String title, String sort, int mode) {
		super(title);
		this.url = url;
		this.sort = sort;
		this.mode = mode;
	}

	void loadInternal(Thread thread) throws Exception {
		StringBuffer sb = new StringBuffer(url);
		JSONArray r = pagedApi(thread, sort != null ? sb.append("sort=").append(sort) : sb);
		int l = r.size();
		
		if (urls == null) {
			urls = new Hashtable();
		} else {
			urls.clear();
		}
		for (int i = 0; i < l && thread == this.thread; ++i) {
			JSONObject j = r.getObject(i);

			// user
			if (mode == 2) {
				String contributions = j.getString("contributions", null);
				String details = contributions == null ? "" : contributions.concat(GH.L[L_contributions]);
				RepoCardItem card = new RepoCardItem(j.getString("login"), details, "");
				card.setFont(GH.medPlainFont);
				card.setDefaultCommand(GH.userCmd);
				card.setItemCommandListener(GH.midlet);
				safeAppend(thread, card);
				continue;
			}

			String name = j.getString("name", "");
			String fullName = j.getString("full_name", null);
			if (fullName == null || fullName.length() == 0) {
				JSONObject owner = j.has("owner") ? j.getObject("owner") : null;
				String ownerName = owner == null ? null : owner.getString("login", null);
				fullName = ownerName == null ? name : ownerName.concat("/").concat(name);
			}
			
			StringBuffer metadata = new StringBuffer();
			boolean privateRepo = j.getBoolean("private", false);
			boolean fork = j.getBoolean("fork", false);
			boolean archived = j.getBoolean("archived", false);
			if (privateRepo || fork || archived) {
				metadata.append(GH.L[privateRepo
						? (archived ? L_privateArchive_ : (fork ? L_privateFork_ : L_private_))
						: (archived ? L_archive_ : L_fork_)]);
			}
			String language = j.getString("language", null);
			if (language != null && language.length() > 0) {
				if (metadata.length() > 0) metadata.append("  -  ");
				metadata.append(language);
			}
			String updated = j.getString("pushed_at", null);
			if (updated == null) updated = j.getString("updated_at", null);
			if (updated != null && updated.length() > 0) {
				if (metadata.length() > 0) metadata.append("  -  ");
				metadata.append(GH.L[LUpdated]).append(GH.localizeDate(updated, 1));
			}
			String description = mini ? "" : j.getString("description", "");
			if (description == null) description = "";
			RepoCardItem card = new RepoCardItem(fullName, description, metadata.toString());
			card.setFont(GH.medPlainFont);
			card.setDefaultCommand(GH.mdLinkCmd);
			card.setItemCommandListener(GH.midlet);
			urls.put(card, fullName);
			safeAppend(thread, card);
		}
	}

}
