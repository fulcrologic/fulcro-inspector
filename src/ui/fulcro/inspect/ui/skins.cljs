(ns fulcro.inspect.ui.skins
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.sugar :as ps]
    [fulcro-css.css :as css]
    [garden.core :as g]))

(defn cssify
  "Replaces slashes and dots with underscore."
  [str] (when str (str/replace str #"[./]" "_")))

(defn css-var* [k]
  (str "--" (cssify (subs (str k) 1))))

(defn css-var [k]
  (str "var(" (css-var* k) ")"))

(defn to-css-vars [m]
  (into {}
        (map (fn [[k v]] [(css-var* k) v]))
        m))

(s/def ::component any?)
(s/def ::css any?)
(s/def ::css-expanded any?)
(s/def ::mode-name string?)
(s/def ::identifier qualified-ident?)
(s/def ::skin-classname string?)
(s/def ::overrides (s/keys))

(pc/defresolver expand-override-names [_ {::keys [component css]}]
  {::pc/input  #{::component ::css}
   ::pc/output [::css-expanded]}
  {::css-expanded
   (with-redefs [css/get-local-rules (fn [] css)]
     (css/localize-css component))})

(pc/defresolver export-theme [{:keys [parser] :as env} {::keys [skin-classname variables]}]
  {::pc/input  #{::overrides
                 ::skin-classname
                 ::variables}
   ::pc/output [::theme-css]}
  (let [overrides (::overrides (parser env [{::overrides [::css-expanded]}]))]
    {::theme-css
     [skin-classname
      (to-css-vars variables)
      (into
        []
        (mapcat ::css-expanded)
        overrides)]}))

(def classname-from-identifier
  (pc/single-attr-resolver ::identifier ::skin-classname
    #(keyword (str "." (cssify (str (namespace %) "__" (name %)))))))

(def register
  [classname-from-identifier
   (pc/single-attr-resolver ::theme-css ::theme-css-string g/css)
   (pc/constantly-resolver ::variables {})
   expand-override-names
   export-theme])

(def parser (ps/context-parser (ps/connect-serial-parser register)))

(defn theme-css [theme]
  (::theme-css-string (parser theme [::theme-css-string])))

(defn remove-from-dom "Remove the given element from the DOM by ID"
  [id]
  (if-let [old-element (.getElementById js/document id)]
    (let [parent (.-parentNode old-element)]
      (.removeChild parent old-element))))

(defn upsert-theme-css
  [{::keys [identifier] :as theme}]
  (remove-from-dom (str identifier))
  (let [style-ele (.createElement js/document "style")]
    (set! (.-innerHTML style-ele) (theme-css theme))
    (.setAttribute style-ele "id" (str identifier))
    (.appendChild (.-body js/document) style-ele)))

(comment
  (css-var :fulcro.inspect.ui.core/mono-font-family)
  (to-css-vars fulcro.inspect.ui.core/css-variables)
  (into {}
        (map (fn [[k v]] [(str "--" (cssify (subs (str k) 1))) v]))
        fulcro.inspect.ui.core/css-variables)
  (g/css [[":root" {:--color "black"}]]))
