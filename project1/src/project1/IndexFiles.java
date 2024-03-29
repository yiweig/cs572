package project1;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class IndexFiles {

  private IndexFiles() {}

    private static String DOC_BEGIN_TAG = "<DOC>";
    private static String DOC_END_TAG = "</DOC>";
    private static String DOC_NO = "";
    private static String DOC_NO_BEGIN_TAG = "<DOCNO>";
    private static String DOC_NO_END_TAG = "</DOCNO>";
    private static String DOC_ID = "";
    private static String DOC_ID_BEGIN_TAG = "<DOCID>";
    private static String DOC_ID_END_TAG = "</DOCID>";
    private static boolean IN_DOC = false;
    private static StringBuilder STRING_BUILDER = new StringBuilder();
    private static String LINE;
    private static Document DOCUMENT = new Document();

  /** Index all text files under a directory. */
  public static void main(String[] args) {
    String usage = "java project1.IndexFiles"
                 + " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
                 + "This indexes the documents in DOCS_PATH, creating a Lucene index"
                 + "in INDEX_PATH that can be searched with SearchFiles";
    String indexPath = "index";
    String docsPath = null;
    boolean create = true;
    for(int i=0;i<args.length;i++) {
      if ("-index".equals(args[i])) {
        indexPath = args[i+1];
        i++;
      } else if ("-docs".equals(args[i])) {
        docsPath = args[i+1];
        i++;
      } else if ("-update".equals(args[i])) {
        create = false;
      }
    }

    if (docsPath == null) {
      System.err.println("Usage: " + usage);
      System.exit(1);
    }

    final Path docDir = Paths.get(docsPath);
    if (!Files.isReadable(docDir)) {
      System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
      System.exit(1);
    }

    Date start = new Date();
    try {
      System.out.println("Indexing to directory '" + indexPath + "'...");

      Directory dir = FSDirectory.open(Paths.get(indexPath));
      Analyzer analyzer = new StandardAnalyzer();
      IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

      if (create) {
        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE);
      } else {
        // Add new documents to an existing index:
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
      }

      // Optional: for better indexing performance, if you
      // are indexing many documents, increase the RAM
      // buffer.  But if you do this, increase the max heap
      // size to the JVM (eg add -Xmx512m or -Xmx1g):
      //
      // iwc.setRAMBufferSizeMB(256.0);

      IndexWriter writer = new IndexWriter(dir, iwc);
      indexDocs(writer, docDir);

      // NOTE: if you want to maximize search performance,
      // you can optionally call forceMerge here.  This can be
      // a terribly costly operation, so generally it's only
      // worth it when your index is relatively static (ie
      // you're done adding documents to it):
      //
      // writer.forceMerge(1);

      writer.close();

      Date end = new Date();
      System.out.println(end.getTime() - start.getTime() + " total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  /**
   * Indexes the given file using the given writer, or if a directory is given,
   * recurses over files and directories found under the given directory.
   *
   * NOTE: This method indexes one DOCUMENT per input file.  This is slow.  For good
   * throughput, put multiple documents into your input file(s).  An example of this is
   * in the benchmark module, which can create "line doc" files, one DOCUMENT per line,
   * using the
   * <a href="../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
   * >WriteLineDocTask</a>.
   *
   * @param writer Writer to the index where the given file/dir info will be stored
   * @param path The file to index, or the directory to recurse into to find files to index
   * @throws IOException If there is a low-level I/O error
   */
  static void indexDocs(final IndexWriter writer, Path path) throws IOException {
    if (Files.isDirectory(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          try {
            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
          } catch (IOException ignore) {
            // don't index files that can't be read.
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } else {
      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
    }
  }

  /** Indexes a single DOCUMENT */
  static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
    try (InputStream stream = Files.newInputStream(file)) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

        while ((LINE = bufferedReader.readLine()) != null) {
            if (LINE.equals(DOC_BEGIN_TAG)) {
                IN_DOC = true;
                continue;
            } else if (LINE.equals(DOC_END_TAG)) {
                IN_DOC = false;
                DOCUMENT.add(new StringField("path", file.toString(), Field.Store.YES));
                DOCUMENT.add(new LongField("modified", lastModified, Field.Store.NO));
                DOCUMENT.add(new TextField("filename", file.toString(), Field.Store.YES));
                if (!DOC_NO.equals("")) {
                    DOCUMENT.add(new TextField("document_id", DOC_NO, Field.Store.YES));
                } else if (!DOC_ID.equals("")) {
                    DOCUMENT.add(new TextField("document_id", DOC_ID, Field.Store.YES));
                } else {
                    DOCUMENT = new Document();
                    STRING_BUILDER = new StringBuilder();
                    continue;
                }

                DOCUMENT.add(new TextField("contents", STRING_BUILDER.toString(), Field.Store.NO));

                String name = DOC_NO.equals("") ? DOC_ID : DOC_NO;
                if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                    // New index, so we just add the DOCUMENT (no old DOCUMENT can be there):
                    System.out.println("adding " + name);
                    writer.addDocument(DOCUMENT);
                } else {
                    // Existing index (an old copy of this DOCUMENT may have been indexed) so
                    // we use updateDocument instead to replace the old one matching the exact
                    // path, if present:
                    System.out.println("updating " + name);
                    writer.updateDocument(new Term("path", file.toString()), DOCUMENT);
                }

                DOCUMENT = new Document();
                STRING_BUILDER = new StringBuilder();

                continue;
            }
            if (LINE.contains(DOC_NO_BEGIN_TAG) && LINE.contains(DOC_NO_END_TAG)) {
                DOC_NO = LINE.replace(DOC_NO_BEGIN_TAG, "").replace(DOC_NO_END_TAG, "").trim();
            }
            if (LINE.contains(DOC_ID_BEGIN_TAG) && LINE.contains(DOC_ID_END_TAG)) {
                DOC_ID = LINE.replace(DOC_ID_BEGIN_TAG, "").replace(DOC_ID_END_TAG, "").trim();
            }
            if (IN_DOC) {
                STRING_BUILDER.append(LINE).append("\n");
            }
        }
    }
  }
}
