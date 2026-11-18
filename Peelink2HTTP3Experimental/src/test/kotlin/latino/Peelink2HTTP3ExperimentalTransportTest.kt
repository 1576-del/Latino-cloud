package latino

import org.junit.Assert.*
import org.junit.Test

class Peelink2HTTP3ExperimentalTransportTest {
    @Test fun preservesEncodedPathAndQuery() {
        assertEquals("http://127.0.0.1:8768/?s=spider%20man&paged=2", peelinkProxyUrl("https://www.peelink2.com/?s=spider%20man&paged=2#ignored"))
        assertEquals("http://127.0.0.1:8768/latino/page/2", peelinkProxyUrl("https://peelink2.com/latino/page/2"))
        assertEquals("http://127.0.0.1:8768/inicio", peelinkProxyUrl("https://www.peelink2.com:443/inicio"))
    }

    @Test fun rejectsUntrustedUrls() {
        listOf("http://www.peelink2.com/", "https://evil.example/", "https://www.peelink2.com.evil.example/", "https://user@www.peelink2.com/", "https://www.peelink2.com:8443/", "https://www.peelink2.com//evil", "https://www.peelink2.com/%2f%2fevil.example", "https://www.peelink2.com/%0d%0aX", "/inicio").forEach {
            assertNull(it, peelinkProxyUrl(it))
        }
    }
}
