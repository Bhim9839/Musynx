package com.nitya.musynx.app.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nitya.musynx.innertube.BotGuard
import com.nitya.musynx.innertube.Innertube
import com.nitya.musynx.app.service.YouTubeSessionManager
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

private class MusynxJsBridge(
    private val decipherRequests: ConcurrentHashMap<String, CompletableDeferred<String>>,
    private val poTokenRequests: ConcurrentHashMap<String, CompletableDeferred<String>>
) {
    @JavascriptInterface
    fun onDecipherResult(requestId: String, result: String) {
        decipherRequests.remove(requestId)?.complete(result)
    }

    @JavascriptInterface
    fun onPoTokenResult(requestId: String, poToken: String) {
        Log.d("Musynx-WebView", "Received poToken for request $requestId: $poToken")
        poTokenRequests.remove(requestId)?.complete(poToken)
        // Also update global token for legacy reasons or initial load
        if (requestId == "INITIAL") {
            YouTubeSessionManager.updateSession(poToken = poToken)
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("Musynx-WebView-JS", message)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView() {
    // Hold pending requests in state
    val decipherRequests = remember { ConcurrentHashMap<String, CompletableDeferred<String>>() }
    val poTokenRequests = remember { ConcurrentHashMap<String, CompletableDeferred<String>>() }

    AndroidView(
        modifier = Modifier.size(100.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                // Using a modern mobile User-Agent
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

                //Attach the explicitly defined bridge
                addJavascriptInterface(MusynxJsBridge(decipherRequests, poTokenRequests), "MusynxBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        Log.d("Musynx-WebView", "Page finished: $url")

                        // Ensure cookies are flushed to storage
                        CookieManager.getInstance().flush()
                        val cookies = CookieManager.getInstance().getCookie(url)

                        //Safer JavaScript evaluation with proper null checks
                        val extractSessionDataJs = """
                            (function() { 
                                const config = window.yt?.config_ || (window.ytcfg && ytcfg.getAll ? ytcfg.getAll() : {});
                                const visitorData = config.VISITOR_DATA || (window.ytcfg && ytcfg.get ? ytcfg.get('VISITOR_DATA') : null);
                                const apiKey = config.INNERTUBE_API_KEY || (window.ytcfg && ytcfg.get ? ytcfg.get('INNERTUBE_API_KEY') : null);
                                
                                MusynxBridge.log('JS: extracted visitorData: ' + visitorData);
                                return JSON.stringify({
                                    visitorData: visitorData,
                                    apiKey: apiKey
                                });
                            })();
                        """.trimIndent()

                        view.evaluateJavascript(extractSessionDataJs) { result ->
                            try {
                                val json = JSONObject(result.replace("\\\"", "\"").trim('"'))
                                val cleanVisitorData = json.optString("visitorData").takeIf { it != "null" }
                                val apiKey = json.optString("apiKey").takeIf { it != "null" }

                                if (!cleanVisitorData.isNullOrBlank()) {
                                    Log.d("Musynx-WebView", "Extracted VisitorData: $cleanVisitorData")

                                    YouTubeSessionManager.updateSession(
                                        visitorData = cleanVisitorData,
                                        cookies = cookies,
                                        decipher = { nParam ->
                                            val deferred = CompletableDeferred<String>()
                                            val requestId = "${System.currentTimeMillis()}_$nParam"
                                            decipherRequests[requestId] = deferred

                                            val decipherInvokeJs = """
                                                (function() {
                                                    if (typeof decipherNParam === 'function') {
                                                        decipherNParam('$nParam', '$requestId');
                                                    } else {
                                                        MusynxBridge.log('JS: decipherNParam not ready for ' + '$nParam');
                                                        MusynxBridge.onDecipherResult('$requestId', '$nParam');
                                                    }
                                                })();
                                            """.trimIndent()

                                            view.post {
                                                view.evaluateJavascript(decipherInvokeJs, null)
                                            }

                                            val decipherResult = deferred.await()
                                            Log.d("Musynx-WebView", "Decipher nParam: $nParam -> $decipherResult")
                                            decipherResult
                                        }
                                    )
                                    
                                    // Set PoTokenResolver
                                    Innertube.poTokenResolver = object : Innertube.PoTokenResolver {
                                        override suspend fun getPoToken(videoId: String?): String? {
                                            val deferred = CompletableDeferred<String>()
                                            val requestId = "REQ_${System.currentTimeMillis()}"
                                            poTokenRequests[requestId] = deferred
                                            
                                            val generatePoTokenJs = """
                                                (async function() {
                                                    try {
                                                        const challenge = window.yt?.config_?.WEB_PLAYER_CONTEXT_CONFIG_ID_WEB_PLAYER_BOTGUARD_CHALLENGE || 
                                                                        (window.ytcfg && ytcfg.get ? ytcfg.get('WEB_PLAYER_CONTEXT_CONFIG_ID_WEB_PLAYER_BOTGUARD_CHALLENGE') : null);
                                                        
                                                        if (challenge) {
                                                            const result = await runBotGuard(challenge);
                                                            if (result && result.botguardResponse) {
                                                                MusynxBridge.onPoTokenResult('$requestId', result.botguardResponse);
                                                            } else {
                                                                MusynxBridge.log('JS: BotGuard failed or empty response');
                                                                MusynxBridge.onPoTokenResult('$requestId', '');
                                                            }
                                                        } else {
                                                            MusynxBridge.log('JS: No BotGuard challenge found');
                                                            MusynxBridge.onPoTokenResult('$requestId', '');
                                                        }
                                                    } catch (e) {
                                                        MusynxBridge.log('JS: BotGuard execution error: ' + e.message);
                                                        MusynxBridge.onPoTokenResult('$requestId', '');
                                                    }
                                                })();
                                            """.trimIndent()
                                            
                                            view.post {
                                                view.evaluateJavascript(generatePoTokenJs, null)
                                            }
                                            
                                            return deferred.await().takeIf { it.isNotBlank() }
                                        }
                                    }
                                }
                                
                                if (!apiKey.isNullOrBlank()) {
                                    Innertube.apiKey = apiKey
                                }
                            } catch (e: Exception) {
                                Log.e("Musynx-WebView", "Failed to parse session data", e)
                            }
                        }
                        try {
                            view.evaluateJavascript(BotGuard.JS, null)
                            
                            val initialPoTokenJs = """
                                (async function() {
                                    try {
                                        const challenge = window.yt?.config_?.WEB_PLAYER_CONTEXT_CONFIG_ID_WEB_PLAYER_BOTGUARD_CHALLENGE || 
                                                        (window.ytcfg && ytcfg.get ? ytcfg.get('WEB_PLAYER_CONTEXT_CONFIG_ID_WEB_PLAYER_BOTGUARD_CHALLENGE') : null);
                                        
                                        if (challenge) {
                                            const result = await runBotGuard(challenge);
                                            if (result && result.botguardResponse) {
                                                MusynxBridge.onPoTokenResult('INITIAL', result.botguardResponse);
                                            }
                                        }
                                    } catch (e) {}
                                })();
                            """.trimIndent()
                            
                            view.evaluateJavascript(initialPoTokenJs, null)
                        } catch (e: Exception) {
                            Log.e("Musynx-WebView", "Failed to inject BotGuard script", e)
                        }

                        // Try to find the decipher function
                        injectDecipherScript(view)
                    }
                }

                loadUrl("https://music.youtube.com")
            }
        },
        onRelease = { webView ->
            decipherRequests.values.forEach { it.cancel() }
            decipherRequests.clear()

            webView.removeJavascriptInterface("MusynxBridge")
            webView.stopLoading()
            webView.destroy()
        }
    )
}

private fun injectDecipherScript(webView: WebView) {
    val script = """
        (function() {
            if (window.decipherFunction) {
                MusynxBridge.log('JS: decipherFunction already exists');
                return;
            }
            
            function findDecipherFunction() {
                MusynxBridge.log('JS: searching for decipher function...');
                try {
                    // Try to find it in common locations
                    if (window._yt_player) {
                         for (const key in window._yt_player) {
                             const val = window._yt_player[key];
                             if (typeof val === 'function' && val.length === 1) {
                                 const str = val.toString();
                                 if (str.includes('.split("")') && str.includes('.join("")')) {
                                     MusynxBridge.log('JS: Found decipher in window._yt_player.' + key);
                                     return val;
                                 }
                             }
                         }
                    }

                    const keys = Object.keys(window);
                    for (const key of keys) {
                        if (key.startsWith('_yt_')) {
                            const val = window[key];
                            if (typeof val === 'object' && val !== null) {
                                for (const subKey in val) {
                                    try {
                                        const subVal = val[subKey];
                                        if (typeof subVal === 'function' && subVal.length === 1) {
                                            const str = subVal.toString();
                                            if (str.includes('.split("")') && str.includes('.join("")')) {
                                                MusynxBridge.log('JS: Found potential decipher function in ' + key + '.' + subKey);
                                                return subVal;
                                            }
                                        }
                                    } catch(e) {}
                                }
                            }
                        }
                    }
                } catch(e) {
                    MusynxBridge.log('JS: findDecipherFunction error: ' + e.message);
                }
                return null;
            }

            window.decipherFunction = findDecipherFunction();
            
            window.decipherNParam = function(n, requestId) {
                MusynxBridge.log('JS: decipherNParam called for ' + n);
                let result = n; 
                if (!window.decipherFunction) {
                    window.decipherFunction = findDecipherFunction();
                }
                
                if (window.decipherFunction) {
                    try { 
                        result = window.decipherFunction(n); 
                        MusynxBridge.log('JS: decipher success: ' + n + ' -> ' + result);
                    } catch(e) { 
                        MusynxBridge.log('JS: decipher execution error: ' + e.message); 
                    }
                } else {
                    MusynxBridge.log('JS: Decipher function not found even after retry');
                }
                MusynxBridge.onDecipherResult(requestId, result);
            };
            
            MusynxBridge.log('JS: injectDecipherScript initialization complete');
        })();
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}
