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

;; server contains: {:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
(defonce channel-socket-server
  (let [packer (tp/make-packer {})
        ]
    (sente-express/make-express-channel-socket-server!
      {:packer     packer
       ;; With websockets, the client will send a GET with a websocket promotion ability. this is the ONE HTPP
       ;; request that will arrive for a new inspect client...all other comms go over the maintained connection.
       ;; This function therefore establishes a sente "user ID" for that particular connection. I recommend we make
       ;; a 1-to-1 relation from connection to app-uuid if possible...otherwise this defaults to a random UUID I think
       ;; if you don't supply it
       :user-id-fn (fn [ring-req]
                     ;; FIXME???
                     ;; IF the app-uuid can be sent in params, then
                     ;; I'd return that here instead, so you can just watch app uuid disconnect/reconnect on the
                     ;; connected-uuids atom
                     (random-uuid))})
    ;; TODO: ch-recv...Watch this async channel for incoming requests
    ;; TODO: send-fn...This is how to send a push...
    ;; TODO: connected-uids...Watch this for connect/disconnect? NOTE: this is UIDs, which are translated via
    ;; a function you can supply on line 27
    ))

(defn routes [{:keys [ajax-post-fn ajax-get-or-ws-handshake-fn
                      ]} express-app]
  (doto express-app
    (.ws "/chsk"
      (fn [ws req next]
        (ajax-get-or-ws-handshake-fn req nil nil
          {:websocket? true
           :websocket  ws})))
    (.get "/chsk" ajax-get-or-ws-handshake-fn)
    (.post "/chsk" ajax-post-fn)
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

;; websocket messages should be forwarded through this function...be careful of the format
(defn process-client-message [web-contents msg reply-fn]
  (try
    ;; TO the renderer
    (when web-contents
      (.send web-contents "event" #js {"fulcro-inspect-remote-message" msg}))
    (catch :default e
      (js/console.error e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ORIGINAL SOCKET IO CODE...PORT TO SENTE
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start! [{:keys [content-atom]}]
  (let [io (Server)]
    ;; port to core async go loops and atom watches (for connect/disconnect)
    (.on ipcMain "event" (fn [evt arg]
                           (when @the-client
                             ;; TODO: change to chsk-send!
                             (.emit @the-client "event" arg))))
    ;; Becomes watch-based...uuid appears/disappears in connected-uuids
    (.on io "connection" (fn [client]
                           ;; TODO: this should be tracked on app started with app ID associated to websocket client ID
                           (reset! the-client client)
                           ;; Convert to core async go loop
                           (.on client "event"
                             (fn [data reply-fn]
                               ;; TODO: Going to send something that can be processed by a parser...
                               (when-let [web-contents (some-> content-atom deref)]
                                 (process-client-message web-contents data reply-fn))))))
    (.listen io @server-port)))


