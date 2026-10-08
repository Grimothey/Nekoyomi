package eu.kanade.tachiyomi.ui.reader.loader

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import mihon.core.archive.EpubReader

/**
 * Loader used to load a text (novel) .epub file. Each XHTML document in the spine becomes one
 * [ReaderPage]; its HTML (with inline images) is fetched on demand via [getHtml] and rendered by the
 * novel viewer (see [eu.kanade.tachiyomi.ui.reader.viewer.text.TextViewer]).
 *
 * If [spineRange] is given, only those spine sections are loaded, so one epub can be split into
 * several chapters. With no range the whole epub is loaded, as before.
 */
internal class EpubTextPageLoader(
    private val reader: EpubReader,
    private val spineRange: IntRange? = null,
) : PageLoader(), TextPageLoader {

    override var isLocal: Boolean = true

    private val spinePaths by lazy {
        val all = reader.getSpinePaths()
        if (spineRange == null) {
            all
        } else {
            val first = spineRange.first.coerceAtLeast(0)
            val last = spineRange.last.coerceAtMost(all.lastIndex)
            if (first > last) emptyList() else all.slice(first..last)
        }
    }

    override suspend fun getPages(): List<ReaderPage> {
        return spinePaths.indices.map { i ->
            ReaderPage(i).apply {
                status = Page.State.READY
            }
        }
    }

    /** Returns the `<body>` HTML of the section for [page], with its images inlined as data URIs. */
    fun getHtml(page: ReaderPage): String {
        val path = spinePaths.getOrNull(page.index) ?: return ""
        return reader.getSectionHtml(path)
    }

    override suspend fun getPageText(page: ReaderPage): String = getHtml(page)

    override suspend fun loadPage(page: ReaderPage) {
        check(!isRecycled)
    }

    override fun recycle() {
        super.recycle()
        reader.close()
    }
}
