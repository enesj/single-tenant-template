#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn check-dependencies []
  "Check if required tools are installed"
  (let [tools ["pandoc"]]
    (doseq [tool tools]
      (try
        (process/shell {:out :string :err :string} "which" tool)
        (println (str "✓ " tool " is available"))
        (catch Exception e
          (println (str "✗ " tool " not found. Install with: brew install " tool)))))))

(defn pandoc-to-pdf [input-file output-file]
  "Convert markdown to PDF using pandoc"
  (try
    (let [result (process/shell {:out :string :err :string}
                               "pandoc" input-file
                               "-o" output-file
                               "--pdf-engine=weasyprint"
                               "--css" "style.css"
                               "--metadata" "title=Claude Code Subagent Experiment"
                               "--metadata" "author=Enes"
                               "--toc"
                               "--toc-depth=2")]
      (if (zero? (:exit result))
        (println (str "✅ PDF created successfully: " output-file))
        (println (str "❌ Error creating PDF: " (:err result)))))
    (catch Exception e
      (println (str "❌ Failed to run pandoc: " (.getMessage e))))))

(defn create-css-file []
  "Create a simple CSS file for better PDF styling"
  (let [css-content "
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  line-height: 1.6;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  color: #333;
}

h1 {
  color: #2c3e50;
  border-bottom: 2px solid #3498db;
  padding-bottom: 10px;
}

h2 {
  color: #34495e;
  margin-top: 30px;
}

h3 {
  color: #7f8c8d;
}

code {
  background-color: #f8f9fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
}

pre {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 5px;
  border-left: 4px solid #3498db;
  overflow-x: auto;
}

blockquote {
  border-left: 4px solid #bdc3c7;
  margin: 0;
  padding-left: 20px;
  font-style: italic;
  color: #7f8c8d;
}

.emoji {
  font-size: 1.2em;
}

a {
  color: #3498db;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

strong {
  color: #2c3e50;
}

ul, ol {
  padding-left: 20px;
}

li {
  margin-bottom: 5px;
}
"]
    (spit "style.css" css-content)
    (println "✓ CSS file created")))

(defn cleanup-temp-files []
  "Remove temporary CSS file"
  (when (.exists (io/file "style.css"))
    (io/delete-file "style.css")
    (println "✓ Temporary files cleaned up")))

(defn print-help []
  (println "Markdown to PDF Converter")
  (println "Usage: bb md-to-pdf.clj <input.md> [output.pdf]")
  (println "")
  (println "Arguments:")
  (println "  input.md     Path to the markdown file to convert")
  (println "  output.pdf   Output PDF file path (optional, defaults to input name with .pdf extension)")
  (println "")
  (println "Dependencies:")
  (println "  - pandoc: brew install pandoc")
  (println "")
  (println "Examples:")
  (println "  bb md-to-pdf.clj article.md")
  (println "  bb md-to-pdf.clj docs/readme.md output/readme.pdf"))

(defn generate-output-filename [input-file]
  "Generate output filename by replacing .md with .pdf"
  (if (str/ends-with? input-file ".md")
    (str/replace input-file #"\.md$" ".pdf")
    (str input-file ".pdf")))

(defn -main [& args]
  (cond
    (or (empty? args) (some #{"--help" "-h"} args))
    (print-help)

    :else
    (let [input-file (first args)
          output-file (or (second args) (generate-output-filename input-file))]

      (if (.exists (io/file input-file))
        (do
          (println (str "🔄 Converting " input-file " to " output-file))
          (println "")
          (check-dependencies)
          (println "")
          (create-css-file)
          (pandoc-to-pdf input-file output-file)
          (cleanup-temp-files)
          (println "")
          (println "✅ Conversion complete!"))
        (do
          (println (str "❌ Error: Input file does not exist: " input-file))
          (System/exit 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
