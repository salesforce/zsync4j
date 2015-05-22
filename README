zsync4j is a Java port of [zsync](http://zsync.moria.org.uk/). In a nutshell, zsync is rsync over http: it reduces the amount of data that has to be downloaded for a given file by reusing unchanged parts from a local version of the file. It uses standard http [Range Requests](https://tools.ietf.org/html/rfc7233) to retrieve only the non-matching parts of the file.

The flow is as follows:

provider:
* upload file f1 to server
* upload f1.zsync metadata file that describes content of f1 in condensed form

consumer:
* download f1.zsync metadata file
* write matching content from local version f0 into new local copy of f1
* download remaining non-matching content for f1 from server