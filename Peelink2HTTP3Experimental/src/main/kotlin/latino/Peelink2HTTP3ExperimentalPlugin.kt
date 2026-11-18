package latino

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.extractors.Uqload

@CloudstreamPlugin
class Peelink2HTTP3ExperimentalPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(Peelink2HTTP3ExperimentalProvider())
        registerExtractorAPI(object : Uqload() {
            override var mainUrl = "https://uqload.vc"
        })
    }
}
