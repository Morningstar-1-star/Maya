package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.viewmodel.BrowserViewModel
import com.example.data.DnsManager
import com.example.data.AdBlocker
import com.example.data.UserScript
import java.io.ByteArrayInputStream

class MediaCaptureInterface(
    private val pageTitleProvider: () -> String,
    private val pageUrlProvider: () -> String,
    private val onCapture: (url: String, type: String, title: String, pageUrl: String) -> Unit,
    private val onLinkLongPress: (url: String, text: String) -> Unit,
    private val onVideoIconClicked: (url: String, title: String) -> Unit,
    private val onElementLongPress: ((selector: String, html: String) -> Unit)? = null,
    private val onAdBlocked: (() -> Unit)? = null
) {
    @android.webkit.JavascriptInterface
    fun onImageFound(url: String) {
        onCapture(url, "image", pageTitleProvider(), pageUrlProvider())
    }

    @android.webkit.JavascriptInterface
    fun onVideoFound(url: String) {
        onCapture(url, "video", pageTitleProvider(), pageUrlProvider())
    }

    @android.webkit.JavascriptInterface
    fun onLinkLongPressed(url: String, text: String) {
        onLinkLongPress(url, text)
    }

    @android.webkit.JavascriptInterface
    fun onVideoIconClicked(url: String, title: String) {
        onVideoIconClicked(url, title)
    }

    @android.webkit.JavascriptInterface
    fun onElementLongPressed(selector: String, html: String) {
        onElementLongPress?.invoke(selector, html)
    }

    @android.webkit.JavascriptInterface
    fun onAdBlockedQuietly() {
        onAdBlocked?.invoke()
    }
}

// Global cache of WebViews per Tab ID to ensure instant switching and state preservation
object WebViewPool {
    private val webViews = mutableMapOf<Long, WebView>()

    fun getWebView(tabId: Long): WebView? {
        return webViews[tabId]
    }

    fun getOrCreateWebView(context: Context, tabId: Long, viewModel: BrowserViewModel): WebView {
        return webViews.getOrPut(tabId) {
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                val tabStateProvider = {
                    viewModel.allTabs.value.find { it.id == tabId }
                }
                addJavascriptInterface(
                    MediaCaptureInterface(
                        pageTitleProvider = { tabStateProvider()?.title ?: "Website" },
                        pageUrlProvider = { tabStateProvider()?.url ?: "" },
                        onCapture = { url, type, title, pageUrl ->
                            viewModel.captureMedia(url, type, title, pageUrl)
                        },
                        onLinkLongPress = { url, text ->
                            post {
                                viewModel.showLinkContextMenu(url, text)
                            }
                        },
                        onVideoIconClicked = { url, title ->
                            post {
                                val finalUrl = if (url.isBlank() || url.startsWith("blob:")) {
                                    val lastVid = viewModel.allCapturedMedia.value.lastOrNull { it.type == "video" }
                                    lastVid?.url ?: url
                                } else {
                                    url
                                }
                                if (finalUrl.isNotBlank()) {
                                    viewModel.setDetectedVideoActionMedia(
                                        com.example.data.CapturedMedia(
                                            url = finalUrl,
                                            type = "video",
                                            pageTitle = title,
                                            pageUrl = tabStateProvider()?.url ?: ""
                                        )
                                    )
                                } else {
                                    android.widget.Toast.makeText(context, "Detecting video stream... Please play the video first", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onElementLongPress = { selector, html ->
                            post {
                                viewModel.showBlockElementConfirm(selector, html)
                            }
                        },
                        onAdBlocked = {
                            post {
                                viewModel.incrementBlockedAds(tabId)
                            }
                        }
                    ),
                    "MediaCaptureInterface"
                )
                setOnLongClickListener {
                    val hitTest = hitTestResult
                    val type = hitTest.type
                    val extra = hitTest.extra
                    if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                        if (extra != null) {
                            viewModel.setLongPressedImage(extra, title = title ?: "Image", pageUrl = url ?: "")
                            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        }
                        true
                    } else if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                        if (extra != null) {
                            viewModel.showLinkContextMenu(extra, "")
                        }
                        true
                    } else {
                        false
                    }
                }
                setupSettings(this, viewModel)
                setupClients(this, tabId, viewModel)
            }
        }
    }

    fun removeWebView(tabId: Long) {
        val webView = webViews.remove(tabId)
        webView?.apply {
            stopLoading()
            clearHistory()
            destroy()
        }
    }

    fun clearAll() {
        webViews.keys.toList().forEach { removeWebView(it) }
    }

    fun onTabSelected(activeTabId: Long) {
        webViews.forEach { (id, wv) ->
            if (id == activeTabId) {
                wv.onResume()
            } else {
                wv.onPause()
            }
        }
    }

    fun onAppPause() {
        webViews.values.forEach { it.onPause() }
    }

    fun onAppResume(activeTabId: Long?) {
        if (activeTabId != null) {
            webViews[activeTabId]?.onResume()
        } else {
            webViews.values.firstOrNull()?.onResume()
        }
    }

    fun trimInactiveMemory(activeTabId: Long?, maxRetained: Int = 6) {
        if (webViews.size <= maxRetained) return
        val candidates = webViews.keys.filter { it != activeTabId }
        val toRemove = candidates.take(webViews.size - maxRetained)
        toRemove.forEach { removeWebView(it) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupSettings(webView: WebView, viewModel: BrowserViewModel) {
        // Fast, buttery-smooth scrolling (60/120Hz)
        webView.overScrollMode = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = false
        webView.scrollBarStyle = android.view.View.SCROLLBARS_INSIDE_OVERLAY
        webView.isScrollbarFadingEnabled = true

        // Full hardware rendering acceleration
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        @Suppress("DEPRECATION")
        webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            
            // Ultra-smooth 60/120Hz rasterization & tile processing
            offscreenPreRaster = true
            
            // Fast cache for rapid instant navigation
            cacheMode = if (viewModel.lowPowerModeEnabled.value) {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            } else {
                WebSettings.LOAD_DEFAULT
            }
            
            // Smooth transitions and rendering optimizations
            setEnableSmoothTransition(true)
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            
            // Prevent unprompted autoplaying videos from lagging initial page loads
            mediaPlaybackRequiresUserGesture = true
            
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)
            allowFileAccess = false
            allowContentAccess = true
        }
    }

    private fun setupClients(webView: WebView, tabId: Long, viewModel: BrowserViewModel) {
        val emptyBytes = ByteArray(0)
        fun emptyTextResponse() = WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(emptyBytes))
        fun emptyImageResponse() = WebResourceResponse("image/png", "UTF-8", ByteArrayInputStream(emptyBytes))
        val imagePattern = java.util.regex.Pattern.compile("(?i)\\.(jpg|jpeg|png|gif|webp|svg|bmp|ico)(\\?.*)?$")
        val pendingBlockedCounter = java.util.concurrent.atomic.AtomicInteger(0)

        fun reportBlockedAd(view: WebView?) {
            if (pendingBlockedCounter.incrementAndGet() == 1) {
                view?.postDelayed({
                    val count = pendingBlockedCounter.getAndSet(0)
                    if (count > 0) {
                        viewModel.incrementBlockedAdsBy(tabId, count)
                    }
                }, 250)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            private fun isAdRequest(urlStr: String): Boolean {
                val mainUrl = viewModel.allTabs.value.find { it.id == tabId }?.url ?: ""
                val mainHost = if (mainUrl.isNotEmpty()) {
                    try {
                        android.net.Uri.parse(mainUrl).host?.lowercase() ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                } else {
                    ""
                }
                if (mainHost.isNotEmpty() && viewModel.isHostWhitelisted(mainHost)) {
                    return false // Ads are allowed on this page!
                }
                return AdBlocker.isAdRequest(urlStr)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val host = request.url?.host ?: ""
                
                // 0. Magnet Link Interception
                if (com.example.data.MagnetTorrentManager.isMagnetUrl(url)) {
                    view?.post {
                        com.example.data.MagnetTorrentManager.activeMagnetRequest.value =
                            com.example.data.MagnetTorrentManager.parseMagnetUri(url)
                    }
                    return true
                }

                // 0b. Tor .onion Detection & Automation
                if (com.example.network.ProxyTorManager.isOnionUrl(url)) {
                    val currentProxy = com.example.network.ProxyTorManager.currentMode.value
                    val isTorActive = currentProxy == com.example.network.ProxyMode.TOR_ORBOT || currentProxy == com.example.network.ProxyMode.TOR_LOCAL
                    if (!isTorActive && com.example.network.ProxyTorManager.autoRouteOnion.value) {
                        view?.post {
                            viewModel.setPendingOnionUrl(url)
                        }
                        return true
                    }
                }

                // 0c. URL Tracker Stripping (only on main frame navigation)
                if (request?.isForMainFrame == true) {
                    val (cleanedUrl, stripped) = com.example.data.PrivacyShieldManager.cleanTrackingParameters(url)
                    if (cleanedUrl != url) {
                        view?.post {
                            view.loadUrl(cleanedUrl)
                        }
                        return true
                    }
                }

                // 1. Prevent App Redirects if option is enabled
                if (viewModel.stopAppRedirects.value) {
                    if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:")) {
                        // This is an app scheme (e.g. market://, intent://, whatsapp://, telegram://)
                        return true
                    }
                }

                // 2. Custom Blocked Links
                if (viewModel.isLinkBlocked(url)) {
                    reportBlockedAd(view)
                    return true
                }
                
                // 3. Prevent ad and redirect host navigation
                if (viewModel.adBlockerOn.value && isAdRequest(url)) {
                    reportBlockedAd(view)
                    return true // Block navigation!
                }
                
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                val adBlockOn = viewModel.adBlockerOn.value

                // 1. Custom Blocked Links
                if (viewModel.isLinkBlocked(url)) {
                    reportBlockedAd(view)
                    return emptyTextResponse()
                }

                // 2. Custom Blocked Images Pattern & General Image Block
                val isImage = imagePattern.matcher(url).find() || request.requestHeaders?.get("Accept")?.contains("image") == true
                if (isImage) {
                    if (viewModel.blockedImagesEnabled.value || viewModel.isImageBlocked(url)) {
                        return emptyImageResponse()
                    }
                }

                // 3. Ad Blocker
                if (adBlockOn && isAdRequest(url)) {
                    reportBlockedAd(view)
                    return emptyTextResponse()
                }

                // 4. Paywall Script Bypasser
                if (viewModel.bypassPaywallsEnabled.value) {
                    val lowUrl = url.lowercase()
                    if (lowUrl.contains("tinypass.com") || lowUrl.contains("piano.io") || lowUrl.contains("poool.fr") || lowUrl.contains("outbrain.com") || lowUrl.contains("paywall") || lowUrl.contains("gatekeeper")) {
                        return emptyTextResponse()
                    }
                }

                // --- Custom DNS Blocking Interceptor ---
                val dnsEnabled = viewModel.dnsEnabled.value
                val host = request.url?.host ?: ""
                if (dnsEnabled && host.isNotEmpty()) {
                    val dnsMode = viewModel.dnsMode.value
                    val dnsPresetId = viewModel.dnsPresetId.value
                    val dnsCustomValue = viewModel.dnsCustomValue.value
                    
                    if (DnsManager.shouldBlockDomain(host, dnsEnabled, dnsMode, dnsPresetId, dnsCustomValue)) {
                        reportBlockedAd(view)
                        return emptyTextResponse()
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                if (url != null) {
                    val lower = url.lowercase()
                    // Fast detection of real video streams; skips spamming SQLite with every site icon/button
                    if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.contains(".m3u8")) {
                        viewModel.captureMedia(
                            url = url,
                            type = "video",
                            pageTitle = view?.title ?: "Website Video",
                            pageUrl = view?.url ?: ""
                        )
                    }
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                viewModel.recordPageStart(tabId)
                if (view != null && url != null) {
                    com.example.ui.CookiePersistence.restoreCookiesForUrl(view.context, url)
                }
                // Inject anti-fingerprinting shield early
                view?.evaluateJavascript(com.example.data.PrivacyShieldManager.getAntiFingerprintingScript(), null)
                viewModel.updateNavigationState(
                    tabId = tabId,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )
                if (url != null) {
                    viewModel.updateTabTitleAndUrl(tabId, view?.title ?: url, url)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                if (failingUrl != null && viewModel.smartAutoRouting.value && !failingUrl.contains("croxyproxy") && !failingUrl.contains("proxysite") && !failingUrl.contains("localhost") && failingUrl != "dineinstyle.com") {
                    val encoded = try {
                        java.net.URLEncoder.encode(failingUrl, "UTF-8")
                    } catch (e: Exception) {
                        failingUrl
                    }
                    val proxiedUrl = "https://www.croxyproxy.com/_en/play?u=$encoded"
                    view?.post {
                        view.loadUrl(proxiedUrl)
                        viewModel.updateRoutingStatus("Fallback Proxy Active")
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val failingUrl = request?.url?.toString()
                val isMainFrame = request?.isForMainFrame ?: false
                if (isMainFrame && failingUrl != null && viewModel.smartAutoRouting.value && !failingUrl.contains("croxyproxy") && !failingUrl.contains("proxysite") && !failingUrl.contains("localhost") && failingUrl != "dineinstyle.com") {
                    val encoded = try {
                        java.net.URLEncoder.encode(failingUrl, "UTF-8")
                    } catch (e: Exception) {
                        failingUrl
                    }
                    val proxiedUrl = "https://www.croxyproxy.com/_en/play?u=$encoded"
                    view?.post {
                        view.loadUrl(proxiedUrl)
                        viewModel.updateRoutingStatus("Fallback Proxy Active")
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.recordPageFinished(tabId)
                viewModel.updateNavigationState(
                    tabId = tabId,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )

                // Calculate Read Time on Every Page load dynamically
                view?.evaluateJavascript(
                    "(function() { " +
                    "   var text = document.body ? document.body.innerText : ''; " +
                    "   var words = text.trim().split(/\\s+/).filter(function(w) { return w.length > 0; }).length; " +
                    "   return words; " +
                    "})()"
                ) { result ->
                    val wordCount = result?.toIntOrNull() ?: 0
                    viewModel.updateTabReadTime(tabId, wordCount)
                }

                // Injected Javascript paywall bypassed elements remover
                if (viewModel.bypassPaywallsEnabled.value) {
                    val paywallScript = """
                        (function() {
                            setTimeout(function() {
                                document.documentElement.style.overflow = 'auto';
                                document.body.style.overflow = 'auto';
                                document.body.style.position = 'static';
                                var list = document.querySelectorAll('[class*="paywall"], [id*="paywall"], [class*="gate"], [class*="subscription"], .tp-modal, .tp-backdrop');
                                for (var i = 0; i < list.length; i++) {
                                    list[i].remove();
                                }
                            }, 1000);
                        })()
                    """.trimIndent()
                    view?.evaluateJavascript(paywallScript, null)
                }

                if (url != null) {
                    viewModel.updateTabTitleAndUrl(tabId, view?.title ?: "Browser Tab", url)

                    // Website Evolution - "What's Changed?"
                    val bookmark = viewModel.allBookmarks.value.find { it.url == url }
                    if (bookmark != null && bookmark.isWatchMode) {
                        val extractScript = """
                            (function() {
                                var items = [];
                                var els = document.querySelectorAll('p, h1, h2, h3, h4, h5, li');
                                for (var i = 0; i < els.length; i++) {
                                    var txt = els[i].innerText.trim();
                                    if (txt.length > 15) {
                                        items.push(txt);
                                    }
                                }
                                return JSON.stringify(items);
                            })()
                        """.trimIndent()
                        view?.evaluateJavascript(extractScript) { result ->
                            if (result != null && result != "null" && result.isNotBlank()) {
                                try {
                                    val cleanJsonStr = if (result.startsWith("\"") && result.endsWith("\"") && result.length > 2) {
                                        org.json.JSONTokener(result).nextValue() as String
                                    } else {
                                        result
                                    }
                                    val jsonArray = org.json.JSONArray(cleanJsonStr)
                                    val hashes = mutableListOf<String>()
                                    for (i in 0 until jsonArray.length()) {
                                        val txt = jsonArray.getString(i)
                                        hashes.add(txt.hashCode().toString())
                                    }

                                    val savedHashesJson = bookmark.lastTextHash
                                    if (savedHashesJson.isNullOrBlank()) {
                                        val jsonStr = org.json.JSONArray(hashes).toString()
                                        viewModel.updateBookmarkTextHash(url, jsonStr)
                                    } else {
                                        val savedArray = org.json.JSONArray(savedHashesJson)
                                        val savedSet = mutableSetOf<String>()
                                        for (i in 0 until savedArray.length()) {
                                            savedSet.add(savedArray.getString(i))
                                        }

                                        val newIndices = mutableListOf<Int>()
                                        for (i in 0 until hashes.size) {
                                            if (!savedSet.contains(hashes[i])) {
                                                newIndices.add(i)
                                            }
                                        }

                                        if (newIndices.isNotEmpty()) {
                                            val indexArrayStr = newIndices.joinToString(",")
                                            val highlightScript = """
                                                (function() {
                                                    var indices = [$indexArrayStr];
                                                    var els = document.querySelectorAll('p, h1, h2, h3, h4, h5, li');
                                                    var count = 0;
                                                    var actualIdx = 0;
                                                    for (var i = 0; i < els.length; i++) {
                                                        var txt = els[i].innerText.trim();
                                                        if (txt.length > 15) {
                                                            if (indices.indexOf(actualIdx) !== -1) {
                                                                els[i].style.backgroundColor = '#d1fae5';
                                                                els[i].style.borderLeft = '4px solid #10b981';
                                                                els[i].style.paddingLeft = '8px';
                                                                els[i].style.transition = 'all 0.5s ease';
                                                                count++;
                                                            }
                                                            actualIdx++;
                                                        }
                                                    }
                                                    return count;
                                                })()
                                            """.trimIndent()
                                            view?.evaluateJavascript(highlightScript) { countResult ->
                                                val highlightedCount = countResult?.toIntOrNull() ?: 0
                                                if (highlightedCount > 0) {
                                                    viewModel.setWebsiteEvolutionAlert(url, highlightedCount)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                // Inject Copy Unblocker if active for this URL
                if (viewModel.isCopyUnblockActiveForUrl(url)) {
                    val unblockScript = """
                        (function() {
                            try {
                                if (!document.getElementById('enable-copy-unblock-styles')) {
                                    var style = document.createElement('style');
                                    style.id = 'enable-copy-unblock-styles';
                                    style.innerHTML = '* { -webkit-user-select: text !important; -moz-user-select: text !important; -ms-user-select: text !important; user-select: text !important; }';
                                    document.head.appendChild(style);
                                }
                                var eventsToBypass = ['contextmenu', 'copy', 'cut', 'paste', 'selectstart', 'dragstart'];
                                eventsToBypass.forEach(function(eventName) {
                                    document.addEventListener(eventName, function(e) {
                                        e.stopPropagation();
                                    }, true);
                                });
                                function clearBlockers() {
                                    var targets = [document, document.body, document.documentElement];
                                    var inlineHandlers = ['oncontextmenu', 'oncopy', 'oncut', 'onpaste', 'onselectstart', 'ondragstart'];
                                    targets.forEach(function(target) {
                                        if (target) {
                                            inlineHandlers.forEach(function(handler) {
                                                if (target[handler] !== null) {
                                                    target[handler] = null;
                                                }
                                            });
                                        }
                                    });
                                }
                                clearBlockers();
                                setInterval(clearBlockers, 2000);
                            } catch (e) {}
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(unblockScript, null)
                }

                // Evaluate enabled User Scripts
                val scripts = viewModel.allUserScripts.value
                val pageUrl = url ?: ""
                scripts.forEach { script ->
                    if (script.isEnabled) {
                        val matchPattern = script.matchUrl.trim()
                        val shouldRun = if (matchPattern == "*" || matchPattern.isBlank()) {
                            true
                        } else {
                            val cleanPattern = matchPattern.lowercase().replace("*", "")
                            pageUrl.lowercase().contains(cleanPattern)
                        }
                        if (shouldRun) {
                            view?.evaluateJavascript(script.code, null)
                        }
                    }
                }

                // Theme Color Extraction
                val themeColorScript = """
                    (function() {
                        var meta = document.querySelector('meta[name="theme-color"]');
                        if (meta && meta.content) {
                            return meta.content;
                        }
                        var bg = window.getComputedStyle(document.body).backgroundColor;
                        return bg;
                    })()
                """.trimIndent()
                view?.evaluateJavascript(themeColorScript) { result ->
                    if (result != null && result != "null" && result.isNotBlank()) {
                        viewModel.updateWebsiteThemeColor(result)
                    }
                }

                // Inject Reality Filters
                val clickbait = viewModel.realityClickbaitFilter.value
                val sponsored = viewModel.realitySponsoredBlock.value
                val aiBadge = viewModel.realityAiBadge.value
                val aiSmartAdBlock = viewModel.aiSmartAdBlockerEnabled.value
                if (clickbait || sponsored || aiBadge || aiSmartAdBlock) {
                    val realityFiltersScript = """
                        (function() {
                            // 1. Clickbait Filter
                            if ($clickbait) {
                                const clickbaitWords = ['you won\\\'t believe', 'shocking truth', 'secret to', 'what happens next', 'will blow your mind', 'this is why', 'destroy your', 'ultimate guide to'];
                                document.querySelectorAll('a, h1, h2, h3, h4, h5, p, span').forEach(el => {
                                    const text = el.innerText ? el.innerText.toLowerCase() : '';
                                    if (clickbaitWords.some(word => text.includes(word))) {
                                        el.style.opacity = '0.15';
                                        el.style.transition = 'opacity 0.5s';
                                        el.title = 'Clickbait filter minimized this element';
                                        el.addEventListener('mouseover', () => el.style.opacity = '1');
                                        el.addEventListener('mouseout', () => el.style.opacity = '0.15');
                                    }
                                });
                            }

                            // 2. Sponsored Content Blocker
                            if ($sponsored) {
                                const sponsoredSelectors = [
                                    '.sponsored-post', '.promoted-content', '[class*="sponsored" i]', '[id*="sponsored" i]',
                                    '[class*="promoted" i]', '[id*="promoted" i]', '[class*="advertisement" i]', '[id*="advertisement" i]'
                                ];
                                sponsoredSelectors.forEach(selector => {
                                    try {
                                        document.querySelectorAll(selector).forEach(el => {
                                            el.style.display = 'none';
                                        });
                                    } catch(e) {}
                                });
                                document.querySelectorAll('span, div, p, a').forEach(el => {
                                    if (el.innerText && (el.innerText === 'Sponsored' || el.innerText === 'Promoted' || el.innerText === 'Advertisement')) {
                                        let parent = el.parentElement;
                                        if (parent) {
                                            parent.style.opacity = '0.1';
                                            parent.style.transition = 'opacity 0.3s';
                                        }
                                    }
                                });
                            }

                            // 3. AI-Generated Badge Finder
                            if ($aiBadge) {
                                const aiPhrases = ['as an ai language model', 'it is important to remember', 'dive deep into', 'delve into', 'testament to', 'not only... but also', 'it is worth noting'];
                                let score = 0;
                                const text = document.body ? document.body.innerText.toLowerCase() : '';
                                aiPhrases.forEach(phrase => {
                                    if (text.includes(phrase)) score++;
                                });
                                if (score >= 2 && !document.getElementById('ai-detection-badge')) {
                                    const badge = document.createElement('div');
                                    badge.id = 'ai-detection-badge';
                                    badge.innerHTML = '🤖 Potential AI Content Detected';
                                    badge.style.position = 'fixed';
                                    badge.style.top = '10px';
                                    badge.style.right = '10px';
                                    badge.style.backgroundColor = '#6366F1';
                                    badge.style.color = 'white';
                                    badge.style.padding = '6px 12px';
                                    badge.style.borderRadius = '20px';
                                    badge.style.fontSize = '12px';
                                    badge.style.fontWeight = 'bold';
                                    badge.style.zIndex = '999999';
                                    badge.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';
                                    badge.style.cursor = 'pointer';
                                    badge.onclick = function() { badge.remove(); };
                                    document.body.appendChild(badge);
                                }
                            }

                            // 4. AI Heuristic AdBlock Scanner
                            if ($aiSmartAdBlock) {
                                const adKeywords = ['banner', 'advertisement', 'sponsored', 'marketing', 'promo', 'adsense', 'ad-slot', 'google-ads'];
                                document.querySelectorAll('div, iframe, section, ins').forEach(el => {
                                    let isAd = false;
                                    const classIdStr = (el.className + ' ' + el.id).toLowerCase();
                                    if (adKeywords.some(kw => classIdStr.includes(kw))) {
                                        isAd = true;
                                    }
                                    const rect = el.getBoundingClientRect();
                                    if (rect.width > 0 && rect.height > 0) {
                                        const ratio = rect.width / rect.height;
                                        if ((Math.abs(ratio - (728/90)) < 0.2 && rect.width > 300) ||
                                            (Math.abs(ratio - (300/250)) < 0.1 && rect.width > 150) ||
                                            (Math.abs(ratio - (160/600)) < 0.1 && rect.height > 300) ||
                                            (Math.abs(ratio - (320/50)) < 0.2 && rect.width > 200)) {
                                            isAd = true;
                                        }
                                    }
                                    if (isAd && el.style.display !== 'none') {
                                        el.style.setProperty('display', 'none', 'important');
                                        try {
                                            MediaCaptureInterface.onAdBlockedQuietly();
                                        } catch(e) {}
                                    }
                                });
                            }
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(realityFiltersScript, null)
                }

                // Inject custom de-clutter keyword spoiler filter
                val deClutterKeywords = viewModel.customDeClutterKeywords.value
                if (deClutterKeywords.isNotEmpty()) {
                    val keywordsArrayJson = org.json.JSONArray(deClutterKeywords).toString()
                    val declutterScript = """
                        (function() {
                            var keywords = $keywordsArrayJson;
                            var targets = document.querySelectorAll('div, p, article, section, h1, h2, h3, h4, h5, li, a');
                            targets.forEach(function(el) {
                                var text = el.innerText ? el.innerText.toLowerCase() : '';
                                if (keywords.some(function(word) { return text.includes(word); })) {
                                    el.style.display = 'none';
                                }
                            });
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(declutterScript, null)
                }

                // Inject custom blocked elements selectors
                val customSelectors = viewModel.customBlockedElements.value
                if (customSelectors.isNotEmpty()) {
                    val selectorsList = customSelectors.joinToString(", ")
                    val customSelectorsScript = """
                        (function() {
                            try {
                                if (!document.getElementById('cleaner-custom-style')) {
                                    var style = document.createElement('style');
                                    style.id = 'cleaner-custom-style';
                                    style.innerHTML = `${selectorsList.replace("`", "\\`")} { display: none !important; visibility: hidden !important; height: 0 !important; width: 0 !important; opacity: 0 !important; pointer-events: none !important; }`;
                                    document.head.appendChild(style);
                                }
                            } catch(e) {}
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(customSelectorsScript, null)
                }

                // Inject block area listeners
                val blockAreaEnabled = viewModel.blockAreaEnabled.value
                if (blockAreaEnabled) {
                    val blockAreaScript = """
                        (function() {
                            function getUniqueSelector(el) {
                                if (!el) return "";
                                if (el.id) return '#' + el.id;
                                var path = [];
                                while (el && el.nodeType === Node.ELEMENT_NODE) {
                                    var selector = el.nodeName.toLowerCase();
                                    if (el.className) {
                                        var classes = el.className.trim().split(/\s+/).filter(function(c) { return c.length > 0; });
                                        if (classes.length > 0) {
                                            selector += '.' + classes.join('.');
                                        }
                                    }
                                    var sibling = el;
                                    var nth = 1;
                                    while (sibling = sibling.previousElementSibling) {
                                        if (sibling.nodeName.toLowerCase() === el.nodeName.toLowerCase()) {
                                            nth++;
                                        }
                                    }
                                    if (nth > 1) {
                                        selector += ':nth-of-type(' + nth + ')';
                                    }
                                    path.unshift(selector);
                                    el = el.parentNode;
                                }
                                return path.join(' > ');
                            }

                            if (window.blockAreaListenerInjected === undefined) {
                                window.blockAreaListenerInjected = true;
                                window.addEventListener('contextmenu', function(e) {
                                    var el = e.target;
                                    if (el) {
                                        var linkEl = el;
                                        while (linkEl && linkEl.tagName !== 'A') {
                                            linkEl = linkEl.parentNode;
                                        }
                                        if (linkEl && linkEl.tagName === 'A') {
                                            return;
                                        }
                                        e.preventDefault();
                                        e.stopPropagation();
                                        var selector = getUniqueSelector(el);
                                        var html = el.outerHTML ? el.outerHTML.substring(0, 500) : "";
                                        window.MediaCaptureInterface.onElementLongPressed(selector, html);
                                    }
                                }, { capture: true });
                            }
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(blockAreaScript, null)
                }

                val adBlockOn = viewModel.adBlockerOn.value
                val cosmeticBlockScript = if (adBlockOn) {
                    """
                        // --- Powerful Cosmetic Ad & Cookie Banner Blocker ---
                        function applyCosmeticAdBlock() {
                            try {
                                if (!document.getElementById('ublock-cosmetic-style')) {
                                    var style = document.createElement('style');
                                    style.id = 'ublock-cosmetic-style';
                                    style.innerHTML = `
                                        iframe[id*="google_ads"], iframe[name*="google_ads"], iframe[src*="doubleclick"],
                                        div[class*="ad-box"], div[class*="ad-banner"], div[class*="sponsored"], 
                                        div[class*="promoted"], div[id*="ad-slot"], div[class*="banner-ad"],
                                        div[class*="overlay-ad"], div[class*="cookie-banner"], div[class*="popup-ad"],
                                        amp-ad, ins.adsbygoogle, div[class*="cookie-notice"], div[class*="cookiebar"],
                                        .ad-container, .ad-wrapper, .ad-placeholder, .sponsored-posts, .sponsored-links,
                                        div[id*="taboola"], div[class*="taboola"], div[id*="outbrain"],
                                        div[class*="cookie-consent"], #onetrust-consent-sdk, .cookie-consent-banner,
                                        .adsbygoogle, .ad-slot, .ad-unit, [data-ad-client], [data-ad-slot],
                                        div[class*="ad_box"], div[id*="ad_box"], div[class*="ad-inner"],
                                        div[class*="advertisement"], div[id*="advertisement"],
                                        aside[class*="ad"], aside[id*="ad"], .trc_rbox_container, .trc_rbox,
                                        div[class*="native-ad"], div[id*="native-ad"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                            opacity: 0 !important;
                                            height: 0 !important;
                                            width: 0 !important;
                                            pointer-events: none !important;
                                            max-height: 0 !important;
                                            max-width: 0 !important;
                                            margin: 0 !important;
                                            padding: 0 !important;
                                        }
                                    `;
                                    document.head.appendChild(style);
                                }

                                var badSelectors = [
                                    'iframe[id*="google_ads"]', 'iframe[name*="google_ads"]', 'iframe[src*="doubleclick"]',
                                    'ins.adsbygoogle', 'amp-ad', '.adsbygoogle', '.ad-slot', '.ad-unit',
                                    'div[class*="cookie-consent"]', '#onetrust-consent-sdk', '.cookie-consent-banner',
                                    'div[class*="cookie-banner"]', 'div[class*="cookie-notice"]', 'div[class*="cookiebar"]',
                                    'div[class*="popup-ad"]', 'div[class*="overlay-ad"]', '.trc_rbox_container',
                                    'div[class*="native-ad"]', 'div[id*="native-ad"]'
                                ];
                                badSelectors.forEach(function(sel) {
                                    var elms = document.querySelectorAll(sel);
                                    for (var i = 0; i < elms.length; i++) {
                                        elms[i].remove();
                                    }
                                });

                                if (document.body && (document.body.style.overflow === 'hidden' || document.documentElement.style.overflow === 'hidden')) {
                                    if (!document.querySelector('#onetrust-consent-sdk') && !document.querySelector('.cookie-consent-banner')) {
                                        document.body.style.setProperty('overflow', 'auto', 'important');
                                        document.documentElement.style.setProperty('overflow', 'auto', 'important');
                                    }
                                }
                            } catch(e) {}
                        }

                        applyCosmeticAdBlock();

                        var sweepCount = 0;
                        var sweepInterval = setInterval(function() {
                            applyCosmeticAdBlock();
                            sweepCount++;
                            if (sweepCount > 12) clearInterval(sweepInterval);
                        }, 500);
                    """.trimIndent()
                } else {
                    ""
                }

                // Inject dynamic media scanner
                val script = """
                    (function() {
                        // --- Powerful popup window and redirect prevention ---
                        try {
                            if (window.popupOverrideInjected === undefined) {
                                window.popupOverrideInjected = true;
                                
                                // Prevent alert/confirm/prompt loops
                                var alertLimit = 3;
                                var originalAlert = window.alert;
                                window.alert = function(msg) {
                                    if (alertLimit-- > 0) originalAlert(msg);
                                };
                                
                                var originalConfirm = window.confirm;
                                window.confirm = function(msg) {
                                    return alertLimit-- > 0 ? originalConfirm(msg) : false;
                                };

                                // Block window.open
                                var originalOpen = window.open;
                                window.open = function(url, name, specs, replace) {
                                    console.log("Blocked window.open popup: ", url);
                                    return null;
                                };
                                
                                // Prevent window.onbeforeunload scams
                                window.onbeforeunload = null;
                            }
                        } catch(e) {}

                        // Listen for link long clicks / contextmenu events
                        try {
                            if (window.longPressListenerInjected === undefined) {
                                window.longPressListenerInjected = true;
                                window.addEventListener('contextmenu', function(e) {
                                    var target = e.target;
                                    while (target && target.tagName !== 'A') {
                                        target = target.parentNode;
                                    }
                                    if (target && target.tagName === 'A') {
                                        var url = target.href;
                                        var text = target.innerText || target.textContent || "";
                                        text = text.trim();
                                        if (url) {
                                            window.MediaCaptureInterface.onLinkLongPressed(url, text);
                                        }
                                    }
                                });
                            }
                        } catch(e) {}

                        $cosmeticBlockScript

                        function reportImage(src) {
                            if (src && (src.startsWith('http://') || src.startsWith('https://'))) {
                                window.MediaCaptureInterface.onImageFound(src);
                            }
                        }
                        function reportVideo(src) {
                            if (src && (src.startsWith('http://') || src.startsWith('https://'))) {
                                window.MediaCaptureInterface.onVideoFound(src);
                            }
                        }

                        // Scan images on load (limited to top 15 for max smooth speed)
                        var imgs = document.getElementsByTagName('img');
                        var maxImgs = Math.min(imgs.length, 15);
                        for (var i = 0; i < maxImgs; i++) {
                            reportImage(imgs[i].src);
                        }

                        // Lightweight, high-performance HTML5 Video detection via native events
                        function handleVideoDetected(video) {
                            if (!video) return;
                            var src = video.currentSrc || video.src || (video.getElementsByTagName('source')[0] && video.getElementsByTagName('source')[0].src) || "";
                            if (src) {
                                reportVideo(src);
                            }
                        }

                        // Listen to play event during capture phase for instant zero-lag video detection
                        document.addEventListener('play', function(e) {
                            if (e.target && e.target.tagName === 'VIDEO') {
                                handleVideoDetected(e.target);
                            }
                        }, true);

                        // Scan existing videos once on page load without heavy DOM polling
                        var vids = document.getElementsByTagName('video');
                        for (var i = 0; i < vids.length; i++) {
                            handleVideoDetected(vids[i]);
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(script, null)

                if (url != null && view != null) {
                    view.evaluateJavascript(com.example.data.PrivacyShieldManager.getAntiFingerprintingScript(), null)
                    val domain = try {
                        val host = android.net.Uri.parse(url).host ?: ""
                        if (host.startsWith("www.")) host.substring(4) else host
                    } catch (e: Exception) {
                        ""
                    }
                    if (domain.isNotEmpty()) {
                        val mode = com.example.ui.BrowserFeaturesManager.siteDarkPref[domain] ?: "Auto"
                        com.example.ui.BrowserFeaturesManager.injectSmartDarkMode(view, mode)
                        com.example.ui.BrowserFeaturesManager.applySiteControls(view, domain)
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.updateLoadingProgress(tabId, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                val url = view?.url ?: ""
                if (title != null && url.isNotEmpty()) {
                    viewModel.updateTabTitleAndUrl(tabId, title, url)
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                val host = view?.url?.let { android.net.Uri.parse(it).host?.lowercase() } ?: ""
                if (viewModel.isPopupWhitelisted(host)) {
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                }

                val mode = viewModel.popupBlockerMode.value
                if (mode == "strong") {
                    return true // Block all popups!
                } else if (mode == "weak") {
                    if (!isUserGesture) {
                        return true // Intercept and block non-user-initiated popups!
                    }
                }
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                if (request == null) return
                val host = webView.url?.let {
                    try {
                        java.net.URI(it).host
                    } catch (e: Exception) {
                        null
                    }
                } ?: "website.com"
                val cleanDomain = if (host != null && host.startsWith("www.")) host.substring(4) else (host ?: "website.com")
                val resources = request.resources
                val context = webView.context

                // Map Android resources to our Permission Types
                val mappedTypes = mutableListOf<String>()
                resources.forEach { res ->
                    when (res) {
                        android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE -> mappedTypes.add("Camera")
                        android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE -> mappedTypes.add("Microphone")
                    }
                }

                if (mappedTypes.isEmpty()) {
                    request.grant(resources)
                    return
                }

                var allAllowed = true
                mappedTypes.forEach { type ->
                    val state = com.example.ui.BrowserFeaturesManager.getPermissionState(cleanDomain, type)
                    com.example.ui.BrowserFeaturesManager.logPermissionAccess(context, cleanDomain, type, state == "Allow")
                    if (state != "Allow") {
                        allAllowed = false
                    }
                }

                if (allAllowed) {
                    request.grant(resources)
                } else {
                    request.deny()
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                if (origin == null || callback == null) return
                val host = android.net.Uri.parse(origin).host ?: "website.com"
                val cleanDomain = if (host.startsWith("www.")) host.substring(4) else host
                val context = webView.context

                val state = com.example.ui.BrowserFeaturesManager.getPermissionState(cleanDomain, "Location")
                com.example.ui.BrowserFeaturesManager.logPermissionAccess(context, cleanDomain, "Location", state == "Allow")

                if (state == "Allow") {
                    callback.invoke(origin, true, false)
                } else {
                    callback.invoke(origin, false, false)
                }
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            try {
                val context = webView.context
                val filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype) ?: "downloaded_file"

                // Intercept torrent files for Magnet/Torrent inspector
                if (com.example.data.MagnetTorrentManager.isTorrentUrl(url) || mimetype == "application/x-bittorrent") {
                    com.example.data.MagnetTorrentManager.activeMagnetRequest.value =
                        com.example.data.ParsedMagnet(
                            rawUri = url,
                            displayName = filename,
                            infoHash = "",
                            trackers = emptyList(),
                            exactLength = if (contentLength > 0) contentLength else null
                        )
                }

                val uri = android.net.Uri.parse(url)
                val request = android.app.DownloadManager.Request(uri).apply {
                    setMimeType(mimetype)
                    addRequestHeader("User-Agent", userAgent)
                    val cookie = android.webkit.CookieManager.getInstance().getCookie(url)
                    if (cookie != null) {
                        addRequestHeader("Cookie", cookie)
                    }
                    setDescription("Downloading with extreme high-speed boost...")
                    setTitle(filename)
                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                        filename
                    )
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                val manager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                manager.enqueue(request)
                
                // Track download in Database
                val sizeString = if (contentLength > 0) {
                    val kb = contentLength / 1024
                    if (kb > 1024) "${kb / 1024} MB" else "$kb KB"
                } else {
                    "Unknown size"
                }
                viewModel.insertDownload(
                    com.example.data.DownloadEntry(
                        filename = filename,
                        url = url,
                        status = "Completed",
                        size = sizeString,
                        timestamp = System.currentTimeMillis()
                    )
                )

                android.widget.Toast.makeText(context, "Boosted download started: $filename", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(webView.context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun TabWebView(
    tabId: Long,
    url: String,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val webView = remember(tabId) {
        WebViewPool.getOrCreateWebView(context, tabId, viewModel)
    }

    val webZoomLevel by viewModel.webZoomLevel.collectAsState()
    val forceDarkWebpages by viewModel.forceDarkWebpages.collectAsState()
    val webTextZoom by viewModel.webTextZoom.collectAsState()
    val hideDistractingItems by viewModel.hideDistractingItems.collectAsState()
    val loadingProgressMap by viewModel.loadingProgressMap.collectAsState()
    val progress = remember(loadingProgressMap, tabId) { loadingProgressMap[tabId] ?: 100 }

    val currentDomain = remember(url) {
        try {
            val uri = java.net.URI(url)
            val host = uri.host ?: ""
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (e: Exception) {
            ""
        }
    }

    val perSitePrefsTrigger by viewModel.perSitePrefsTrigger.collectAsState()
    val clickbaitOn by viewModel.realityClickbaitFilter.collectAsState()
    val sponsoredOn by viewModel.realitySponsoredBlock.collectAsState()
    val aiBadgeOn by viewModel.realityAiBadge.collectAsState()

    val siteZoom = remember(currentDomain, perSitePrefsTrigger, webZoomLevel) {
        if (currentDomain.isNotBlank() && viewModel.getSiteZoom(currentDomain) != 1.0f) {
            viewModel.getSiteZoom(currentDomain)
        } else {
            webZoomLevel
        }
    }

    val siteForceDark = remember(currentDomain, perSitePrefsTrigger, forceDarkWebpages) {
        if (currentDomain.isNotBlank() && viewModel.getSiteForceDark(currentDomain)) {
            true
        } else {
            forceDarkWebpages
        }
    }

    // Load URL if the web view's URL is empty or is different from the target
    LaunchedEffect(url) {
        if (url != "dineinstyle.com") {
            val currentWebUrl = webView.url ?: ""
            if (currentWebUrl != url) {
                webView.loadUrl(url)
            }
        }
    }

    // Capture tab visual preview screenshots once when page finishes loading
    LaunchedEffect(tabId, progress) {
        if (progress == 100) {
            kotlinx.coroutines.delay(1000)
            TabThumbnailManager.captureThumbnail(context, tabId, webView)
        }
    }

    // Find on Page logic integrated with Android WebView native text finding
    val findOnPageActive by viewModel.findOnPageActive.collectAsState()
    val findOnPageQuery by viewModel.findOnPageQuery.collectAsState()
    val findOnPageTrigger by viewModel.findOnPageTrigger.collectAsState()

    LaunchedEffect(findOnPageActive, findOnPageQuery) {
        if (findOnPageActive) {
            if (findOnPageQuery.isNotEmpty()) {
                webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                    viewModel.updateFindOnPageMatches(activeMatchOrdinal, numberOfMatches)
                }
                webView.findAllAsync(findOnPageQuery)
            } else {
                webView.clearMatches()
                viewModel.updateFindOnPageMatches(0, 0)
            }
        } else {
            webView.clearMatches()
        }
    }

    LaunchedEffect(findOnPageTrigger) {
        findOnPageTrigger?.let { forward ->
            webView.findNext(forward)
            viewModel.clearFindOnPageTrigger()
        }
    }

    val isSplit = com.example.ui.BrowserFeaturesManager.splitTabStates[tabId]?.isSplit == true
    val isReaderActive = com.example.ui.BrowserFeaturesManager.readerActiveForTab[tabId] == true

    // 1. Text zoom
    LaunchedEffect(webTextZoom) {
        webView.settings.textZoom = (webTextZoom * 100).toInt()
    }

    // 2. Force Dark Mode for Webpages
    LaunchedEffect(siteForceDark, progress == 100) {
        val darkScript = if (siteForceDark) {
            """
            (function() {
                if (!document.getElementById('force-dark-style')) {
                    var style = document.createElement('style');
                    style.id = 'force-dark-style';
                    style.innerHTML = `
                        html, body, #page, .page, main, article, section, div:not([style*="background-image"]) {
                            filter: invert(1) hue-rotate(180deg) !important;
                            background-color: #000000 !important;
                            background: #000000 !important;
                            color: #FFFFFF !important;
                        }
                        img, video, iframe, canvas, [style*="background-image"] {
                            filter: invert(1) hue-rotate(180deg) !important;
                        }
                    `;
                    document.head.appendChild(style);
                }
            })()
            """.trimIndent()
        } else {
            """
            (function() {
                var style = document.getElementById('force-dark-style');
                if (style) style.remove();
            })()
            """.trimIndent()
        }
        webView.evaluateJavascript(darkScript, null)
    }

    // 3. Page Zoom Level
    LaunchedEffect(siteZoom, progress == 100) {
        val zoomScript = """
            (function() {
                if (document.body) {
                    document.body.style.zoom = "$siteZoom";
                }
            })()
        """.trimIndent()
        webView.evaluateJavascript(zoomScript, null)
    }

    // 4. Hide Distracting Items
    LaunchedEffect(hideDistractingItems, progress == 100) {
        val distractScript = if (hideDistractingItems) {
            """
            (function() {
                if (!document.getElementById('hide-distracting-style')) {
                    var style = document.createElement('style');
                    style.id = 'hide-distracting-style';
                    style.innerHTML = `
                        .floating-widget, .social-share, .newsletter-signup, .sidebar-ads,
                        [class*="newsletter"], [id*="newsletter"], [class*="signup-prompt"],
                        [class*="sticky-footer"], [class*="floating-buttons"], .share-buttons,
                        aside, .sidebar, #sidebar, .related-posts, .recommended-content,
                        [id*="comments"], [class*="comments"], .comment-section {
                            display: none !important;
                            visibility: hidden !important;
                        }
                    `;
                    document.head.appendChild(style);
                }
            })()
            """.trimIndent()
        } else {
            """
            (function() {
                var style = document.getElementById('hide-distracting-style');
                if (style) style.remove();
            })()
            """.trimIndent()
        }
        webView.evaluateJavascript(distractScript, null)
    }

    // 5. Dynamic Reality Filters (only run when page completes or toggles change)
    LaunchedEffect(clickbaitOn, sponsoredOn, aiBadgeOn, progress == 100) {
        if (progress == 100 || (!clickbaitOn && !sponsoredOn && !aiBadgeOn)) {
            val clickbait = clickbaitOn
            val sponsored = sponsoredOn
            val aiBadge = aiBadgeOn
            
            val realityFiltersScript = """
                (function() {
                    // 1. Clickbait Filter
                    if ($clickbait) {
                        const clickbaitWords = ['you won\'t believe', 'shocking truth', 'secret to', 'what happens next', 'will blow your mind', 'this is why', 'destroy your', 'ultimate guide to'];
                        document.querySelectorAll('a, h1, h2, h3, h4, h5, p, span').forEach(el => {
                            const text = el.innerText ? el.innerText.toLowerCase() : '';
                            if (clickbaitWords.some(word => text.includes(word))) {
                                el.style.opacity = '0.15';
                                el.style.transition = 'opacity 0.5s';
                                el.title = 'Clickbait filter minimized this element';
                                el.addEventListener('mouseover', () => el.style.opacity = '1');
                                el.addEventListener('mouseout', () => el.style.opacity = '0.15');
                            }
                        });
                    } else {
                        document.querySelectorAll('a, h1, h2, h3, h4, h5, p, span').forEach(el => {
                            if (el.style.opacity === '0.15' && el.title === 'Clickbait filter minimized this element') {
                                el.style.opacity = '1';
                            }
                        });
                    }

                    // 2. Sponsored Content Blocker
                    if ($sponsored) {
                        const sponsoredSelectors = [
                            '.sponsored-post', '.promoted-content', '[class*="sponsored" i]', '[id*="sponsored" i]',
                            '[class*="promoted" i]', '[id*="promoted" i]', '[class*="advertisement" i]', '[id*="advertisement" i]'
                        ];
                        sponsoredSelectors.forEach(selector => {
                            try {
                                document.querySelectorAll(selector).forEach(el => {
                                    el.style.display = 'none';
                                });
                            } catch(e) {}
                        });
                        document.querySelectorAll('span, div, p, a').forEach(el => {
                            if (el.innerText && (el.innerText === 'Sponsored' || el.innerText === 'Promoted' || el.innerText === 'Advertisement')) {
                                let parent = el.parentElement;
                                if (parent) {
                                    parent.style.opacity = '0.1';
                                    parent.style.transition = 'opacity 0.3s';
                                }
                            }
                        });
                    } else {
                        document.querySelectorAll('span, div, p, a').forEach(el => {
                            if (el.innerText && (el.innerText === 'Sponsored' || el.innerText === 'Promoted' || el.innerText === 'Advertisement')) {
                                let parent = el.parentElement;
                                if (parent && parent.style.opacity === '0.1') {
                                    parent.style.opacity = '1';
                                }
                            }
                        });
                    }

                    // 3. AI-Generated Badge Finder
                    if ($aiBadge) {
                        const aiPhrases = ['as an ai language model', 'it is important to remember', 'dive deep into', 'delve into', 'testament to', 'not only... but also', 'it is worth noting'];
                        let score = 0;
                        const text = document.body ? document.body.innerText.toLowerCase() : '';
                        aiPhrases.forEach(phrase => {
                            if (text.includes(phrase)) score++;
                        });
                        if (score >= 2 && !document.getElementById('ai-detection-badge')) {
                            const badge = document.createElement('div');
                            badge.id = 'ai-detection-badge';
                            badge.innerHTML = '🤖 Potential AI Content Detected';
                            badge.style.position = 'fixed';
                            badge.style.top = '10px';
                            badge.style.right = '10px';
                            badge.style.backgroundColor = '#0284C7';
                            badge.style.color = 'white';
                            badge.style.padding = '6px 12px';
                            badge.style.borderRadius = '20px';
                            badge.style.fontSize = '12px';
                            badge.style.fontWeight = 'bold';
                            badge.style.zIndex = '999999';
                            badge.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';
                            badge.style.cursor = 'pointer';
                            badge.onclick = function() { badge.remove(); };
                            document.body.appendChild(badge);
                        }
                    } else {
                        const badge = document.getElementById('ai-detection-badge');
                        if (badge) badge.remove();
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(realityFiltersScript, null)
        }
    }

    if (isReaderActive) {
        com.example.ui.SmartReaderView(
            tabId = tabId,
            viewModel = viewModel,
            modifier = modifier
        )
    } else if (isSplit) {
        com.example.ui.SplitScreenTabWebView(
            tabId = tabId,
            viewModel = viewModel,
            modifier = modifier
        )
    } else {
        androidx.compose.runtime.key(tabId) {
            AndroidView(
                factory = { ctx ->
                    androidx.swiperefreshlayout.widget.SwipeRefreshLayout(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                        addView(webView)
                        setOnRefreshListener {
                            webView.reload()
                        }
                    }
                },
                modifier = modifier.fillMaxSize(),
                update = { swipeRefreshLayout ->
                    if (progress >= 100 && swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.isRefreshing = false
                    }
                    if (siteForceDark) {
                        webView.setBackgroundColor(android.graphics.Color.BLACK)
                    } else {
                        webView.setBackgroundColor(android.graphics.Color.WHITE)
                    }
                }
            )
        }
    }
}
