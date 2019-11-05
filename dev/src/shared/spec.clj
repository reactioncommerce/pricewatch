; Instrument spec test to validate that specs are used properly.
; See: https://clojure.github.io/spec.alpha/clojure.spec.test.alpha-api.html#clojure.spec.test.alpha/instrument
(require
  '[clojure.spec.alpha :as s]
  '[clojure.spec.gen.alpha :as gen]
  '[clojure.spec.test.alpha :as stest])

; Instrument in development for feedback on failed specs.
(stest/instrument)

; Use expound for more humane spec error messages.
; https://github.com/bhb/expound
(require '[expound.alpha :as expound])
(set! s/*explain-out* expound/printer)

(println :loaded "shared/spec")
