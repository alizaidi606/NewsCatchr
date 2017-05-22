/*
 * NewsCatchr
 * Copyright © 2017 Jan-Lukas Else
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package jlelse.newscatchr.ui.fragments

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import co.metalab.asyncawait.async
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import jlelse.newscatchr.backend.helpers.Database
import jlelse.newscatchr.backend.helpers.Preferences
import jlelse.newscatchr.backend.loaders.PocketLoader
import jlelse.newscatchr.extensions.notNullAndEmpty
import jlelse.newscatchr.extensions.notNullOrBlank
import jlelse.newscatchr.extensions.tryOrNull
import jlelse.newscatchr.ui.layout.RefreshRecyclerUI
import jlelse.newscatchr.ui.recycleritems.ArticleRecyclerItem
import jlelse.newscatchr.ui.views.StatefulRecyclerView
import jlelse.newscatchr.ui.views.SwipeRefreshLayout
import jlelse.readit.R
import jlelse.viewmanager.ViewManagerView
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.find

class BookmarksView : ViewManagerView() {
	private var fragmentView: View? = null
	private val recyclerOne: StatefulRecyclerView? by lazy { fragmentView?.find<StatefulRecyclerView>(R.id.refreshrecyclerview_recycler) }
	private var fastAdapter = FastItemAdapter<ArticleRecyclerItem>()
	private val refreshOne: SwipeRefreshLayout? by lazy { fragmentView?.find<SwipeRefreshLayout>(R.id.refreshrecyclerview_refresh) }

	override fun onCreateView(): View? {
		super.onCreateView()
		fragmentView = RefreshRecyclerUI().createView(AnkoContext.create(context, this))
		refreshOne?.setOnRefreshListener { loadArticles() }
		if (recyclerOne?.adapter == null) recyclerOne?.adapter = fastAdapter
		loadArticles(true)
		return fragmentView
	}

	fun loadArticles(cache: Boolean = false) = async {
		refreshOne?.showIndicator()
		val articles = await {
			if (!cache && Preferences.pocketSync && Preferences.pocketUserName.notNullOrBlank() && Preferences.pocketAccessToken.notNullOrBlank()) {
				tryOrNull { Database.allBookmarks = PocketLoader().items() }
			}
			Database.allBookmarks
		}
		if (articles.notNullAndEmpty()) {
			fastAdapter.setNewList(articles.map { ArticleRecyclerItem(ctx = context, article = it, fragment = this@BookmarksView) })
			if (cache) recyclerOne?.restorePosition()
		} else {
			fastAdapter.setNewList(listOf())
		}
		refreshOne?.hideIndicator()
	}

	override fun inflateMenu(inflater: MenuInflater, menu: Menu?) {
		super.inflateMenu(inflater, menu)
		inflater.inflate(R.menu.bookmarksfragment, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem?) {
		when (item?.itemId) {
			R.id.refresh -> {
				loadArticles()
			}
		}
	}
}