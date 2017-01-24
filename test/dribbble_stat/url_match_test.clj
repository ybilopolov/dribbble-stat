(ns dribbble-stat.url-match-test
  (:require [clojure.test :refer :all]
            [dribbble-stat.url-match :refer :all]))

(deftest test-url-match
  (testing "url-match recognize"
    (let [twitter (gen-pattern "host(twitter.com); path(?user/status/?id);")
          dribbble (gen-pattern "host(dribbble.com); path(shots/?id); queryparam(offset=?offset);")
          dribbble2 (gen-pattern "host(dribbble.com); path(shots/?id); queryparam(offset=?offset); queryparam(list=?type);")]

      (is (= (into {} (recognize twitter "http://twitter.com/bradfitz/status/562360748727611392"))
             {:user "bradfitz", :id 562360748727611392}))

      (is (= (into {} (recognize dribbble "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"))
             {:id "1905065-Travel-Icons-pack", :offset 1}))

      (is (= (into {} (recognize dribbble2 "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users&offset=1"))
             {:id "1905065-Travel-Icons-pack", :type "users", :offset 1}))

      (is (= (recognize dribbble "https://twitter.com/shots/1905065-Travel-Icons-pack?list=users&offset=1")
             nil)) ;; host mismatch

      (is (= (recognize dribbble "https://dribbble.com/shots/1905065-Travel-Icons-pack?list=users")
             nil)) ;; offset queryparam missing

      )))
