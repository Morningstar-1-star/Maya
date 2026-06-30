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
import java.io.ByteArrayInputStream

class MediaCaptureInterface(
    private val pageTitleProvider: () -> String,
    private val pageUrlProvider: () -> String,
    private val onCapture: (url: String, type: String, title: String, pageUrl: String) -> Unit,
    private val onLinkLongPress: (url: String, text: String) -> Unit
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
}

// Global cache of WebViews per Tab ID to ensure instant switching and state preservation
object WebViewPool {
    private val webViews = mutableMapOf<Long, WebView>()

    fun getOrCreateWebView(context: Context, tabId: Long, viewModel: BrowserViewModel): WebView {
        return webViews.getOrPut(tabId) {
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                addJavascriptInterface(
                    MediaCaptureInterface(
                        pageTitleProvider = { this.title ?: "Website" },
                        pageUrlProvider = { this.url ?: "" },
                        onCapture = { url, type, title, pageUrl ->
                            viewModel.captureMedia(url, type, title, pageUrl)
                        },
                        onLinkLongPress = { url, text ->
                            viewModel.showLinkContextMenu(url, text)
                        }
                    ),
                    "MediaCaptureInterface"
                )
                setOnLongClickListener {
                    val hitTest = hitTestResult
                    val type = hitTest.type
                    val extra = hitTest.extra
                    if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                        if (extra != null) {
                            viewModel.showLinkContextMenu(extra, "")
                        }
                        true
                    } else {
                        false
                    }
                }
                setupSettings(this)
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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupSettings(webView: WebView) {
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
            cacheMode = WebSettings.LOAD_DEFAULT
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)
        }
    }

    private fun setupClients(webView: WebView, tabId: Long, viewModel: BrowserViewModel) {
        webView.webViewClient = object : WebViewClient() {
            private fun isAdRequest(urlStr: String): Boolean {
                return AdBlocker.isAdRequest(urlStr)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val host = request.url?.host ?: ""
                
                // 1. Prevent App Redirects if option is enabled
                if (viewModel.stopAppRedirects.value) {
                    if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:")) {
                        // This is an app scheme (e.g. market://, intent://, whatsapp://, telegram://)
                        // Block it from redirecting!
                        return true
                    }
                }
                
                // 2. Prevent ad and redirect host navigation
                if (viewModel.adBlockerOn.value && isAdRequest(url)) {
                    view?.post {
                        viewModel.incrementBlockedAds(tabId)
                    }
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

                if (adBlockOn && isAdRequest(url)) {
                    // Increment count in UI thread safely
                    view?.post {
                        viewModel.incrementBlockedAds(tabId)
                    }
                    // Return empty response to block the ad!
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream("".toByteArray())
                    )
                }

                // --- Custom DNS Blocking Interceptor ---
                val dnsEnabled = viewModel.dnsEnabled.value
                val host = request.url?.host ?: ""
                if (dnsEnabled && host.isNotEmpty()) {
                    val dnsMode = viewModel.dnsMode.value
                    val dnsPresetId = viewModel.dnsPresetId.value
                    val dnsCustomValue = viewModel.dnsCustomValue.value
                    
                    if (DnsManager.shouldBlockDomain(host, dnsEnabled, dnsMode, dnsPresetId, dnsCustomValue)) {
                        view?.post {
                            viewModel.incrementBlockedAds(tabId)
                        }
                        return WebResourceResponse(
                            "text/plain",
                            "UTF-8",
                            ByteArrayInputStream("".toByteArray())
                        )
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                if (url != null) {
                    val lower = url.lowercase()
                    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
                        lower.endsWith(".webp") || lower.endsWith(".gif")
                    ) {
                        viewModel.captureMedia(
                            url = url,
                            type = "image",
                            pageTitle = view?.title ?: "Website Image",
                            pageUrl = view?.url ?: ""
                        )
                    } else if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.contains(".m3u8")) {
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
                viewModel.updateNavigationState(
                    tabId = tabId,
                    canGoBack = view?.canGoBack() ?: false,
                    canGoForward = view?.canGoForward() ?: false
                )
                if (url != null) {
                    viewModel.updateTabTitleAndUrl(tabId, view?.title ?: "Browser Tab", url)
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

                        // Scan images on load
                        var imgs = document.getElementsByTagName('img');
                        for (var i = 0; i < imgs.length; i++) {
                            reportImage(imgs[i].src);
                        }

                        // Scan background images
                        var all = document.getElementsByTagName('*');
                        for (var i = 0; i < all.length; i++) {
                            var bg = window.getComputedStyle(all[i]).backgroundImage;
                            if (bg && bg !== 'none') {
                                var match = bg.match(/url\((['"]?)(.*?)\1\)/);
                                if (match && match[2]) {
                                    reportImage(match[2]);
                                }
                            }
                        }

                        // Scan videos on load
                        var vids = document.getElementsByTagName('video');
                        for (var i = 0; i < vids.length; i++) {
                            var src = vids[i].src || (vids[i].getElementsByTagName('source')[0] && vids[i].getElementsByTagName('source')[0].src);
                            reportVideo(src);
                        }

                        // MutationObserver for infinite scroll pages like Insta / ArtStation
                        var observer = new MutationObserver(function(mutations) {
                            ${if (adBlockOn) "try { applyCosmeticAdBlock(); } catch(e) {}" else ""}
                            mutations.forEach(function(mutation) {
                                mutation.addedNodes.forEach(function(node) {
                                    if (node.tagName === 'IMG') {
                                        reportImage(node.src);
                                    } else if (node.tagName === 'VIDEO') {
                                        var src = node.src || (node.getElementsByTagName('source')[0] && node.getElementsByTagName('source')[0].src);
                                        reportVideo(src);
                                    } else if (node.getElementsByTagName) {
                                        var nestedImgs = node.getElementsByTagName('img');
                                        for (var j = 0; j < nestedImgs.length; j++) {
                                            reportImage(nestedImgs[j].src);
                                        }
                                        var nestedVids = node.getElementsByTagName('video');
                                        for (var j = 0; j < nestedVids.length; j++) {
                                            var src = nestedVids[j].src || (nestedVids[j].getElementsByTagName('source')[0] && nestedVids[j].getElementsByTagName('source')[0].src);
                                            reportVideo(src);
                                        }
                                    }
                                });
                            });
                        });
                        observer.observe(document.body, { childList: true, subtree: true });
                    })();
                """.trimIndent()
                view?.evaluateJavascript(script, null)
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
                if (viewModel.adBlockerOn.value) {
                    if (!isUserGesture) {
                        return true // Intercept and block non-user-initiated popups!
                    }
                }
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
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

    // Load URL if the web view's URL is empty or is different from the target
    LaunchedEffect(url) {
        if (url != "dineinstyle.com") {
            val currentWebUrl = webView.url ?: ""
            if (currentWebUrl != url) {
                webView.loadUrl(url)
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            // 1. Text zoom
            view.settings.textZoom = (webTextZoom * 100).toInt()
            
            // 2. Force Dark Mode for Webpages via CSS injection
            val darkScript = if (forceDarkWebpages) {
                """
                (function() {
                    if (!document.getElementById('force-dark-style')) {
                        var style = document.createElement('style');
                        style.id = 'force-dark-style';
                        style.innerHTML = `
                            html {
                                filter: invert(1) hue-rotate(180deg) !important;
                                background-color: #121212 !important;
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
            view.evaluateJavascript(darkScript, null)

            // 3. Page Zoom Level via CSS body style injection
            val zoomScript = """
                (function() {
                    document.body.style.zoom = "$webZoomLevel";
                })()
            """.trimIndent()
            view.evaluateJavascript(zoomScript, null)

            // 4. Hide Distracting Items script
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
            view.evaluateJavascript(distractScript, null)
        }
    )
}
