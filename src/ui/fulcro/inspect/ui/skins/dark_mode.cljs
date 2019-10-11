(ns fulcro.inspect.ui.skins.dark-mode
  (:require
    [fulcro.inspect.ui.skins :as skins]
    [fulcro.inspect.ui.data-history :as data-history]
    [fulcro.inspect.ui.dom-history-viewer :as domv]
    [fulcro.inspect.ui.inspector :as inspector]
    [fulcro.inspect.ui.multi-inspector :as multi-inspector]
    [fulcro.inspect.ui.events :as events]
    [fulcro.inspect.ui.element :as element]
    [fulcro.inspect.ui.network :as network]
    [fulcro-css.css :as css]
    [fulcro.inspect.ui.transactions :as transactions]
    [com.wsscode.pathom.sugar :as ps]
    [garden.core :as g]
    [fulcro.inspect.ui.core :as ui]
    [garden.selectors :as gs]))

(def css-variables
  {::ui/color-main-bg          "#242424"

   ::ui/color-bg-secondary     "#333"
   ::ui/color-bg-row-alternate "#f5f5f5"
   ::ui/color-bg-light-border  "#e1e1e1"

   ::ui/color-text-normal      "#a5a5a5"
   ::ui/color-text-strong      "#eaeaea"
   ::ui/color-text-faded       "#333"

   ::ui/color-icon-normal      "#919191"
   ::ui/color-icon-strong      "#ccc"

   ::ui/separator-border       "1px solid #525252"})

(def color-text-normal "#A5A5A5")
(def color-text-strong "#EAEAEA")
(def color-text-faded "#333")

(def color-icon-normal "#919191")
(def color-icon-strong "#ccc")

(def separator-border "1px solid #525252")

(def overrides
  [{::skins/component
    multi-inspector/MultiInspector

    ::skins/css
    [[:.no-app
      {:background "#1B1B1B"
       :color      "#9A9A9A"}]]}

   {::skins/component
    inspector/Inspector

    ::skins/css
    [[:.tabs
      {:border-bottom separator-border}]

     [:.tab {:cursor  "pointer"
             :padding "6px 10px 5px"}

      [:&:hover {:background "#202020"
                 :color      "#C1C1C1"}]
      [:&.tab-selected {:background    "#000"
                        :border-bottom "none"
                        :margin-bottom "0"}]]]}

   {::skins/component
    ui/ToolBar

    ::skins/css
    [[:.container {:background    "#333"
                   :border-bottom "1px solid #dadada"}

      [:&.details {:border-bottom "1px solid #ccc"}]]

     [:.separator {:background "#525252"}]

     [:.input
      {:background "#242424"
       :border     "1px solid #242424"}
      [:&:hover {:border "1px solid #555"}]
      [:&:focus {:border "1px solid #2A6398"}]]]}

   #_{::skins/component
      data-history/DataHistory

      ::skins/css
      [[:.container {:background "#242424"}]
       [:.snapshots {:border-left "1px solid #a3a3a3"}]
       [:.snapshots-toggler {:background "#a3a3a3"}]
       [(gs/> :.snapshots (gs/div (gs/nth-child "odd"))) {:background "#f5f5f5"}]]}])

(def skin-dark-mode
  {::skins/mode-name  "Dark Mode"
   ::skins/identifier ::dark-mode
   ::skins/variables  css-variables
   ::skins/overrides  overrides})
