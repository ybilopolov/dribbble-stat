(ns dribbble-stat.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]))

;; -- API ----------------------------------------------------------------------

(def dribbble-oauth "d0fc7b1a28bd2ac223b6ec30136edfae823c458aabca3c85154556a48ca993fc")

(def urls {:api "https://api.dribbble.com/v1/"
           :user "users/%s"
           :shots "users/%s/shots"
           :followers "users/%s/followers"
           :likes "shots/%d/likes"})

(defn get-url [key id]
  (str (:api urls) (format (get urls key) id)))

(defn get-dribbble-data [url]
  (loop [url url, data nil]
    (if (nil? url)
      data
      (let [auth {"access_token" dribbble-oauth}
            resp (client/get url {:query-params auth})
            body (json/read-str (:body resp))
            next-url (get-in resp [:links :next :href])
            rl-remaining (bigdec (get-in resp [:headers :X-RateLimit-Remaining]))
            rl-reset (bigdec (get-in resp [:headers :X-RateLimit-Reset]))]
        (if (and next-url (<= rl-remaining 1))
          (let [timeout (+ (- (* rl-reset 1000) (System/currentTimeMillis)) 1000)]
            (println "API rate limit excceded, resuming in" timeout "ms.")
            (Thread/sleep timeout)))
        (recur next-url (if (map? body) (merge data body) (concat data body)))))))

(defn get-user [name-or-id]
  (get-dribbble-data (get-url :user name-or-id)))

(defn get-followers [user]
  (map #(get % "follower") (get-dribbble-data (get user "followers_url"))))

(defn get-shots [user]
  (get-dribbble-data (get user "shots_url")))

(defn get-likers [shot]
  (map #(get % "user") (get-dribbble-data (get shot "likes_url"))))

(defn get-top-likers [top-n shots]
  (let [likers (mapcat get-likers shots)
        ids (map #(get % "id") likers)
        keyed (into {} (map vector ids likers))]
    (->> ids
         frequencies
         (sort-by (fn [[id fq]] [fq (get-in keyed [id "followers_count"])]))
         reverse
         (take top-n)
         (map (fn [[id cnt]] [cnt (get keyed id)])))))

;; -- CLI ----------------------------------------------------------------------

(defn repr-user [user]
  (format "%10d | %s (aka %s) | shots: %d, followers: %d, likes: %d"
          (get user "id") (get user "name") (get user "username")
          (get user "shots_count") (get user "followers_count")
          (get user "likes_received_count")))

(defn repr-shot [shot]
  (format "%10d | %s (%s) | likes: %d, comments: %d"
          (get shot "id") (get shot "title") (get shot "html_url")
          (get shot "likes_count") (get shot "comments_count")))

(defn show-top-likers [n users desc]
  (let [n (if (string? n) (Integer/parseInt n) n)]
    (println (format "Top (%d) shot likers of %s:" n desc))
    (dorun
     (->> users
          (mapcat get-shots)
          (get-top-likers n)
          (map (fn [[cnt liker]] (str cnt " likes:" (repr-user liker))))
          (map println)))))

(def commands
  {"user"      #(println (repr-user (get-user %)))
   "shots"     #(dorun (map (comp println repr-shot) (get-shots (get-user %))))
   "followers" #(dorun (map (comp println repr-user) (get-followers (get-user %))))
   "top-likers"
               (fn [usr & args]
                 (let [user (get-user usr)]
                   (show-top-likers
                     (or (first args) 10)
                     [user]
                     (get user "username"))))
   "top-net-likers"
               (fn [usr & args]
                 (let [user (get-user usr)]
                   (show-top-likers
                     (or (first args) 10)
                     (get-followers user)
                     (str "all the follovers of " (get user "username")))))})

(defn -main [& args]
  (let [usage (format "dribbble <%s> <user> [top-n]" (string/join "|" (keys commands)))
        cmd (commands (first args))
        usr (second args)]
    (if (or (nil? cmd) (nil? usr)) (do (println usage) (System/exit 1)))
    (apply cmd usr (drop 2 args))))
