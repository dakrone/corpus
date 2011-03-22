(ns corpus.core
  (:use [opennlp.nlp :only [make-sentence-detector make-tokenizer
                            make-detokenizer]]
        [opennlp.tools.lazy :only [sentence-seq]]
        [clojure.java.io :only [file]]
        [clojure.contrib.seq :only [indexed]])
  (:require [clojure.string :as string]))

(def s-detect (atom nil))
(def tokenize (atom nil))
(def detokenize (atom nil))

(defn- init
  [& [force]]
  (when (or force (not (and @s-detect @tokenize @detokenize)))
    (reset! s-detect (make-sentence-detector "models/en-sent.bin"))
    (reset! tokenize (make-tokenizer "models/en-token.bin"))
    (reset! detokenize (make-detokenizer "models/english-detokenizer.xml"))))

(defn- match-text
  [idx s tokens]
  (let [de-text (@detokenize tokens)]
    (when-not (= de-text s)
      {:tokens tokens
       :detokenized de-text
       :text s
       :index idx})))

(defn- strip
  [text]
  (-> text
      (string/replace "\n" " ")
      (string/replace "\r" "")
      ;; this tends to confuse the sentence detector
      (string/replace "'" "")
      (string/replace #"\s+" " ")
      (.trim)))

(defn- test-sentence
  [[idx s]]
  (match-text idx (strip s) (@tokenize (strip s))))

(defn corpus-file
  [filename]
  (init)
  (with-open [rdr (clojure.java.io/reader filename)]
    (let [sentences (sentence-seq rdr @s-detect)]
      (vec
       (doall
        (filter identity (map test-sentence (indexed sentences))))))))
