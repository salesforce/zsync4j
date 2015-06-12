# zsync4j

zsync4j is a Java port of [zsync](http://zsync.moria.org.uk/). In a nutshell, zsync is rsync over http: it reduces data downloaded for a given file by reusing unchanged parts from an older local version of the file. This can significantly improve download performance, especially for large files with few changes. All the processing is done on the client and no special server-side support is required as zsync uses standard http [Range Requests](https://tools.ietf.org/html/rfc7233) to retrieve non-matching parts of the file.


## How do I use it?

### Upload

When you upload a file to your http server, generate a .zsync file and upload it along with it. To generate the zsync file use [zsyncmake](http://linux.die.net/man/1/zsyncmake):

```Java
ZsyncMake zsyncmake = new ZsyncMake();

Path file = Paths.get("/Users/ubuntu/20150612/wily-desktop-i386.iso");
Path zsyncFile = zsyncmake.writeToFile(file).getOutputFile();

// upload file and zsync file ...
```

### Download

To download a file use [zsync](http://linux.die.net/man/1/zsync):

```Java
Zsync zsync = new Zsync();

URI zsyncFileURI = URI.create("http://cdimage.ubuntu.com/daily-live/20150612/wily-desktop-i386.iso.zsync");
Path prevVersion = Path.get("/Users/ubuntu/20150611/wily-desktop-i386.iso");
Options options = new Options().addInputFile(prevVersion);

Path outputFile = zsync.zsync(zsyncFileURI, options).getOutputFile();
```


## How does it work?

The zsync control file uploaded together with each file contains block-level checksums, which clients use to identify which parts of the output file can copied from the local file and which parts have to be downloaded. The rough sequence of steps is:

1. Download zysnc control file
2. Write all unchanged blocks from the input file into the output file
3. Request all changed blocks from the server via http [Range Requests](https://tools.ietf.org/html/rfc7233)
4. Compute full checksum of output file to ensure file was properly put together

For more information check out the [zsync paper](http://zsync.moria.org.uk/paper/).


## When should I use zsync4j?

zsync4j is most effective if you frequently update large files over relatively slow http connections. It is less effective for small files, since the additional overhead of transferring a small control file does not amortize. Similarly, if large portions of the file change between requests, the cost of issuing multiple range requests vs a single large request may outweigh the savings. zsync4j also does not yet support gzip-compressed files. However, with some modifications to the creation process (see next section) it can be quite effective for zip and jar files. If used effectively, zsycn4j can improve throughput of your http server, since less data has to be sent in response to each request.


## How does Salesforce use zsync4j?

At Salesforce we currently use zsync4j to optimize our build process. Our continuous integration infrastructure deploys complete jarsets for each commit (up to 1000 per day) and developers download these jarsets to avoid rebuilding parts of the system locally they are not working on. zsync4j reduces the amount of data that has to be downloaded daily by over 90%.

Most of the content we transfer through zsync4j are jar files. To minimize the diff between updates we ensure jars are created with consistent entry ordering and entry timestamps set to the same value. This also ensure idempotency of jar creation, i.e. building a jar for the same content twice results in identical output files.
