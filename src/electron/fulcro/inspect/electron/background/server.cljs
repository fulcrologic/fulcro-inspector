(ns fulcro.inspect.electron.background.server
  (:require
    [fulcro.websockets :as ws]
    [fulcro.websockets.transit-packer :as tp]
    [taoensso.sente.server-adapters.express :as sente-express]
    [taoensso.timbre :as log]
    [cljs.nodejs :as nodejs]
    ["electron" :refer [ipcMain]]
    [goog.object :as gobj]
    [fulcro.inspect.remote.transit :as encode]))

(defonce server-port (atom 8237))
(defonce the-client (atom nil))

(def http (nodejs/require "http"))
(def express (nodejs/require "express"))
(def express-ws (nodejs/require "express-ws"))
(def ws (nodejs/require "ws"))
(def body-parser (nodejs/require "body-parser"))

(let [packer (tp/make-packer {})
      {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente-express/make-express-channel-socket-server!
        {:packer     packer
         :user-id-fn (fn [ring-req] (aget (:body ring-req) "session" "uid"))})]
  (def ajax-post ajax-post-fn)
  (def ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  ;; TODO: Watch this async channel for incoming requests
  (def ch-chsk ch-recv)                                     ; ChannelSocket's receive channel
  ;; TODO: This is how to send a push...
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  ;; TODO: Watch this for connect/disconnect?
  (def connected-uids connected-uids)                       ; Watchable, read-only atom
  )

(defn routes [express-app]
  (doto express-app
    (.ws "/chsk"
      (fn [ws req next]
        (ajax-get-or-ws-handshake req nil nil
          {:websocket? true
           :websocket  ws})))
    (.get "/chsk" ajax-get-or-ws-handshake)
    (.post "/chsk" ajax-post)
    (.use (fn [req res next]
            (log/warn "Unhandled request: %s" (.-originalUrl req))
            (next)))))

(defn wrap-defaults [express-app routes]
  (doto express-app
    (.use (fn [req res next]
            (log/trace "Request: %s" (.-originalUrl req))
            (next)))
    (.use (.urlencoded body-parser #js {:extended false}))
    (routes)))

(defn start-selected-web-server! [ring-handler port]
  (log/info "Starting express...")
  (let [express-app       (express)
        express-ws-server (express-ws express-app)]

    (wrap-defaults express-app routes)

    (let [http-server (.listen express-app port)]
      {:express-app express-app
       :ws-server   express-ws-server
       :http-server http-server
       :stop-fn     #(.close http-server)
       :port        port})))

(defn process-client-message [web-contents msg reply-fn]
  (try
    (when web-contents
      (.send web-contents "event" #js {"fulcro-inspect-remote-message" msg}))
    (catch :default e
      (js/console.error e))))

(defn start! [{:keys [content-atom]}]
  (let [io (Server)]
    (.on ipcMain "event" (fn [evt arg]
                           (when @the-client
                             (.emit @the-client "event" arg))))
    (.on io "connection" (fn [client]
                           ;; TODO: this should be tracked on app started with app ID associated to websocket client ID
                           (reset! the-client client)
                           ;; This is just incoming events, which we're going to encode
                           (.on client "event"
                             (fn [data reply-fn]
                               ;; TODO: Going to send something that can be processed by a parser...
                               (when-let [web-contents (some-> content-atom deref)]
                                 (process-client-message web-contents data reply-fn))))))
    (.listen io @server-port)))


