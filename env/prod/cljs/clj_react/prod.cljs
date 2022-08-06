(ns clj-react.prod
  (:require [clj-react.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
