# dribbble-stat

A tool to calculate Dribbble stats:

For a given Dribbble user find all followers
For each follower find all shots
For each shot find all "likers"
Calculate Top10 "likers"


## Usage

Accepts few simple commands as a command-line tool, to see usage run either with lein

    $ lein run

or .jar directly

    $ java -jar dribbble-stat-0.1.0-standalone.jar [args]

URL matching task also included as a separate namespace: dribbble-stat.url-match


## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
