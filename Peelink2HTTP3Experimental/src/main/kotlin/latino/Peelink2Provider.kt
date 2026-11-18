package latino

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.net.URLEncoder
import java.net.URI
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal fun peelinkProxyUrl(url: String): String? = runCatching {
    val uri = URI(url)
    require(uri.scheme.equals("https", true) && uri.host?.lowercase() in setOf("peelink2.com", "www.peelink2.com"))
    require(uri.userInfo == null && (uri.port == -1 || uri.port == 443))
    val path = uri.rawPath.orEmpty().ifEmpty { "/" }
    val decodedPath = uri.path.orEmpty().ifEmpty { "/" }
    require(path.startsWith("/") && !path.startsWith("//") && !decodedPath.startsWith("//"))
    require(!decodedPath.any { it == '\\' || it.code < 32 || it.code == 127 })
    require(!uri.query.orEmpty().any { it == '\\' || it.code < 32 || it.code == 127 })
    "http://127.0.0.1:8768$path" + (uri.rawQuery?.let { "?$it" } ?: "")
}.getOrNull()

class Peelink2HTTP3ExperimentalProvider : LatinoProvider() {
    override var mainUrl = "https://www.peelink2.com"
    override var name = "Peelink2 HTTP3 Experimental"
    override val mainPage = mainPageOf(
        "$mainUrl/inicio" to "Todo el catálogo",
        "$mainUrl/inicio#destacadas" to "Destacadas",
        "$mainUrl/latino" to "Películas en latino",
        "$mainUrl/2012/12/pagina-accion.html" to "Acción",
        "$mainUrl/2012/12/pagina-ficcion.html" to "Ciencia ficción",
        "$mainUrl/2012/12/pagina-drama.html" to "Drama",
        "$mainUrl/2012/12/pagina-comedia.html" to "Comedia",
        "$mainUrl/2012/12/pagina-infantil.html" to "Infantil",
        "$mainUrl/2012/12/pagina-terror.html" to "Terror"
    )
    private suspend fun peelinkDocument(url: String): Document {
        val target = peelinkProxyUrl(url) ?: throw ErrorLoadingException("URL de Peelink2 no autorizada.")
        val response = app.get(target)
        if (response.code !in 200..299) throw ErrorLoadingException("Peelink2 respondió HTTP ${response.code}.")
        if (response.text.contains("<title>Just a moment", ignoreCase = true)) {
            throw ErrorLoadingException("Peelink2 bloquea el acceso con Cloudflare.")
        }
        return Jsoup.parse(response.text, url)
    }

    private fun cards(
        doc: Document, featured: Boolean = false, fragment: Boolean = false, search: Boolean = false
    ): List<SearchResponse> =
        PeelinkCatalog.items(doc, mainUrl, featured, fragment, search).map { item ->
            newMovieSearchResponse(item.title, item.url, TvType.Movie) {
                posterUrl = item.poster
                year = item.year
            }
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val featured = request.data.endsWith("#destacadas")
        val genre = request.data.contains("/pagina-")
        val doc: Document
        if (genre && page > 1) {
            return newHomePageResponse(HomePageList(request.name, emptyList()), false)
        } else {
            doc = peelinkDocument(PeelinkCatalog.pageUrl(request.data, page))
        }
        return newHomePageResponse(
            HomePageList(request.name, cards(doc, featured, genre && page > 1)),
            if (genre) false
            else !featured && PeelinkCatalog.hasNext(doc, page)
        )
    }

    private suspend fun searchPage(query: String, page: Int): Pair<List<SearchResponse>, Boolean> {
        for (term in PeelinkCatalog.searchTerms(query)) {
            val doc = peelinkDocument("$mainUrl/?s=${URLEncoder.encode(term, "UTF-8")}&paged=$page")
            val results = cards(doc, search = true)
            if (results.isNotEmpty()) return results to PeelinkCatalog.hasNext(doc, page)
        }
        return emptyList<SearchResponse>() to false
    }

    override suspend fun search(query: String): List<SearchResponse> = searchPage(query, 1).first

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val (results, hasNext) = searchPage(query, page)
        return newSearchResponseList(results, hasNext)
    }

    override suspend fun load(url: String): LoadResponse = movie(url, peelinkDocument(url))

    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = peelinkDocument(data)
        return resolvePlayers(
            HtmlTools.latinoPlayers(doc.html(), data), data, subtitleCallback, callback
        )
    }
}
